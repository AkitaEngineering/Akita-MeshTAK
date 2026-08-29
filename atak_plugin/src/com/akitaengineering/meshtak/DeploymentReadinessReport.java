package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.akitaengineering.meshtak.ui.AkitaMockSettings;
import com.akitaengineering.meshtak.ui.AkitaProvisioningManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Operator-facing go/no-go snapshot of placeholder secrets, BLE UUIDs,
 * encryption, mock mode, audit, and OpenTAKServer settings.
 */
public final class DeploymentReadinessReport {

    public enum Severity {
        PASS,
        WARN,
        FAIL
    }

    public static final class Check {
        public final String name;
        public final Severity severity;
        public final String detail;

        Check(String name, Severity severity, String detail) {
            this.name = name;
            this.severity = severity;
            this.detail = detail;
        }
    }

    public static final class Report {
        public final List<Check> checks;
        public final int passCount;
        public final int warnCount;
        public final int failCount;

        Report(List<Check> checks) {
            this.checks = checks;
            int pass = 0;
            int warn = 0;
            int fail = 0;
            for (Check check : checks) {
                if (check.severity == Severity.PASS) {
                    pass++;
                } else if (check.severity == Severity.WARN) {
                    warn++;
                } else {
                    fail++;
                }
            }
            this.passCount = pass;
            this.warnCount = warn;
            this.failCount = fail;
        }

        public boolean isFieldReady() {
            return failCount == 0;
        }

        public String headline() {
            if (failCount > 0) {
                return String.format(Locale.US, "Not field-ready • %d failed checks", failCount);
            }
            if (warnCount > 0) {
                return String.format(Locale.US, "Ready with warnings • %d warnings", warnCount);
            }
            return "Field-ready software posture";
        }

        public String asText() {
            StringBuilder builder = new StringBuilder();
            builder.append(headline()).append('\n');
            for (Check check : checks) {
                builder.append(check.severity.name())
                        .append(": ")
                        .append(check.name)
                        .append(" — ")
                        .append(check.detail)
                        .append('\n');
            }
            return builder.toString().trim();
        }
    }

    private DeploymentReadinessReport() {
    }

    public static Report evaluate(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        List<Check> checks = new ArrayList<>();

        boolean placeholderSecret = AkitaProvisioningManager.isActiveSecretPlaceholder(context);
        checks.add(new Check(
                "Provisioning secret",
                placeholderSecret ? Severity.FAIL : Severity.PASS,
                placeholderSecret
                        ? "Placeholder or missing secret. Rotate before live traffic."
                        : "Per-device secret is present."));

        boolean encryption = AkitaProvisioningManager.isEncryptionEnabled(preferences)
                && SecurityManager.getInstance().isInitialized()
                && SecurityManager.getInstance().isEncryptionEnabled();
        boolean mockMode = AkitaMockSettings.isEnabled(preferences);
        if (mockMode) {
            checks.add(new Check("Encryption", Severity.WARN,
                    "Mock transport is using simulated AES/HMAC. Disable mock mode for field use."));
        } else if (!SecurityManager.getInstance().isInitialized()) {
            checks.add(new Check("Encryption", Severity.FAIL,
                    "Security manager is not initialized. Stage provisioning before live traffic."));
        } else if (!encryption) {
            checks.add(new Check("Encryption", Severity.FAIL, "Authenticated transport is not active."));
        } else {
            checks.add(new Check("Encryption", Severity.PASS,
                    "AES-256/HMAC " + SecurityManager.getInstance().getActiveKeyId() + " is active."));
        }

        checks.add(new Check(
                "Mock transport",
                mockMode ? Severity.FAIL : Severity.PASS,
                mockMode ? "Mock transport mode is enabled." : "Live bearers are selected."));

        AuditLogger auditLogger = AuditLogger.getInstance();
        checks.add(new Check(
                "Audit log",
                auditLogger.isEnabled() ? Severity.PASS : Severity.FAIL,
                auditLogger.isEnabled()
                        ? String.format(Locale.US, "Audit armed • %d events", auditLogger.getEntryCount())
                        : "Audit logging is disabled."));

        checks.add(bleUuidCheck("BLE service UUID", BuildConfig.AKITA_BLE_SERVICE_UUID,
                "0000181A-0000-1000-8000-00805F9B34FB"));
        checks.add(bleUuidCheck("BLE CoT characteristic UUID", BuildConfig.AKITA_COT_CHARACTERISTIC_UUID,
                "00002A6E-0000-1000-8000-00805F9B34FB"));
        checks.add(bleUuidCheck("BLE write characteristic UUID", BuildConfig.AKITA_WRITE_CHARACTERISTIC_UUID,
                "00002A6C-0000-1000-8000-00805F9B34FB"));

        boolean ssl = OpenTakStreamingClient.isSsl(preferences);
        boolean otsEnabled = OpenTakStreamingClient.isEnabled(preferences);
        String host = OpenTakStreamingClient.getHost(preferences);
        boolean hasCert = OpenTakCertificateStore.hasImportedCertificate(context);
        if (ssl && !hasCert) {
            checks.add(new Check("OpenTAKServer SSL", Severity.FAIL,
                    "SSL is enabled without an imported client PKCS#12. Connection is fail-closed."));
        } else if (ssl) {
            OpenTakCertificateStore.CertificateMetadata metadata = OpenTakCertificateStore.getImportedMetadata(context);
            boolean expired = metadata != null && metadata.isExpired(System.currentTimeMillis());
            checks.add(new Check("OpenTAKServer SSL", expired ? Severity.FAIL : Severity.PASS,
                    expired ? "Imported client certificate is expired." : OpenTakCertificateStore.describe(context)));
        } else if (otsEnabled && host.isEmpty()) {
            checks.add(new Check("OpenTAKServer", Severity.WARN,
                    "Native streaming is enabled but no host is configured. ATAK TAK server path can still be used."));
        } else if (otsEnabled) {
            checks.add(new Check("OpenTAKServer", Severity.PASS,
                    "Native " + (ssl ? "SSL" : "TCP") + " streaming target " + host + ":"
                            + OpenTakStreamingClient.getPort(preferences)));
        } else {
            checks.add(new Check("OpenTAKServer", Severity.PASS,
                    "Native streaming idle. ATAK TAK server path remains the default ingest."));
        }

        if (DeviceSecurityState.hasReport() && !DeviceSecurityState.hasFlashEncryption()) {
            checks.add(new Check("Controller flash encryption", Severity.WARN,
                    "Controller reported flash encryption off. Enable ESP32 flash encryption before fielding."));
        } else if (DeviceSecurityState.hasReport()) {
            checks.add(new Check("Controller flash encryption", Severity.PASS,
                    DeviceSecurityState.getHardwareSummary()));
        }

        String callsign = OperatorIdentity.getCallsign(preferences);
        checks.add(new Check("Operator identity",
                OperatorIdentity.hasConfiguredCallsign(preferences) ? Severity.PASS : Severity.WARN,
                OperatorIdentity.summarize(preferences)));

        if (BuildConfig.USING_ATAK_STUB) {
            checks.add(new Check("ATAK SDK", Severity.FAIL,
                    "Build is using ATAK stubs. Field APKs require the official ATAK SDK jar."));
        }

        return new Report(checks);
    }

    private static Check bleUuidCheck(String name, String value, String placeholder) {
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return new Check(name, Severity.FAIL, "UUID is malformed.");
        }
        if (value.equalsIgnoreCase(placeholder) || value.toUpperCase(Locale.US).startsWith("YOUR_")) {
            return new Check(name, Severity.FAIL, "Placeholder BLE UUID is still in this APK.");
        }
        return new Check(name, Severity.PASS, "Deployment UUID is set.");
    }
}
