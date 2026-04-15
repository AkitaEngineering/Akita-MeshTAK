// File: atak_plugin/src/com/akitaengineering/meshtak/Config.java
// Description: Central configuration file for ATAK plugin. ACTION REQUIRED: Replace placeholders.

package com.akitaengineering.meshtak;

import java.util.UUID;

/**
 * Configuration Constants for Akita MeshTAK Plugin.
 * * * ACTION REQUIRED: Replace ALL placeholders below with the actual IDs 
 * from your Heltec V3 firmware and device settings before compiling the APK.
 */
public final class Config {

    // --- Security Provisioning ---

    /** Replace with deployment-specific shared secret. Must match firmware PROVISIONING_SECRET. */
    public static final String PROVISIONING_SECRET = "REPLACE_WITH_DEPLOYMENT_SECRET";

    /** Returns true when the provisioning secret is still set to the compile-time placeholder. */
    public static boolean isPlaceholderSecret() {
        return "REPLACE_WITH_DEPLOYMENT_SECRET".equals(PROVISIONING_SECRET);
    }

    /** Prefix for encrypted payload envelopes exchanged over BLE/Serial. */
    public static final String ENCRYPTED_PAYLOAD_PREFIX = "ENC:";

    /** Envelope protocol version used for encrypted payloads. */
    public static final String ENCRYPTED_PAYLOAD_VERSION = "v1";

    /** Active key identifier for payload encryption; rotate on reprovisioning. */
    public static final String ENCRYPTED_KEY_ID = "k1";

    // --- BLE (Bluetooth Low Energy) Configuration ---
    
    /** UUID of the primary BLE Service provided by the Akita MeshTAK firmware. (REPLACE ME) */
    public static final UUID BLE_SERVICE_UUID = UUID.fromString("0000181A-0000-1000-8000-00805F9B34FB"); 
    
    /** UUID for the Characteristic used to receive CoT data (Notifications). (REPLACE ME) */
    public static final UUID COT_CHARACTERISTIC_UUID = UUID.fromString("00002A6E-0000-1000-8000-00805F9B34FB"); 
    
    /** UUID for the Characteristic used to send commands/data (Write). (REPLACE ME) */
    public static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("00002A6C-0000-1000-8000-00805F9B34FB");
    
    // --- Serial (USB) Configuration ---
    
    /** USB Vendor ID (Decimal) for the Heltec V3's serial chip. (REPLACE ME) */
    public static final int HELTEC_VENDOR_ID = 1027;
    
    /** USB Product ID (Decimal) for the Heltec V3's serial chip. (REPLACE ME) */
    public static final int HELTEC_PRODUCT_ID = 24577; 
    
    // --- COMMAND AND STATUS CONSTANTS ---
    
    /** Command prefix sent to the firmware to request battery status. */
    public static final String CMD_GET_BATT = "CMD:GET_BATT";
    
    /** Command sent to initiate a critical alert broadcast. */
    public static final String CMD_ALERT_SOS = "CMD:ALERT:SOS";

    /** Command prefix used to queue mission traffic for device-side mailbox delivery. */
    public static final String CMD_MAILBOX_PUT_PREFIX = "CMD:MAILBOX:PUT:";

    /** Command prefix used to stage provisioning material onto a connected device. */
    public static final String CMD_PROVISION_STAGE_PREFIX = "CMD:PROV:STAGE:";

    /** Prefix expected in the response string when receiving battery status. */
    public static final String STATUS_BATT_PREFIX = "STATUS:BATT:";
    
    /** Command prefix sent to the firmware to request version. */
    public static final String CMD_GET_VERSION = "CMD:GET_VERSION";
    
    /** Prefix expected in the response string when receiving version. */
    public static final String STATUS_VERSION_PREFIX = "STATUS:VERSION:";

    /** Prefix expected when the device acknowledges a mailbox message. */
    public static final String STATUS_MAILBOX_ACK_PREFIX = "STATUS:MAILBOX:ACK:";

    /** Prefix expected when the device forwards inbound non-CoT traffic from the mesh. */
    public static final String STATUS_MAILBOX_RX_PREFIX = "STATUS:MAILBOX:RX:";

    /** Prefix expected when a runtime provisioning stage succeeds on the device. */
    public static final String STATUS_PROVISION_STAGED_PREFIX = "STATUS:PROV:STAGED:";

    /** Prefix expected when a runtime provisioning stage fails on the device. */
    public static final String STATUS_PROVISION_FAILED_PREFIX = "STATUS:PROV:FAILED:";
    
    // --- ATAK Rendering Defaults ---
    
    /** Default CoT Type to use for rendering Meshtastic nodes if type data is missing. */
    public static final String DEFAULT_COT_TYPE = "a-h-G-U-T"; // Human/Ground/Friend/Unknown/Team
}
