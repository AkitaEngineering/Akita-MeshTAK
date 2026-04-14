package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AkitaMockSettings {

    public static final String PREF_MOCK_MODE = "mock_mode";
    public static final String PREF_MOCK_BLE_STATUS = "mock_ble_status";
    public static final String PREF_MOCK_SERIAL_STATUS = "mock_serial_status";
    public static final String PREF_MOCK_BATTERY_LEVEL = "mock_battery_level";

    private static final List<Integer> DEMO_PAYLOADS = Arrays.asList(118, 142, 96, 188, 164, 204, 176, 238, 220, 274, 248, 318);
    private static final List<String> DEMO_COMMANDS = Arrays.asList(
            "{\"type\":\"spotrep\",\"priority\":\"routine\"}",
            "ATAK Test Message!",
            "CUSTOM:REQUEST_STATUS");

    private AkitaMockSettings() {
    }

    public static boolean isEnabled(Context context) {
        return isEnabled(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public static boolean isEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(PREF_MOCK_MODE, false);
    }

    public static String getBleStatus(SharedPreferences preferences) {
        if (preferences.contains(PREF_MOCK_BLE_STATUS)) {
            return preferences.getString(PREF_MOCK_BLE_STATUS, "Connected");
        }
        return "ble".equals(preferences.getString("connection_method", "ble")) ? "Connected" : "Idle";
    }

    public static String getSerialStatus(SharedPreferences preferences) {
        if (preferences.contains(PREF_MOCK_SERIAL_STATUS)) {
            return preferences.getString(PREF_MOCK_SERIAL_STATUS, "Idle");
        }
        return "serial".equals(preferences.getString("connection_method", "ble")) ? "Connected" : "Idle";
    }

    public static int getBatteryPercent(SharedPreferences preferences) {
        String configuredValue = preferences.getString(PREF_MOCK_BATTERY_LEVEL, "78");
        try {
            int parsedValue = Integer.parseInt(configuredValue);
            return Math.max(0, Math.min(100, parsedValue));
        } catch (NumberFormatException ignored) {
            return 78;
        }
    }

    public static String getBatteryLabel(SharedPreferences preferences) {
        return getBatteryPercent(preferences) + "%";
    }

    public static boolean shouldUseDemoData(SharedPreferences preferences,
                                            int successfulSends,
                                            int failedSends,
                                            List<String> commandHistory,
                                            List<Integer> recentPayloads) {
        return isEnabled(preferences)
                && successfulSends == 0
                && failedSends == 0
                && (commandHistory == null || commandHistory.isEmpty())
                && (recentPayloads == null || recentPayloads.isEmpty());
    }

    public static List<Integer> getDemoPayloads() {
        return new ArrayList<>(DEMO_PAYLOADS);
    }

    public static List<String> getDemoCommands() {
        return new ArrayList<>(DEMO_COMMANDS);
    }

    public static int getDemoPlainCount() {
        return 6;
    }

    public static int getDemoJsonCount() {
        return 12;
    }

    public static int getDemoCustomCount() {
        return 6;
    }

    public static int getDemoSuccessfulSends() {
        return 24;
    }

    public static int getDemoFailedSends() {
        return 2;
    }

    public static long getDemoLastSendAt() {
        return System.currentTimeMillis() - 120000L;
    }

    public static int getDemoLastSendBytes() {
        return 318;
    }

    public static String getDemoLastSendFormat() {
        return "JSON";
    }

    public static String getDemoLastSendRoute() {
        return "BLE";
    }
}