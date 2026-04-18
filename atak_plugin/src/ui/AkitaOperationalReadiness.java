package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.akitaengineering.meshtak.AuditLogger;

import java.util.Locale;

public final class AkitaOperationalReadiness {

    private AkitaOperationalReadiness() {
    }

    public static boolean isProvisioningPlaceholder(Context context) {
        return AkitaProvisioningManager.isActiveSecretPlaceholder(context);
    }

    public static String getEncryptionStatus(Context context, boolean transportAttached) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean mockMode = AkitaMockSettings.isEnabled(preferences);
        com.akitaengineering.meshtak.SecurityManager securityManager = com.akitaengineering.meshtak.SecurityManager.getInstance();
        long failures = securityManager.getIntegrityFailures() + securityManager.getAuthFailures();

        if (mockMode) {
            return isProvisioningPlaceholder(context)
                    ? "Simulated AES/HMAC • rotate secret"
                    : "Simulated AES/HMAC secure session";
        }
        if (!AkitaProvisioningManager.isEncryptionEnabled(preferences)) {
            return "Encryption disabled by policy";
        }
        if (securityManager.isInitialized() && securityManager.isEncryptionEnabled()) {
            if (failures > 0) {
                return String.format(Locale.US, "AES/HMAC active • %d failures", failures);
            }
            if (securityManager.getMessagesEncrypted() > 0) {
                return String.format(Locale.US, "AES/HMAC active • %d frames", securityManager.getMessagesEncrypted());
            }
            return "AES-256 / HMAC active";
        }
        if (securityManager.isInitialized()) {
            return "Keys loaded • encryption standby";
        }
        return transportAttached ? "Provisioning failed" : "Awaiting provisioning";
    }

    public static String getAuditStatus(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        AuditLogger auditLogger = AuditLogger.getInstance();
        if (!auditLogger.isEnabled()) {
            return "Audit disabled";
        }
        int eventCount = auditLogger.getEntryCount();
        if (AkitaMockSettings.isEnabled(preferences)) {
            return eventCount > 0
                    ? String.format(Locale.US, "Mock audit active • %d events", eventCount)
                    : "Mock audit active";
        }
        return eventCount > 0
                ? String.format(Locale.US, "Audit active • %d events", eventCount)
                : "Audit armed • awaiting events";
    }

    public static String getInteroperabilityStatus(Context context, boolean transportAttached, boolean mapAvailable) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (AkitaMockSettings.isEnabled(preferences)) {
            return "ATAK CoT + partner bridges rehearsed";
        }
        if (transportAttached && mapAvailable) {
            return "ATAK CoT + partner bridges live";
        }
        if (mapAvailable) {
            return "ATAK overlay live • route standby";
        }
        if (transportAttached) {
            return "Partner bridges ready • map pending";
        }
        return "Partner bridges staged";
    }

    public static String getProvisioningStatus(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        com.akitaengineering.meshtak.SecurityManager securityManager = com.akitaengineering.meshtak.SecurityManager.getInstance();

        if (isProvisioningPlaceholder(context)) {
            return "Rotate deployment secret";
        }
        if (securityManager.isInitialized()) {
            return AkitaProvisioningManager.getRotationSummary(preferences);
        }
        if (AkitaMockSettings.isEnabled(preferences)) {
            return "Mock provisioning staged";
        }
        return "Provisioning pending";
    }

    public static String getToolbarSecurityStatus(Context context, boolean transportAttached) {
        if (isProvisioningPlaceholder(context)) {
            return "Security: Rotate deployment secret";
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!AkitaProvisioningManager.isEncryptionEnabled(preferences)) {
            return "Security: Encryption disabled";
        }
        if (AkitaMockSettings.isEnabled(preferences)) {
            return "Security: Simulated AES/HMAC + audit";
        }
        com.akitaengineering.meshtak.SecurityManager securityManager = com.akitaengineering.meshtak.SecurityManager.getInstance();
        if (securityManager.isInitialized() && securityManager.isEncryptionEnabled()) {
            return "Security: AES/HMAC + audit active";
        }
        return transportAttached ? "Security: Provisioning degraded" : "Security: Provisioning pending";
    }

    public static String getAssuranceSummary(Context context, boolean transportAttached, boolean mapAvailable) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String profileLabel = AkitaMissionProfile.getProfileLabel(preferences);
        boolean mockMode = AkitaMockSettings.isEnabled(preferences);
        com.akitaengineering.meshtak.SecurityManager securityManager = com.akitaengineering.meshtak.SecurityManager.getInstance();

        if (isProvisioningPlaceholder(context)) {
            return profileLabel + " workflows are rehearsable, but the deployment secret is still placeholder material. Rotate provisioning before field use.";
        }
        if (!AkitaProvisioningManager.isEncryptionEnabled(preferences)) {
            return profileLabel + " workflows are configured with encryption disabled. Re-enable protected transport before fielding.";
        }
        if (mockMode) {
            return profileLabel + " workflows are in simulated assurance mode with crypto, audit, and ATAK interoperability indicators available for dry runs.";
        }
        if (securityManager.isInitialized() && securityManager.isEncryptionEnabled() && transportAttached && mapAvailable) {
            return profileLabel + " workflows are backed by active encryption, audit logging, and ATAK/partner interoperability.";
        }
        if (transportAttached) {
            return profileLabel + " workflows are staged with transport attached. Complete live crypto and interoperability validation before fielding.";
        }
        return profileLabel + " workflows are staged. Attach the active transport to bring encrypted and interoperable traffic online.";
    }
}