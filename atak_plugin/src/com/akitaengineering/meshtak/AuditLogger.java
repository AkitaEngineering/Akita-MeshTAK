// File: atak_plugin/src/com/akitaengineering/meshtak/AuditLogger.java
// Description: Audit logging for accountability and security monitoring.
// CRITICAL: For military/law enforcement accountability requirements

package com.akitaengineering.meshtak;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Audit Logger for tracking all security-relevant events.
 * Provides accountability for military/law enforcement operations.
 */
public class AuditLogger {
    private static final String TAG = "AuditLogger";
    private static final int MAX_ENTRIES = 512;
    private static final String AUDIT_LOG_DIR = "audit_logs";

    public enum EventType {
        CONNECTION,
        DISCONNECTION,
        COMMAND_SENT,
        COMMAND_RECEIVED,
        DATA_SENT,
        DATA_RECEIVED,
        SECURITY_VIOLATION,
        AUTHENTICATION_FAILURE,
        INTEGRITY_FAILURE,
        SOS_TRIGGERED,
        CONFIGURATION_CHANGE,
        ERROR
    }

    public enum Severity {
        INFO(0),
        WARNING(1),
        ERROR(2),
        CRITICAL(3);

        private final int level;
        Severity(int level) {
            this.level = level;
        }
        public int getLevel() {
            return level;
        }
    }

    private static class AuditEntry {
        long timestamp;
        EventType eventType;
        Severity severity;
        String source;
        String details;
        boolean success;

        AuditEntry(long timestamp, EventType eventType, Severity severity,
                   String source, String details, boolean success) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.severity = severity;
            this.source = source;
            this.details = details;
            this.success = success;
        }
    }

    // initialize() converts every supplied context to its application context.
    @SuppressLint("StaticFieldLeak")
    private static AuditLogger instance;
    private Context context;
    private List<AuditEntry> entries;

    private AuditLogger() {
        entries = new ArrayList<>();
    }

    public static synchronized AuditLogger getInstance() {
        if (instance == null) {
            instance = new AuditLogger();
        }
        return instance;
    }

    public void initialize(Context context) {
        // Retain only the process-wide application context; callers may pass an
        // Activity or ATAK plugin context whose lifecycle is much shorter.
        this.context = context == null ? null : context.getApplicationContext();
        log(EventType.CONFIGURATION_CHANGE, Severity.INFO, "SYSTEM",
            "Audit logger initialized", true);
    }

    public void log(EventType eventType, Severity severity, String source,
                    String details, boolean success) {
        long timestamp = System.currentTimeMillis();
        String safeSource = sanitizeField(source, 64);
        String safeDetails = sanitizeField(details, 256);
        AuditEntry entry = new AuditEntry(timestamp, eventType, severity,
                                          safeSource, safeDetails, success);

        synchronized (entries) {
            entries.add(entry);

            // Maintain max entries (circular buffer)
            if (entries.size() > MAX_ENTRIES) {
                entries.remove(0);
            }
        }

        // Avoid leaking operational metadata to logcat in release builds.
        if (BuildConfig.DEBUG) {
            String logMessage = String.format(Locale.US,
                    "[AUDIT] T:%d E:%s S:%s Src:%s Det:%s Res:%s",
                    timestamp, eventType, severity, safeSource, safeDetails,
                    success ? "OK" : "FAIL");
            switch (severity) {
                case CRITICAL:
                case ERROR:
                    Log.e(TAG, logMessage);
                    break;
                case WARNING:
                    Log.w(TAG, logMessage);
                    break;
                default:
                    Log.i(TAG, logMessage);
            }
        }
    }

    public List<AuditEntry> getEntries() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    public int getEntryCount() {
        synchronized (entries) {
            return entries.size();
        }
    }

    public String exportToFile() {
        if (context == null) {
            Log.e(TAG, "Context not initialized, cannot export");
            return null;
        }

        try {
            File logDir = new File(context.getFilesDir(), AUDIT_LOG_DIR);
            if (!logDir.exists() && !logDir.mkdirs()) {
                Log.e(TAG, "Unable to create private audit export directory");
                return null;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US);
            String filename = "audit_log_" + sdf.format(new Date()) + ".txt";
            File logFile = new File(logDir, filename);

            try (FileOutputStream fos = new FileOutputStream(logFile, false)) {
                synchronized (entries) {
                    for (AuditEntry entry : entries) {
                        String line = String.format(Locale.US,
                                "%d|%s|%s|%s|%s|%s\n",
                                entry.timestamp,
                                entry.eventType,
                                entry.severity,
                                entry.source,
                                entry.details,
                                entry.success ? "OK" : "FAIL");
                        fos.write(line.getBytes(StandardCharsets.UTF_8));
                    }
                }
                fos.flush();
            }
            if (BuildConfig.DEBUG) Log.i(TAG, "Audit log exported to private app storage");
            return logFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Failed to export audit log", e);
            return null;
        }
    }

    public boolean isEnabled() {
        return true;
    }

    private static String sanitizeField(String value, int maximumLength) {
        if (value == null) return "";
        String sanitized = value.replace('\r', ' ').replace('\n', ' ').replace('|', '/');
        return sanitized.length() <= maximumLength ? sanitized : sanitized.substring(0, maximumLength);
    }
}

