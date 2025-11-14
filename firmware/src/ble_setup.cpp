// File: firmware/src/ble_setup.cpp
// Description: Implements BLE server, characteristics, and callbacks for ATAK.

#ifdef ENABLE_BLE
#include "ble_setup.h"
#include "config.h"
#include "cot_generation.h"
#include "power_management.h" // For processIncomingCommand

BLEUUID serviceUUID(BLE_SERVICE_UUID);
BLEUUID cotCharacteristicUUID(BLE_COT_CHARACTERISTIC_UUID);
BLEUUID writeCharacteristicUUID(BLE_WRITE_CHARACTERISTIC_UUID);
BLEServer *pServer = nullptr;
BLECharacteristic *pCoTCharacteristic = nullptr;
BLECharacteristic *pWriteCharacteristic = nullptr;

class ServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      Serial.println("BLE Client Connected");
    };

    void onDisconnect(BLEServer* pServer) {
      Serial.println("BLE Client Disconnected");
      pServer->startAdvertising();
    }
};

// Callback for when ATAK writes a command to us
class CommandCallback: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        std::string value = pCharacteristic->getValue();
        if (value.length() > 0) {
            String cmd = "";
            for (int i = 0; i < value.length(); i++) {
                cmd += (char)value[i];
            }
            cmd.trim(); // Clean up any whitespace
            Serial.print("Received command via BLE: ");
            Serial.println(cmd);
            processIncomingCommand(cmd); // Process the command
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
void sendDataBLE(const uint8_t* data, size_t len) {
  if (pCoTCharacteristic != nullptr && pServer->getConnectedCount() > 0) {
    pCoTCharacteristic->setValue(data, len);
    pCoTCharacteristic->notify();
    Serial.print("Sent data via BLE: ");
    Serial.write(data, len);
    Serial.println();
  } else {
    Serial.println("BLE not connected, cannot send data.");
  }
}
#endif
