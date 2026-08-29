package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class OpenTakStreamingClientTest {

    private Context context;
    private SharedPreferences preferences;
    private ServerSocket serverSocket;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear().commit();
        OpenTakStreamingClient.getInstance().resetForTests();
        OpenTakStreamingClient.getInstance().attach(context);
        serverSocket = new ServerSocket(0);
    }

    @After
    public void tearDown() throws Exception {
        OpenTakStreamingClient.getInstance().resetForTests();
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    @Test
    public void sslWithoutCertificateIsFailClosed() {
        preferences.edit()
                .putBoolean(OpenTakStreamingClient.PREF_ENABLED, true)
                .putBoolean(OpenTakStreamingClient.PREF_SSL, true)
                .putString(OpenTakStreamingClient.PREF_HOST, "127.0.0.1")
                .putString(OpenTakStreamingClient.PREF_PORT, "8089")
                .commit();
        boolean published = OpenTakStreamingClient.getInstance().publish(
                CotEventFactory.testLocationEvent("A", "Cyan", "Team Member", "", System.currentTimeMillis(), 120));
        assertFalse(published);
        OpenTakStreamingClient.HealthSnapshot health = OpenTakStreamingClient.getInstance().getHealth(preferences);
        assertTrue(health.detail.toLowerCase().contains("ssl"));
        assertFalse(health.connected);
    }

    @Test
    public void tcpPublishSendsCotXml() throws Exception {
        CountDownLatch accepted = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>("");
        Thread acceptor = new Thread(() -> {
            try (Socket client = serverSocket.accept();
                 InputStream input = client.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int read = input.read(buffer);
                if (read > 0) {
                    output.write(buffer, 0, read);
                }
                received.set(output.toString(StandardCharsets.UTF_8.name()));
            } catch (Exception ignored) {
            } finally {
                accepted.countDown();
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();

        int port = serverSocket.getLocalPort();
        preferences.edit()
                .putBoolean(OpenTakStreamingClient.PREF_ENABLED, true)
                .putBoolean(OpenTakStreamingClient.PREF_SSL, false)
                .putString(OpenTakStreamingClient.PREF_HOST, "127.0.0.1")
                .putString(OpenTakStreamingClient.PREF_PORT, String.valueOf(port))
                .commit();

        OpenTakStreamingClient.getInstance().setConnectorForTests((host, connectPort, ssl, sslContext) -> {
            Socket socket = new Socket(host, connectPort);
            socket.setSoTimeout(2000);
            return socket;
        });

        String xml = CotEventFactory.testLocationEvent("Alpha1", "Cyan", "Team Member", "River", System.currentTimeMillis(), 120);
        assertTrue(OpenTakStreamingClient.getInstance().publish(xml));
        assertTrue(accepted.await(3, TimeUnit.SECONDS));
        assertTrue(received.get().contains("<event"));
        assertTrue(received.get().contains("Alpha1"));
    }

}
