package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.akitaengineering.meshtak.ui.AkitaMockSettings;
import com.akitaengineering.meshtak.ui.AkitaMissionProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Writes a redacted field diagnostics bundle. Secrets, passwords, and raw
 * mailbox payloads are never included.
 */
public final class FieldDiagnosticsExporter {

    private static final String TAG = "FieldDiagnostics";
    private static final Pattern SECRET_KEY = Pattern.compile("password|secret|token|keystore|bundle", Pattern.CASE_INSENSITIVE);

    private FieldDiagnosticsExporter() {
    }

    public static String export(Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        try {
            JSONObject bundle = buildBundle(appContext, preferences);
            File directory = new File(appContext.getFilesDir(), "diagnostics");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Unable to create diagnostics directory");
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            File output = new File(directory, "akita-diagnostics-" + format.format(new Date()) + ".json");
            byte[] json = bundle.toString(2).getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream stream = new FileOutputStream(output)) {
                stream.write(json);
                stream.flush();
            }
            AuditLogger.getInstance().log(AuditLogger.EventType.CONFIGURATION_CHANGE, AuditLogger.Severity.INFO,
                    "Diagnostics", "Redacted diagnostics exported", true);
            return output.getAbsolutePath();
        } catch (Exception exception) {
            Log.e(TAG, "Failed to export diagnostics", exception);
            AuditLogger.getInstance().log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                    "Diagnostics", "Diagnostics export failed", false);
            return null;
        }
    }

    static JSONObject buildBundle(Context context, SharedPreferences preferences) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("generatedAt", CotEventFactory.formatCotTime(System.currentTimeMillis()));
        root.put("pluginVersion", BuildConfig.VERSION_NAME);
        root.put("minFirmware", BuildConfig.MIN_FIRMWARE_VERSION);
        root.put("maxFirmware", BuildConfig.MAX_FIRMWARE_VERSION);
        root.put("usingAtakStub", BuildConfig.USING_ATAK_STUB);
        root.put("mockMode", AkitaMockSettings.isEnabled(preferences));
        root.put("missionProfile", AkitaMissionProfile.getProfileLabel(preferences));
        root.put("operator", OperatorIdentity.summarize(preferences));
        root.put("missionName", OperatorIdentity.getMissionName(preferences));

        DeploymentReadinessReport.Report report = DeploymentReadinessReport.evaluate(context);
        JSONArray checks = new JSONArray();
        for (DeploymentReadinessReport.Check check : report.checks) {
            JSONObject item = new JSONObject();
            item.put("name", check.name);
            item.put("severity", check.severity.name());
            item.put("detail", redact(check.detail));
            checks.put(item);
        }
        JSONObject readiness = new JSONObject();
        readiness.put("headline", report.headline());
        readiness.put("failCount", report.failCount);
        readiness.put("warnCount", report.warnCount);
        readiness.put("passCount", report.passCount);
        readiness.put("checks", checks);
        root.put("readiness", readiness);

        OpenTakStreamingClient.HealthSnapshot health = OpenTakStreamingClient.getInstance().getHealth(preferences);
        JSONObject server = new JSONObject();
        server.put("enabled", health.enabled);
        server.put("ssl", health.ssl);
        server.put("endpoint", health.endpointLabel());
        server.put("connected", health.connected);
        server.put("publishedCount", health.publishedCount);
        server.put("detail", redact(health.detail));
        server.put("certificate", OpenTakCertificateStore.describe(context));
        root.put("openTakServer", server);

        JSONObject mailbox = new JSONObject();
        AkitaMissionControl.QueueSnapshot snapshot = AkitaMissionControl.getInstance(context)
                .getQueueSnapshot(AkitaMissionControl.isAutoFailoverEnabled(preferences));
        mailbox.put("pending", snapshot.pendingCount);
        mailbox.put("inFlight", snapshot.inFlightCount);
        mailbox.put("delivered", snapshot.deliveredCount);
        mailbox.put("failed", snapshot.failedCount);
        mailbox.put("summary", redact(snapshot.summary));
        root.put("mailbox", mailbox);

        JSONObject audit = new JSONObject();
        audit.put("enabled", AuditLogger.getInstance().isEnabled());
        audit.put("eventCount", AuditLogger.getInstance().getEntryCount());
        root.put("audit", audit);

        if (DeviceSecurityState.hasReport()) {
            JSONObject device = new JSONObject();
            device.put("keySummary", DeviceSecurityState.getKeySummary());
            device.put("hardwareSummary", DeviceSecurityState.getHardwareSummary());
            device.put("flashEncryption", DeviceSecurityState.hasFlashEncryption());
            root.put("controller", device);
        }

        JSONArray preferenceKeys = new JSONArray();
        for (String key : preferences.getAll().keySet()) {
            if (SECRET_KEY.matcher(key).find()) {
                continue;
            }
            preferenceKeys.put(key);
        }
        root.put("preferenceKeys", preferenceKeys);
        return root;
    }

    static String redact(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("(?i)(password|secret|token|bundle)=\\S+", "$1=REDACTED");
    }
}
