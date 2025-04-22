// firmware/src/serial_bridge.h
#ifndef SERIAL_BRIDGE_H
#define SERIAL_BRIDGE_H

#ifdef ENABLE_SERIAL
  bool setupSerialBridge();
  void loopSerialBridge();
  void sendDataSerial(const uint8_t* data, size_t len); // Add this
#endif

#endif
