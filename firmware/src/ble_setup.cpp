// firmware/src/ble_setup.cpp
#ifdef ENABLE_BLE
#include "ble_setup.h"
#include "config.h"
#include "cot_generation.h"

BLEUUID serviceUUID(BLE_SERVICE_UUID);
BLEUUID cotCharacteristicUUID(BLE_COT_CHARACTERISTIC_UUID);
BLEUUID writeCharacteristicUUID(BLE_WRITE_CHARACTERISTIC_UUID); // Add this
BLEServer *pServer = nullptr;
BLECharacteristic *pCoTCharacteristic = nullptr;
BLECharacteristic *pWriteCharacteristic = nullptr; // Add this

class ServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      Serial.println("BLE Client Connected");
    };

    void onDisconnect(BLEServer* pServer) {
      Serial.println("BLE Client Disconnected");
      pServer->startAdvertising();
    }
};

bool setupBLE() {
  Serial.println("Initializing BLE...");
  BLEDevice::init(DEVICE_ID);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());

  BLEService *pService = pServer->createService(serviceUUID);

  pCoTCharacteristic = pService->createCharacteristic(
                      cotCharacteristicUUID,
                      BLECharacteristic::PROPERTY_READ |
                      BLECharacteristic::PROPERTY_WRITE |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  pCoTCharacteristic->setValue("Initial CoT");

  pWriteCharacteristic = pService->createCharacteristic(          // Add this
                      writeCharacteristicUUID,
                      BLECharacteristic::PROPERTY_WRITE |
                      BLECharacteristic::PROPERTY_WRITE_NR // Without Response
                    );

  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(serviceUUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->start();
  Serial.println("BLE Advertising started.");
  return true;
}

void loopBLE() {
  delay(100);
}

void sendDataBLE(const uint8_t* data, size_t len) {
  if (pWriteCharacteristic != nullptr && pServer->getConnectedCount() > 0) {
    pWriteCharacteristic->setValue(data, len);
    pWriteCharacteristic->notify();
    Serial.print("Sent data via BLE: ");
    Serial.write(data, len);
    Serial.println();
  } else {
    Serial.println("BLE not connected, cannot send data.");
  }
}
#endif
