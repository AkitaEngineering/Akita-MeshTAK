// File: atak_plugin/src/com/akitaengineering/meshtak/VersionManager.java
// Description: Version reporting and compatibility management.

package com.akitaengineering.meshtak;

import android.util.Log;

/**
 * Version Manager for plugin and firmware version helpers.
 */
public class VersionManager {
    private static final String TAG = "VersionManager";

    /**
     * Compare version strings (format: "major.minor.patch")
     * Returns: -1 if v1 < v2, 0 if v1 == v2, 1 if v1 > v2
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return 0;
        }

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int part1 = (i < parts1.length) ? parseVersionPart(parts1[i]) : 0;
            int part2 = (i < parts2.length) ? parseVersionPart(parts2[i]) : 0;

            if (part1 < part2) {
                return -1;
            } else if (part1 > part2) {
                return 1;
            }
        }

        return 0;
    }

    private static int parseVersionPart(String part) {
        if (part == null) {
            return 0;
        }
        try {
            return Integer.parseInt(part.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid version segment: " + part + ", treating as 0");
            return 0;
        }
    }

    /**
     * Check if firmware version is compatible with plugin.
     *
     * Compatibility is an inclusive semantic-version range. Unknown or malformed
     * versions are rejected because protocol compatibility is safety-critical.
     */
    public static boolean isFirmwareCompatible(String firmwareVersion) {
        if (firmwareVersion == null || firmwareVersion.trim().isEmpty()) {
            Log.w(TAG, "Firmware version is null or empty");
            return false;
        }

        if (!isStrictSemanticVersion(firmwareVersion)) {
            Log.w(TAG, "Malformed firmware version: " + firmwareVersion);
            return false;
        }
        return compareVersions(firmwareVersion, BuildConfig.MIN_FIRMWARE_VERSION) >= 0
                && compareVersions(firmwareVersion, BuildConfig.MAX_FIRMWARE_VERSION) <= 0;
    }

    private static boolean isStrictSemanticVersion(String version) {
        return version != null && version.trim().matches("\\d+\\.\\d+\\.\\d+");
    }

    /**
     * Get plugin version string.
     */
    public static String getPluginVersion() {
        return BuildConfig.VERSION_NAME;
    }
}

