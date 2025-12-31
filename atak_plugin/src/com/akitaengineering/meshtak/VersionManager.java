// File: atak_plugin/src/com/akitaengineering/meshtak/VersionManager.java
// Description: Version checking and compatibility management.
// Ensures firmware and plugin compatibility

package com.akitaengineering.meshtak;

import android.util.Log;

/**
 * Version Manager for checking compatibility between plugin and firmware.
 */
public class VersionManager {
    private static final String TAG = "VersionManager";
    
    // Plugin version
    public static final String PLUGIN_VERSION = "0.2.0";
    
    // Minimum supported firmware version
    public static final String MIN_FIRMWARE_VERSION = "0.2.0";
    
    // Maximum supported firmware version (for compatibility checking)
    public static final String MAX_FIRMWARE_VERSION = "1.0.0";
    
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
            int part1 = (i < parts1.length) ? Integer.parseInt(parts1[i]) : 0;
            int part2 = (i < parts2.length) ? Integer.parseInt(parts2[i]) : 0;
            
            if (part1 < part2) {
                return -1;
            } else if (part1 > part2) {
                return 1;
            }
        }
        
        return 0;
    }
    
    /**
     * Check if firmware version is compatible with plugin.
     */
    public static boolean isFirmwareCompatible(String firmwareVersion) {
        if (firmwareVersion == null || firmwareVersion.isEmpty()) {
            Log.w(TAG, "Firmware version is null or empty");
            return false;
        }
        
        int minCompare = compareVersions(firmwareVersion, MIN_FIRMWARE_VERSION);
        int maxCompare = compareVersions(firmwareVersion, MAX_FIRMWARE_VERSION);
        
        boolean compatible = (minCompare >= 0 && maxCompare <= 0);
        
        if (!compatible) {
            Log.w(TAG, "Firmware version " + firmwareVersion + " is not compatible. " +
                  "Required: " + MIN_FIRMWARE_VERSION + " - " + MAX_FIRMWARE_VERSION);
        }
        
        return compatible;
    }
    
    /**
     * Get plugin version string.
     */
    public static String getPluginVersion() {
        return PLUGIN_VERSION;
    }
}

