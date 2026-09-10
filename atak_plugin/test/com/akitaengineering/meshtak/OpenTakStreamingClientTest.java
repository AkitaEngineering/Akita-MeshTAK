package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

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
import java.io.OutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.robolectric.Shadows.shadowOf;

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

    @Test
    public void asyncPublishDoesNotBlockUiAndReturnsResultOnMainThread() throws Exception {
        enableTcp();
        CountDownLatch connecting = new CountDownLatch(1);
        CountDownLatch allowConnection = new CountDownLatch(1);
        CountDownLatch callback = new CountDownLatch(1);
        AtomicReference<Thread> networkThread = new AtomicReference<>();
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        AtomicReference<Boolean> result = new AtomicReference<>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        OpenTakStreamingClient.getInstance().setConnectorForTests((host, port, ssl, sslContext) -> {
            networkThread.set(Thread.currentThread());
            connecting.countDown();
            try {
                if (!allowConnection.await(3, TimeUnit.SECONDS)) throw new IOException("Timed out");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException(exception);
            }
            return socketWritingTo(output);
        });
        try {
            OpenTakStreamingClient.getInstance().publishAsync("<event uid=\"async\"/>", published -> {
                callbackThread.set(Thread.currentThread());
                result.set(published);
                callback.countDown();
            });
            assertTrue(connecting.await(3, TimeUnit.SECONDS));
            assertNotSame(Thread.currentThread(), networkThread.get());
            assertEquals(1L, callback.getCount());
        } finally {
            allowConnection.countDown();
        }
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (callback.getCount() != 0 && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        assertEquals(0L, callback.getCount());
        assertEquals(Boolean.TRUE, result.get());
        assertSame(Looper.getMainLooper().getThread(), callbackThread.get());
        assertEquals("<event uid=\"async\"/>\n", output.toString("UTF-8"));
    }

    @Test
    public void shutdownDiscardsQueuedTraffic() throws Exception {
        enableTcp();
        CountDownLatch connecting = new CountDownLatch(1);
        CountDownLatch allowConnection = new CountDownLatch(1);
        AtomicInteger sends = new AtomicInteger();
        OutputStream output = new ByteArrayOutputStream() {
            @Override public void flush() { sends.incrementAndGet(); }
        };
        OpenTakStreamingClient.getInstance().setConnectorForTests((host, port, ssl, sslContext) -> {
            connecting.countDown();
            try {
                if (!allowConnection.await(3, TimeUnit.SECONDS)) throw new IOException("Timed out");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException(exception);
            }
            return socketWritingTo(output);
        });
        try {
            OpenTakStreamingClient.getInstance().publishAsync("<event uid=\"first\"/>");
            assertTrue(connecting.await(3, TimeUnit.SECONDS));
            OpenTakStreamingClient.getInstance().publishAsync("<event uid=\"cancelled\"/>");
            OpenTakStreamingClient.getInstance().shutdown();
        } finally {
            allowConnection.countDown();
        }
        // Wait for all worker activity before checking how many writes took place.
        OpenTakStreamingClient.getInstance().resetForTests();
        assertEquals(1, sends.get());
    }

    @Test
    public void tlsRequiresHostnameVerificationAndOnlyModernSupportedProtocols() throws Exception {
        try (SSLSocket socket = (SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket()) {
            OpenTakStreamingClient.configureTls(socket);
            assertEquals("HTTPS", socket.getSSLParameters().getEndpointIdentificationAlgorithm());
            assertTrue(socket.getEnabledProtocols().length > 0);
            for (String protocol : socket.getEnabledProtocols()) {
                assertTrue(protocol.equals("TLSv1.2") || protocol.equals("TLSv1.3"));
            }
        }
    }

    private void enableTcp() {
        preferences.edit()
                .putBoolean(OpenTakStreamingClient.PREF_ENABLED, true)
                .putBoolean(OpenTakStreamingClient.PREF_SSL, false)
                .putString(OpenTakStreamingClient.PREF_HOST, "127.0.0.1")
                .putString(OpenTakStreamingClient.PREF_PORT, "8088")
                .commit();
    }

    private static Socket socketWritingTo(OutputStream output) {
        return new Socket() {
            @Override public OutputStream getOutputStream() { return output; }
            @Override public boolean isConnected() { return true; }
        };
    }

}
