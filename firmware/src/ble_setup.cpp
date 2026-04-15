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
            // Rate limiting
            unsigned long now = millis();
            if ((now - lastBleCommandMs) < CMD_RATE_LIMIT_MS) {
              logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 1, "BLE",
                           "Rate limit exceeded – command dropped", false);
              return;
            }
            lastBleCommandMs = now;
            String cmd = "";
            cmd.reserve(value.length());
            for (size_t i = 0; i < value.length(); i++) {
                cmd += (char)value[i];
            }
            cmd.trim(); // Clean up any whitespace

            String decodedCmd = "";
            if (!decodeIncomingPayload(cmd, decodedCmd)) {
              logAuditEvent(AUDIT_EVENT_AUTHENTICATION_FAILURE, 2, "BLE",
                     "Encrypted payload decode failed", false);
              return;
            }
            
            // Security: Log BLE data reception
            logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "BLE", 
                   ("Data received: " + decodedCmd.substring(0, 32)).c_str(), true);
            
            Serial.print("Received command via BLE: ");
            Serial.println(decodedCmd);
            
            // Input validation before processing
            ValidationResult validation = validateCommand(decodedCmd);
            if (validation == VALIDATION_OK) {
              processIncomingCommand(decodedCmd); // Process the command
            } else {
                logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "BLE",
                             "Invalid command - validation failed", false);
                Serial.printf("SECURITY: BLE command validation failed: %d\n", validation);
            }
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
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  pCoTCharacteristic->addDescriptor(new BLE2902()); // Standard BLE descriptor

  // Write Characteristic (Commands from ATAK)
  pWriteCharacteristic = pService->createCharacteristic(
                      writeCharacteristicUUID,
                      BLECharacteristic::PROPERTY_WRITE |
                      BLECharacteristic::PROPERTY_WRITE_NR 
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
void sendDataBLE(const uint8_t* data, size_t len, bool forcePlaintext) {
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
  if (forcePlaintext) {
    outgoingPayload = String((const char*)data).substring(0, len);
  } else if (!encodeOutgoingPayload(data, len, outgoingPayload)) {
    logAuditEvent(AUDIT_EVENT_ERROR, 2, "BLE", "Send failed - encryption error", false);
    return;
  }
  
  pCoTCharacteristic->setValue(outgoingPayload.c_str());
  pCoTCharacteristic->notify();
  
  {
    Serial.print("Sent data via BLE: ");
    Serial.print(outgoingPayload.substring(0, 64)); // Limit serial output
    Serial.println();
    logAuditEvent(AUDIT_EVENT_DATA_SENT, 0, "BLE", 
                 String("Data sent, len: " + String(outgoingPayload.length())).c_str(), true);
  }
}
#endif
