package com.akitaengineering.meshtak;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Read-only Marti mission listing. SSL without a client certificate is fail-closed.
 */
public final class OpenTakMissionApi {

    public static final int DEFAULT_MARTI_PORT = 8443;

    private OpenTakMissionApi() {
    }

    public static String missionListPath() {
        return "/Marti/api/missions";
    }

    public static String missionListUrl(String host, int port) {
        String safeHost = host == null ? "" : host.trim();
        int safePort = port > 0 ? port : DEFAULT_MARTI_PORT;
        return String.format(Locale.US, "https://%s:%d%s", safeHost, safePort, missionListPath());
    }

    public static List<String> parseMissionNames(String json) throws JSONException {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String trimmed = json.trim();
        JSONArray array;
        if (trimmed.startsWith("[")) {
            array = new JSONArray(trimmed);
        } else {
            JSONObject object = new JSONObject(trimmed);
            if (object.has("data")) {
                array = object.getJSONArray("data");
            } else if (object.has("missions")) {
                array = object.getJSONArray("missions");
            } else {
                array = new JSONArray();
                array.put(object);
            }
        }
        List<String> names = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            Object entry = array.get(index);
            String name = null;
            if (entry instanceof JSONObject) {
                JSONObject object = (JSONObject) entry;
                name = firstText(object, "name", "missionName", "uid");
            } else if (entry instanceof String) {
                name = (String) entry;
            }
            String sanitized = OperatorIdentity.sanitizeMissionName(name);
            if (!sanitized.isEmpty() && !names.contains(sanitized)) {
                names.add(sanitized);
            }
        }
        return names;
    }

    public static List<String> listMissions(OpenTakStreamingClient.HealthSnapshot health,
                                            SSLSocketFactory sslSocketFactory) throws IOException, JSONException {
        if (health == null || !health.ssl) {
            throw new IllegalStateException("Mission API requires OpenTAKServer SSL.");
        }
        if (sslSocketFactory == null) {
            throw new IllegalStateException("OpenTAKServer SSL requires an imported client PKCS#12.");
        }
        if (health.host == null || health.host.isEmpty()) {
            throw new IllegalStateException("OpenTAKServer host is not configured.");
        }
        URL url = new URL(missionListUrl(health.host, DEFAULT_MARTI_PORT));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            if (!(connection instanceof HttpsURLConnection)) {
                throw new IllegalStateException("Mission API requires HTTPS.");
            }
            HttpsURLConnection https = (HttpsURLConnection) connection;
            https.setSSLSocketFactory(sslSocketFactory);
            https.setConnectTimeout(5000);
            https.setReadTimeout(8000);
            https.setRequestMethod("GET");
            https.setRequestProperty("Accept", "application/json");
            int code = https.getResponseCode();
            InputStream stream = code >= 400 ? https.getErrorStream() : https.getInputStream();
            String body = readFully(stream);
            if (code < 200 || code >= 300) {
                throw new IOException("Mission API HTTP " + code);
            }
            return parseMissionNames(body);
        } finally {
            connection.disconnect();
        }
    }

    public static SSLSocketFactory sslSocketFactoryFromStore(KeyStoreAdapter adapter)
            throws GeneralSecurityException, IOException {
        if (adapter == null) {
            throw new IllegalStateException("OpenTAKServer SSL requires an imported client PKCS#12.");
        }
        SSLContext sslContext = adapter.createTlsContext();
        return sslContext.getSocketFactory();
    }

    private static String firstText(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.optString(key, "");
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static String readFully(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    interface KeyStoreAdapter {
        SSLContext createTlsContext() throws GeneralSecurityException, IOException;
    }
}
