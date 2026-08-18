package com.akitaengineering.meshtak;

import android.util.AtomicFile;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Session and restart-resistant replay defense for authenticated envelopes.
 * Nonces are remembered in memory and, when a state file is attached, on disk.
 */
public final class ReplayGuard {
    public static final String STATE_FILE_NAME = "akita-replay-state.json";
    static final int REPLAY_CACHE_LIMIT = 128;

    private static final String TAG = "ReplayGuard";
    private static final Object LOCK = new Object();
    private static final Set<String> SEEN_NONCES = new LinkedHashSet<>();

    private static File stateFile;
    private static long watermarkEpochSeconds;
    private static boolean loaded;

    private ReplayGuard() {
    }

    public static void attach(File persistenceFile) {
        synchronized (LOCK) {
            stateFile = persistenceFile;
            loaded = false;
            SEEN_NONCES.clear();
            watermarkEpochSeconds = 0L;
            loadLocked();
        }
    }

    public static void resetForTests() {
        synchronized (LOCK) {
            stateFile = null;
            loaded = true;
            SEEN_NONCES.clear();
            watermarkEpochSeconds = 0L;
        }
    }

    public static boolean hasSeen(String nonceKey) {
        if (nonceKey == null || nonceKey.isEmpty()) {
            return true;
        }
        synchronized (LOCK) {
            ensureLoadedLocked();
            return SEEN_NONCES.contains(nonceKey);
        }
    }

    public static void remember(String nonceKey, long timestampEpochSeconds) {
        if (nonceKey == null || nonceKey.isEmpty()) {
            return;
        }
        synchronized (LOCK) {
            ensureLoadedLocked();
            SEEN_NONCES.add(nonceKey);
            while (SEEN_NONCES.size() > REPLAY_CACHE_LIMIT) {
                String oldest = SEEN_NONCES.iterator().next();
                SEEN_NONCES.remove(oldest);
            }
            if (timestampEpochSeconds > watermarkEpochSeconds) {
                watermarkEpochSeconds = timestampEpochSeconds;
            }
            persistLocked();
        }
    }

    public static long getWatermarkEpochSeconds() {
        synchronized (LOCK) {
            ensureLoadedLocked();
            return watermarkEpochSeconds;
        }
    }

    static List<String> snapshotNoncesForTests() {
        synchronized (LOCK) {
            ensureLoadedLocked();
            return new ArrayList<>(SEEN_NONCES);
        }
    }

    private static void ensureLoadedLocked() {
        if (!loaded) {
            loadLocked();
        }
    }

    private static void loadLocked() {
        loaded = true;
        if (stateFile == null || !stateFile.isFile()) {
            return;
        }
        AtomicFile atomicFile = new AtomicFile(stateFile);
        try (FileInputStream inputStream = atomicFile.openRead()) {
            byte[] payload = readAllBytes(inputStream);
            if (payload.length == 0) {
                return;
            }
            JSONObject root = new JSONObject(new String(payload, StandardCharsets.UTF_8));
            watermarkEpochSeconds = Math.max(0L, root.optLong("watermark", 0L));
            JSONArray nonces = root.optJSONArray("nonces");
            if (nonces == null) {
                return;
            }
            for (int index = 0; index < nonces.length(); index++) {
                String nonce = nonces.optString(index, "");
                if (!nonce.isEmpty()) {
                    SEEN_NONCES.add(nonce);
                }
            }
            while (SEEN_NONCES.size() > REPLAY_CACHE_LIMIT) {
                String oldest = SEEN_NONCES.iterator().next();
                SEEN_NONCES.remove(oldest);
            }
        } catch (IOException | JSONException exception) {
            Log.w(TAG, "Failed to load persisted replay state; starting with empty cache", exception);
            SEEN_NONCES.clear();
            watermarkEpochSeconds = 0L;
        }
    }

    private static void persistLocked() {
        if (stateFile == null) {
            return;
        }
        File parent = stateFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.w(TAG, "Unable to create replay-state directory: " + parent);
            return;
        }
        AtomicFile atomicFile = new AtomicFile(stateFile);
        FileOutputStream outputStream = null;
        try {
            JSONObject root = new JSONObject();
            root.put("schemaVersion", 1);
            root.put("watermark", watermarkEpochSeconds);
            JSONArray nonces = new JSONArray();
            for (String nonce : SEEN_NONCES) {
                nonces.put(nonce);
            }
            root.put("nonces", nonces);
            outputStream = atomicFile.startWrite();
            outputStream.write(root.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            atomicFile.finishWrite(outputStream);
            outputStream = null;
        } catch (IOException | JSONException exception) {
            Log.w(TAG, "Failed to persist replay state", exception);
            if (outputStream != null) {
                atomicFile.failWrite(outputStream);
            }
        }
    }

    private static byte[] readAllBytes(FileInputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }
}
