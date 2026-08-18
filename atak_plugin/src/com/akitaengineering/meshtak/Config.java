// File: atak_plugin/src/com/akitaengineering/meshtak/Config.java
// Description: Central constants backed by deployment values supplied to Gradle.

package com.akitaengineering.meshtak;

import java.util.UUID;

/**
 * Configuration constants for the Akita MeshTAK plugin. Deployment values are
 * injected into BuildConfig by Gradle; do not place secrets in this source file.
 */
public final class Config {

    // --- Security Provisioning ---
    // No deployment secret is embedded in the APK. Provisioning state is held
    // by AkitaProvisioningManager in Android Keystore-backed storage.

    /** Prefix for encrypted payload envelopes exchanged over BLE/Serial. */
    public static final String ENCRYPTED_PAYLOAD_PREFIX = "ENC:";

    /** Envelope protocol version used for encrypted payloads. */
    public static final String ENCRYPTED_PAYLOAD_VERSION = "v2";

    /** First overlapping key slot. */
    public static final String ENCRYPTED_KEY_ID_K1 = "k1";

    /** Second overlapping key slot. */
    public static final String ENCRYPTED_KEY_ID_K2 = "k2";

    /** Default key identifier for a newly provisioned device. */
    public static final String ENCRYPTED_KEY_ID = ENCRYPTED_KEY_ID_K1;

    public static boolean isKnownKeyId(String keyId) {
        return ENCRYPTED_KEY_ID_K1.equals(keyId) || ENCRYPTED_KEY_ID_K2.equals(keyId);
    }

    public static String nextKeyId(String keyId) {
        return ENCRYPTED_KEY_ID_K2.equals(keyId) ? ENCRYPTED_KEY_ID_K1 : ENCRYPTED_KEY_ID_K2;
    }

    public static String normalizeKeyId(String keyId) {
        return isKnownKeyId(keyId) ? keyId : ENCRYPTED_KEY_ID;
    }

    // --- BLE (Bluetooth Low Energy) Configuration ---

    /** UUID of the primary BLE service provided by the Akita MeshTAK firmware. */
    public static final UUID BLE_SERVICE_UUID = UUID.fromString(BuildConfig.AKITA_BLE_SERVICE_UUID);

    /** UUID for the characteristic used to receive CoT data (notifications). */
    public static final UUID COT_CHARACTERISTIC_UUID = UUID.fromString(BuildConfig.AKITA_COT_CHARACTERISTIC_UUID);

    /** UUID for the characteristic used to send commands/data (write). */
    public static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString(BuildConfig.AKITA_WRITE_CHARACTERISTIC_UUID);

    // --- Serial (USB) Configuration ---

    /** USB vendor ID (decimal) for the Heltec V3's serial chip. */
    public static final int HELTEC_VENDOR_ID = BuildConfig.AKITA_HELTEC_VENDOR_ID;

    /** USB product ID (decimal) for the Heltec V3's serial chip. */
    public static final int HELTEC_PRODUCT_ID = BuildConfig.AKITA_HELTEC_PRODUCT_ID;

    // --- COMMAND AND STATUS CONSTANTS ---

    /** Command prefix sent to the firmware to request battery status. */
    public static final String CMD_GET_BATT = "CMD:GET_BATT";

    /** Command sent to initiate a critical alert broadcast. */
    public static final String CMD_ALERT_SOS = "CMD:ALERT:SOS";

    /** Command prefix used to queue mission traffic for device-side mailbox delivery. */
    public static final String CMD_MAILBOX_PUT_PREFIX = "CMD:MAILBOX:PUT:";

    /** Command prefix used to synchronize firmware CoT timestamps from the ATAK device clock. */
    public static final String CMD_TIME_SYNC_PREFIX = "CMD:TIME:SYNC:";

    /** Command prefix used to tag firmware CoT with an OpenTAKServer mission name. */
    public static final String CMD_COT_MISSION_PREFIX = "CMD:COT:MISSION:";

    /** Command prefix used to stage provisioning material onto a connected device. */
    public static final String CMD_PROVISION_STAGE_PREFIX = "CMD:PROV:STAGE:";

    /** Prefix expected in the response string when receiving battery status. */
    public static final String STATUS_BATT_PREFIX = "STATUS:BATT:";

    /** Command prefix sent to the firmware to request version. */
    public static final String CMD_GET_VERSION = "CMD:GET_VERSION";

    /** Command sent to request controller key-id and hardware-security posture. */
    public static final String CMD_GET_SEC_STATE = "CMD:GET_SEC_STATE";

    /** Prefix expected in the response string when receiving version. */
    public static final String STATUS_VERSION_PREFIX = "STATUS:VERSION:";

    /** Prefix expected when firmware reports key-id and hardware-security posture. */
    public static final String STATUS_SEC_STATE_PREFIX = "STATUS:SEC_STATE:";

    /** Prefix expected when firmware reports time synchronization status. */
    public static final String STATUS_TIME_SYNC_PREFIX = "STATUS:TIME:SYNC:";

    /** Prefix expected when firmware reports the active CoT mission tag. */
    public static final String STATUS_COT_MISSION_PREFIX = "STATUS:COT:MISSION:";

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

    private Config() {
    }
}
