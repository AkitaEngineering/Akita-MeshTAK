// File: firmware/src/power_management.cpp
// Description: Implements power management, battery reading, and command processing.

#include "power_management.h"
#include "config.h"
#include "meshtastic_setup.h" // For Meshtastic object
#include "cot_generation.h"   // For CoT runtime metadata
#include "ble_setup.h"         // For sendDataBLE
#include "serial_bridge.h"   // For sendDataSerial
#include "provisioning_store.h"
#include "audit_log.h"        // For audit logging
#include "input_validation.h" // For input validation
#include "security.h"         // For security operations
#include "hardware_security.h"
#include "mailbox_escape.h"   // Shared escape/unescape
#include <Arduino.h>
#include <sys/time.h>

static void sendStatusToAtak(const String& response) {
#if defined(ENABLE_SERIAL) && ENABLE_SERIAL
    sendDataSerial((const uint8_t*)response.c_str(), response.length());
#endif
#if defined(ENABLE_BLE) && ENABLE_BLE
    sendDataBLE((const uint8_t*)response.c_str(), response.length());
#endif
}

static bool parseMailboxCommand(const String& cmd, String& messageId, String& format, String& payload) {
    String body = cmd.substring(strlen(CMD_MAILBOX_PUT_PREFIX));
    int firstSep = body.indexOf(':');
    int secondSep = body.indexOf(':', firstSep + 1);
    if (firstSep <= 0 || secondSep <= firstSep + 1) {
        return false;
    }

    messageId = body.substring(0, firstSep);
    format = body.substring(firstSep + 1, secondSep);
    payload = unescapeMailboxPayload(body.substring(secondSep + 1));
    if (messageId.length() == 0 || messageId.length() > 32 || payload.length() == 0
            || (format != "TEXT" && format != "JSON" && format != "CUSTOM")) {
        return false;
    }
    for (size_t i = 0; i < messageId.length(); i++) {
        char c = messageId.charAt(i);
        if (!isalnum(c) && c != '-' && c != '_') return false;
    }
    return true;
}

static void sendMailboxAck(const String& messageId, const char* status) {
    String response = String(STATUS_MAILBOX_ACK_PREFIX) + messageId + ":" + status;
    sendStatusToAtak(response);
}

// Standard Heltec V3 battery pin and voltage divider constants
const int VBAT_PIN = 37;
const float VOLTAGE_DIVIDER_R1 = 100.0; // kOhms
const float VOLTAGE_DIVIDER_R2 = 100.0; // kOhms
const float ADC_READING_CONVERSION = (3.3 / 4095.0); // 12-bit ADC, 3.3V reference

bool setupPowerManagement() {
  Serial.println("Initializing Power Management...");
  pinMode(VBAT_PIN, INPUT);
  return true;
}

float readBatteryVoltage() {
    // Read the ADC value
    int adcValue = analogRead(VBAT_PIN);

    // Convert ADC value to voltage
    float vbatVoltage = adcValue * ADC_READING_CONVERSION;

    // Account for the voltage divider
    // Vout = Vin * (R2 / (R1 + R2))
    // Vin = Vout * ((R1 + R2) / R2)
    float actualVoltage = vbatVoltage * ((VOLTAGE_DIVIDER_R1 + VOLTAGE_DIVIDER_R2) / VOLTAGE_DIVIDER_R2);

    // Heltec V3 specific correction factor (often needed)
    actualVoltage *= 1.1; // Adjust this factor based on real-world testing

    return actualVoltage;
}

void loopPowerManagement() {
  // This function can be used for future power-saving logic
}

void processIncomingCommand(const String& cmd) {
    // Input validation - CRITICAL for security
    ValidationResult validation = validateCommand(cmd);
    if (validation != VALIDATION_OK) {
        logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "SYSTEM",
                     "Invalid command received - validation failed", false);
        Serial.printf("SECURITY: Command validation failed: %d\n", validation);
        return;
    }

    // Never record command bodies: provisioning and mission commands can carry
    // sensitive material.
    logAuditEvent(AUDIT_EVENT_COMMAND_RECEIVED, 0, DEVICE_ID,
                  "Validated command received", true);

    if (cmd == CMD_ALERT_SOS) {
        Serial.println("ALERT RECEIVED: SOS COMMAND TRIGGERED! BROADCASTING.");

        // Log SOS trigger - CRITICAL event
        logAuditEvent(AUDIT_EVENT_SOS_TRIGGERED, 3, DEVICE_ID,
                     "SOS alert broadcast initiated", true);

        // --- Implement Meshtastic high-power emergency broadcast here ---
        String sosMessage = "SOS: Critical Alert from " + String(DEVICE_ID);
        // sendData(payload, len, channel, wantAck, hopLimit, txPower)
        // Using max power (20), default hop limit, on primary channel
        bool sent = mt_send_text(sosMessage.c_str(), BROADCAST_ADDR, 0);

        logAuditEvent(AUDIT_EVENT_COMMAND_EXECUTED, sent ? 0 : 2, DEVICE_ID,
                     sent ? "SOS broadcast successful" : "SOS broadcast failed", sent);

    } else if (cmd == CMD_GET_BATT) {
        float voltage = readBatteryVoltage();
        // Simple LiPo calculation (3.2V min, 4.2V max)
        int percent = (int)((voltage - 3.2) / (4.2 - 3.2) * 100);
        if (percent > 100) percent = 100;
        if (percent < 0) percent = 0;

        String response = String(STATUS_BATT_PREFIX) + percent + "%";
        sendStatusToAtak(response);

        logAuditEvent(AUDIT_EVENT_COMMAND_EXECUTED, 0, DEVICE_ID,
                     ("Battery status: " + String(percent) + "%").c_str(), true);
    } else if (cmd == CMD_GET_VERSION) {
        String response = String(STATUS_VERSION_PREFIX) + FIRMWARE_VERSION;
        sendStatusToAtak(response);

        logAuditEvent(AUDIT_EVENT_COMMAND_EXECUTED, 0, DEVICE_ID,
                     ("Version: " + String(FIRMWARE_VERSION)).c_str(), true);
    } else if (cmd == CMD_GET_SEC_STATE) {
        String previousKeyId = String(getPreviousKeyId());
        if (previousKeyId.length() == 0) {
            previousKeyId = "none";
        }
        String response = String(STATUS_SEC_STATE_PREFIX) + "key=" + String(getActiveKeyId())
            + ":prev=" + previousKeyId + ":" + formatHardwareSecurityFields();
        sendStatusToAtak(response);
        logAuditEvent(AUDIT_EVENT_COMMAND_EXECUTED, 0, DEVICE_ID,
                     "Security state reported", true);
    } else if (cmd.startsWith(CMD_TIME_SYNC_PREFIX)) {
        String epochText = cmd.substring(strlen(CMD_TIME_SYNC_PREFIX));
        epochText.trim();
        char* parseEnd = nullptr;
        unsigned long long epochSeconds = strtoull(epochText.c_str(), &parseEnd, 10);
        bool parsedAll = parseEnd != nullptr && *parseEnd == '\0';
        if (parsedAll && epochSeconds >= 1609459200ULL && epochSeconds <= 4102444800ULL) {
            timeval tv;
            tv.tv_sec = (time_t)epochSeconds;
            tv.tv_usec = 0;
            bool synced = settimeofday(&tv, nullptr) == 0;
            String response = String(STATUS_TIME_SYNC_PREFIX) + (synced ? "OK:" : "FAILED:") + epochText;
            sendStatusToAtak(response);
            logAuditEvent(AUDIT_EVENT_CONFIGURATION_CHANGE, synced ? 0 : 2, DEVICE_ID,
                         synced ? "CoT time synchronized" : "CoT time sync failed", synced);
        } else {
            sendStatusToAtak(String(STATUS_TIME_SYNC_PREFIX) + "FAILED:INVALID");
            logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 1, DEVICE_ID,
                         "Rejected invalid time sync command", false);
        }
    } else if (cmd.startsWith(CMD_COT_MISSION_PREFIX)) {
        String missionName = cmd.substring(strlen(CMD_COT_MISSION_PREFIX));
        missionName.trim();
        setCotMissionName(missionName);
        String activeMission = getCotMissionName();
        String response = String(STATUS_COT_MISSION_PREFIX) + (activeMission.length() > 0 ? activeMission : "CLEARED");
        sendStatusToAtak(response);
        logAuditEvent(AUDIT_EVENT_CONFIGURATION_CHANGE, 0, DEVICE_ID,
                     activeMission.length() > 0 ? "CoT mission tag updated" : "CoT mission tag cleared", true);
    } else if (cmd.startsWith(CMD_MAILBOX_PUT_PREFIX)) {
        String messageId = "";
        String format = "";
        String payload = "";
        if (!parseMailboxCommand(cmd, messageId, format, payload)) {
            logAuditEvent(AUDIT_EVENT_ERROR, 1, DEVICE_ID,
                         "Mailbox command parse failed", false);
            sendMailboxAck("UNKNOWN", "FAILED");
            return;
        }

        bool sent = sendMailboxPayloadOverMesh(messageId, format, payload);
        sendMailboxAck(messageId, sent ? "IN_FLIGHT" : "FAILED");
        logAuditEvent(AUDIT_EVENT_COMMAND_EXECUTED, sent ? 0 : 2, DEVICE_ID,
                     ("Mailbox relay " + format + " " + messageId).c_str(), sent);
    } else if (cmd.startsWith(CMD_PROVISION_STAGE_PREFIX)) {
        String provisioningValue = cmd.substring(strlen(CMD_PROVISION_STAGE_PREFIX));
        int separator = provisioningValue.lastIndexOf(':');
        String sharedSecret = provisioningValue.substring(0, separator);
        String epochText = provisioningValue.substring(separator + 1);
        sharedSecret.trim();
        unsigned long long epochSeconds = strtoull(epochText.c_str(), nullptr, 10);
        timeval tv;
        tv.tv_sec = (time_t)epochSeconds;
        tv.tv_usec = 0;
        bool clockSet = epochSeconds >= 1609459200ULL && epochSeconds <= 4102444800ULL
            && settimeofday(&tv, nullptr) == 0;
        bool staged = clockSet && persistProvisioningSecret(sharedSecret)
            && initSecurityFromStore();
        String responsePrefix = staged ? STATUS_PROVISION_STAGED_PREFIX : STATUS_PROVISION_FAILED_PREFIX;
        String response = String(responsePrefix) + ENCRYPTED_PAYLOAD_VERSION + ":" + String(getActiveKeyId());
        sendStatusToAtak(response);
        logAuditEvent(AUDIT_EVENT_CONFIGURATION_CHANGE, staged ? 0 : 2, DEVICE_ID,
                     staged ? "Runtime provisioning staged" : "Runtime provisioning stage failed", staged);
        for (size_t i = 0; i < sharedSecret.length(); i++) sharedSecret.setCharAt(i, '\0');
        for (size_t i = 0; i < provisioningValue.length(); i++) provisioningValue.setCharAt(i, '\0');
    } else {
        // Unknown command
        logAuditEvent(AUDIT_EVENT_ERROR, 1, DEVICE_ID,
                     "Unknown validated command", false);
    }
}
