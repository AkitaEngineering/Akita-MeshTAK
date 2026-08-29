package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.akitaengineering.meshtak.ui.AkitaMockSettings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

/**
 * Native OpenTAKServer CoT streaming client. SSL without an imported client
 * certificate is fail-closed.
 */
public final class OpenTakStreamingClient {

    public static final String PREF_ENABLED = "opentakserver_enabled";
    public static final String PREF_HOST = "opentakserver_host";
    public static final String PREF_PORT = "opentakserver_port";
    public static final String PREF_SSL = "opentakserver_ssl";
    public static final int DEFAULT_TCP_PORT = 8088;
    public static final int DEFAULT_SSL_PORT = 8089;

    private static final String TAG = "OpenTakStream";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int SOCKET_TIMEOUT_MS = 15000;

    private static final OpenTakStreamingClient INSTANCE = new OpenTakStreamingClient();

    private final Object lock = new Object();
    private final AtomicReference<String> status = new AtomicReference<>("Idle");
    private final AtomicReference<String> lastError = new AtomicReference<>("");
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger publishedCount = new AtomicInteger();
    private final AtomicLong lastPublishAt = new AtomicLong();
    private Context appContext;
    private Socket socket;
    private OutputStream outputStream;
    private Connector connector = new DefaultConnector();

    private OpenTakStreamingClient() {
    }

    public static OpenTakStreamingClient getInstance() {
        return INSTANCE;
    }

    public synchronized void attach(Context context) {
        appContext = context.getApplicationContext();
    }

    synchronized void setConnectorForTests(Connector connector) {
        this.connector = connector == null ? new DefaultConnector() : connector;
    }

    public synchronized void resetForTests() {
        disconnectLocked();
        status.set("Idle");
        lastError.set("");
        connected.set(false);
        publishedCount.set(0);
        lastPublishAt.set(0L);
        connector = new DefaultConnector();
        appContext = null;
    }

    public HealthSnapshot getHealth(SharedPreferences preferences) {
        boolean enabled = isEnabled(preferences);
        boolean ssl = isSsl(preferences);
        String host = getHost(preferences);
        int port = getPort(preferences);
        String detail;
        if (AkitaMockSettings.isEnabled(preferences)) {
            detail = "Simulated OpenTAKServer " + (ssl ? "SSL" : "TCP");
        } else if (!enabled) {
            detail = "Native streaming disabled • ATAK TAK server path remains available";
        } else if (host.isEmpty()) {
            detail = "Host not configured";
        } else if (ssl && (appContext == null || !OpenTakCertificateStore.hasImportedCertificate(appContext))) {
            detail = "SSL blocked: import a client PKCS#12 before connecting";
        } else {
            detail = status.get();
            String error = lastError.get();
            if (!connected.get() && !TextUtils.isEmpty(error)) {
                detail = detail + " • " + error;
            }
        }
        return new HealthSnapshot(enabled, ssl, host, port, connected.get(), publishedCount.get(), lastPublishAt.get(), detail);
    }

    public boolean publish(String cotXml) {
        if (TextUtils.isEmpty(cotXml) || !cotXml.contains("<event")) {
            return false;
        }
        SharedPreferences preferences = currentPreferences();
        if (preferences == null) {
            return false;
        }
        if (AkitaMockSettings.isEnabled(preferences)) {
            publishedCount.incrementAndGet();
            lastPublishAt.set(System.currentTimeMillis());
            status.set("Simulated publish");
            return true;
        }
        if (!isEnabled(preferences)) {
            return false;
        }
        try {
            ensureConnected(preferences);
            byte[] payload = (cotXml.trim() + "\n").getBytes(StandardCharsets.UTF_8);
            synchronized (lock) {
                if (outputStream == null) {
                    return false;
                }
                outputStream.write(payload);
                outputStream.flush();
            }
            publishedCount.incrementAndGet();
            lastPublishAt.set(System.currentTimeMillis());
            status.set("Connected • " + publishedCount.get() + " events");
            AuditLogger.getInstance().log(AuditLogger.EventType.DATA_SENT, AuditLogger.Severity.INFO,
                    "OpenTAKServer", "CoT streamed, bytes " + payload.length, true);
            return true;
        } catch (IllegalStateException exception) {
            status.set("Blocked");
            lastError.set(exception.getMessage());
            AuditLogger.getInstance().log(AuditLogger.EventType.SECURITY_VIOLATION, AuditLogger.Severity.WARNING,
                    "OpenTAKServer", exception.getMessage(), false);
            return false;
        } catch (Exception exception) {
            status.set("Error");
            lastError.set(safeError(exception));
            disconnect();
            AuditLogger.getInstance().log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                    "OpenTAKServer", "Publish failed", false);
            return false;
        }
    }

    public boolean publishInboundChat(String originNode, String payload, SharedPreferences preferences) {
        CotEventFactory.ChatPayload chat = CotEventFactory.parseMailboxChat(payload);
        String sender = OperatorIdentity.sanitizeToken(originNode, OperatorIdentity.MAX_CALLSIGN_LENGTH);
        if (sender.isEmpty()) {
            sender = OperatorIdentity.getCallsign(preferences);
        }
        String destination;
        String message;
        boolean direct;
        if (chat != null) {
            destination = chat.destination;
            message = chat.message;
            direct = chat.directMessage;
        } else if (payload != null && !payload.trim().isEmpty() && payload.trim().length() <= 240) {
            destination = OperatorIdentity.getChatRoom(preferences);
            message = payload.trim();
            direct = false;
        } else {
            return false;
        }
        String xml = CotEventFactory.geoChatEvent(
                sender,
                destination,
                message,
                direct,
                OperatorIdentity.getMissionName(preferences),
                0.0d,
                0.0d,
                System.currentTimeMillis(),
                OperatorIdentity.getStaleSeconds(preferences));
        return publish(xml);
    }

    public void applyPreferences() {
        SharedPreferences preferences = currentPreferences();
        if (preferences == null) {
            return;
        }
        if (AkitaMockSettings.isEnabled(preferences) || !isEnabled(preferences)) {
            disconnect();
            status.set(AkitaMockSettings.isEnabled(preferences) ? "Simulated" : "Disabled");
            return;
        }
        disconnect();
        try {
            ensureConnected(preferences);
        } catch (Exception exception) {
            status.set("Error");
            lastError.set(safeError(exception));
        }
    }

    public void shutdown() {
        disconnect();
        status.set("Idle");
    }

    public static boolean isEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(PREF_ENABLED, false);
    }

    public static boolean isSsl(SharedPreferences preferences) {
        return preferences.getBoolean(PREF_SSL, false);
    }

    public static String getHost(SharedPreferences preferences) {
        String host = preferences.getString(PREF_HOST, "");
        return host == null ? "" : host.trim();
    }

    public static int getPort(SharedPreferences preferences) {
        boolean ssl = isSsl(preferences);
        String raw = preferences.getString(PREF_PORT, String.valueOf(ssl ? DEFAULT_SSL_PORT : DEFAULT_TCP_PORT));
        try {
            int port = Integer.parseInt(raw.trim());
            if (port > 0 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
        }
        return ssl ? DEFAULT_SSL_PORT : DEFAULT_TCP_PORT;
    }

    public static boolean isValidHost(String host) {
        if (host == null) {
            return false;
        }
        String trimmed = host.trim();
        if (trimmed.isEmpty() || trimmed.length() > 253) {
            return false;
        }
        for (int index = 0; index < trimmed.length(); index++) {
            char c = trimmed.charAt(index);
            if (!Character.isLetterOrDigit(c) && c != '.' && c != '-' && c != ':') {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidPort(String portText) {
        try {
            int port = Integer.parseInt(portText.trim());
            return port > 0 && port <= 65535;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void ensureConnected(SharedPreferences preferences) throws IOException, GeneralSecurityException {
        if (connected.get() && socket != null && socket.isConnected() && !socket.isClosed()) {
            return;
        }
        String host = getHost(preferences);
        if (host.isEmpty() || !isValidHost(host)) {
            throw new IllegalStateException("OpenTAKServer host is not configured.");
        }
        boolean ssl = isSsl(preferences);
        if (ssl) {
            if (appContext == null || !OpenTakCertificateStore.hasImportedCertificate(appContext)) {
                throw new IllegalStateException("OpenTAKServer SSL requires an imported client PKCS#12.");
            }
        }
        int port = getPort(preferences);
        SSLContext sslContext = ssl ? OpenTakCertificateStore.createSslContext(appContext) : null;
        Socket newSocket = connector.connect(host, port, ssl, sslContext);
        synchronized (lock) {
            disconnectLocked();
            socket = newSocket;
            outputStream = newSocket.getOutputStream();
            connected.set(true);
            status.set("Connected");
            lastError.set("");
        }
        AuditLogger.getInstance().log(AuditLogger.EventType.CONNECTION, AuditLogger.Severity.INFO,
                "OpenTAKServer", (ssl ? "SSL" : "TCP") + " " + host + ":" + port, true);
    }

    private void disconnect() {
        synchronized (lock) {
            disconnectLocked();
        }
        connected.set(false);
    }

    private void disconnectLocked() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ignored) {
            }
            outputStream = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }
        connected.set(false);
    }

    private SharedPreferences currentPreferences() {
        if (appContext == null) {
            return null;
        }
        return PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    private static String safeError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isEmpty()) {
            return exception.getClass().getSimpleName();
        }
        return message.replace('\n', ' ').trim();
    }

    public static final class HealthSnapshot {
        public final boolean enabled;
        public final boolean ssl;
        public final String host;
        public final int port;
        public final boolean connected;
        public final int publishedCount;
        public final long lastPublishAt;
        public final String detail;

        HealthSnapshot(boolean enabled,
                       boolean ssl,
                       String host,
                       int port,
                       boolean connected,
                       int publishedCount,
                       long lastPublishAt,
                       String detail) {
            this.enabled = enabled;
            this.ssl = ssl;
            this.host = host;
            this.port = port;
            this.connected = connected;
            this.publishedCount = publishedCount;
            this.lastPublishAt = lastPublishAt;
            this.detail = detail;
        }

        public String endpointLabel() {
            if (host == null || host.isEmpty()) {
                return ssl ? "SSL unconfigured" : "TCP unconfigured";
            }
            return String.format(Locale.US, "%s %s:%d", ssl ? "SSL" : "TCP", host, port);
        }
    }

    interface Connector {
        Socket connect(String host, int port, boolean ssl, SSLContext sslContext) throws IOException;
    }

    private static final class DefaultConnector implements Connector {
        @Override
        public Socket connect(String host, int port, boolean ssl, SSLContext sslContext) throws IOException {
            if (ssl) {
                SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket();
                sslSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                sslSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
                sslSocket.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
                sslSocket.startHandshake();
                return sslSocket;
            }
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            return socket;
        }
    }
}
