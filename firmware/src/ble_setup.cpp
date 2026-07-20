// File: firmware/src/ble_setup.cpp
// Description: Implements BLE server, characteristics, and callbacks for ATAK.

#include "config.h"
#if defined(ENABLE_BLE) && ENABLE_BLE
#include "ble_setup.h"
#include "cot_generation.h"
#include "power_management.h" // For processIncomingCommand
#include "audit_log.h"        // For audit logging
#include "input_validation.h" // For input validation
#include "security.h"         // For payload encryption/decryption
#include "payload_codec.h"    // Shared encode/decode utilities
#include "transport_framing.h"
#include <algorithm>
#include <string.h>

BLEUUID serviceUUID(BLE_SERVICE_UUID);
BLEUUID cotCharacteristicUUID(BLE_COT_CHARACTERISTIC_UUID);
BLEUUID writeCharacteristicUUID(BLE_WRITE_CHARACTERISTIC_UUID);
BLEServer *pServer = nullptr;
BLECharacteristic *pCoTCharacteristic = nullptr;
BLECharacteristic *pWriteCharacteristic = nullptr;

class ServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      Serial.println("BLE Client Connected");
      logAuditEvent(AUDIT_EVENT_CONNECTION, 0, "BLE", "BLE client connected", true);
    };

    void onDisconnect(BLEServer* pServer) {
      Serial.println("BLE Client Disconnected");
      logAuditEvent(AUDIT_EVENT_DISCONNECTION, 0, "BLE", "BLE client disconnected", true);
      pServer->startAdvertising();
    }
};

// Callback for when ATAK writes a command to us
static unsigned long lastBleCommandMs = 0;

class CommandCallback: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        std::string value = pCharacteristic->getValue();
        if (value.length() > 0 && value.length() <= MAX_SERIAL_LINE_LENGTH) {
            String frame = "";
            frame.reserve(value.length());
            for (size_t i = 0; i < value.length(); i++) {
                frame += (char)value[i];
            }
            String cmd = "";
            bool completeFrame = consumeBleFrame(frame, cmd);
            for (size_t i = 0; i < frame.length(); i++) frame.setCharAt(i, '\0');
            std::fill(value.begin(), value.end(), '\0');
            if (!completeFrame) {
              return;
            }
            cmd.trim();

            // Rate-limit complete protocol messages, not individual BLE
            // fragments; otherwise valid multi-frame commands are discarded.
            unsigned long now = millis();
            if ((now - lastBleCommandMs) < CMD_RATE_LIMIT_MS) {
              logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 1, "BLE",
                           "Rate limit exceeded - command dropped", false);
              for (size_t i = 0; i < cmd.length(); i++) cmd.setCharAt(i, '\0');
              return;
            }
            lastBleCommandMs = now;

            String decodedCmd = "";
            if (!decodeIncomingPayload(cmd, decodedCmd, true)) {
              logAuditEvent(AUDIT_EVENT_AUTHENTICATION_FAILURE, 2, "BLE",
                     "Encrypted payload decode failed", false);
              return;
            }

            // Security: Log BLE data reception
            logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "BLE",
                   "Authenticated command received", true);

            // Input validation before processing
            ValidationResult validation = validateCommand(decodedCmd);
            if (validation == VALIDATION_OK) {
              processIncomingCommand(decodedCmd); // Process the command
            } else {
                logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "BLE",
                             "Invalid command - validation failed", false);
                Serial.printf("SECURITY: BLE command validation failed: %d\n", validation);
            }
            for (size_t i = 0; i < decodedCmd.length(); i++) decodedCmd.setCharAt(i, '\0');
            for (size_t i = 0; i < cmd.length(); i++) cmd.setCharAt(i, '\0');
        }
    }
};

bool setupBLE() {
  Serial.println("Initializing BLE...");
  BLEDevice::init(DEVICE_ID);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());

  BLEService *pService = pServer->createService(serviceUUID);

  // CoT Characteristic (Notifications to ATAK)
  pCoTCharacteristic = pService->createCharacteristic(
                      cotCharacteristicUUID,
                      BLECharacteristic::PROPERTY_INDICATE
                    );
  pCoTCharacteristic->addDescriptor(new BLE2902()); // Standard BLE descriptor

  // Write Characteristic (Commands from ATAK)
  pWriteCharacteristic = pService->createCharacteristic(
                      writeCharacteristicUUID,
                      BLECharacteristic::PROPERTY_WRITE
                    );
  pWriteCharacteristic->setCallbacks(new CommandCallback()); // Set the command callback

  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(serviceUUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->start();
  Serial.println("BLE Advertising started.");
  return true;
}

void loopBLE() {
  // Logic for handling BLE loop tasks, if any
  delay(100);
}

// Function to send CoT or Status data to ATAK
void sendDataBLE(const uint8_t* data, size_t len) {
  if (pCoTCharacteristic == nullptr || pServer->getConnectedCount() == 0) {
    Serial.println("BLE not connected, cannot send data.");
    logAuditEvent(AUDIT_EVENT_ERROR, 1, "BLE", "Send failed - not connected", false);
    return;
  }

  // Input validation for outgoing data
  if (len == 0 || len > MAX_MESSAGE_LENGTH) {
    logAuditEvent(AUDIT_EVENT_ERROR, 1, "BLE", "Send failed - invalid data length", false);
    return;
  }

  String outgoingPayload = "";
  if (!encodeOutgoingPayload(data, len, outgoingPayload)) {
    logAuditEvent(AUDIT_EVENT_ERROR, 2, "BLE", "Send failed - encryption error", false);
    return;
  }

  char messageIdBuffer[9];
  snprintf(messageIdBuffer, sizeof(messageIdBuffer), "%08lx", (unsigned long)esp_random());
  String messageId(messageIdBuffer);
  size_t frameCount = (outgoingPayload.length() + BLE_FRAME_PAYLOAD_BYTES - 1) / BLE_FRAME_PAYLOAD_BYTES;
  if (frameCount == 0 || frameCount > BLE_MAX_FRAME_PARTS) {
    logAuditEvent(AUDIT_EVENT_ERROR, 2, "BLE", "Send failed - frame count exceeded", false);
    return;
  }
  for (size_t index = 0; index < frameCount; index++) {
    size_t start = index * BLE_FRAME_PAYLOAD_BYTES;
    size_t end = start + BLE_FRAME_PAYLOAD_BYTES;
    if (end > outgoingPayload.length()) end = outgoingPayload.length();
    String frame = buildBleFrame(messageId, index, frameCount, outgoingPayload.substring(start, end));
    pCoTCharacteristic->setValue(frame.c_str());
    pCoTCharacteristic->indicate();
  }

  logAuditEvent(AUDIT_EVENT_DATA_SENT, 0, "BLE",
               String("Data sent, len: " + String(outgoingPayload.length())).c_str(), true);
}
#endif
