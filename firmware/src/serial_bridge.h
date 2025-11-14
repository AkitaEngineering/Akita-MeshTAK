// File: firmware/src/serial_bridge.h
// Description: Declares Serial bridge functions for ATAK.
#ifndef SERIAL_BRIDGE_H
#define SERIAL_BRIDGE_H

#ifdef ENABLE_SERIAL
  #include <Arduino.h> // Include for size_t
  bool setupSerialBridge();
  void loopSerialBridge();
  void sendDataSerial(const uint8_t* data, size_t len);
#endif

#endif
