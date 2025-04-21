// firmware/src/ble_setup.h
#ifndef BLE_SETUP_H
#define BLE_SETUP_H

#ifdef ENABLE_BLE
  #include <BLEServer.h>
  #include <BLEUtils.h>
  #include <BLEDevice.h>
  #include <BLECharacteristic.h>
  #include <BLEDescriptor.h>

  extern BLEUUID serviceUUID;
  extern BLEUUID cotCharacteristicUUID;
  extern BLEUUID writeCharacteristicUUID; // Add this
  extern BLEServer *pServer;
  extern BLECharacteristic *pCoTCharacteristic;
  extern BLECharacteristic *pWriteCharacteristic; // Add this

  bool setupBLE();
  void loopBLE();
  void sendDataBLE(const uint8_t* data, size_t len); // Add this
#endif

#endif
