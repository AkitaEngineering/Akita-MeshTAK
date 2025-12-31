// File: firmware/src/config.h
// Description: Main configuration and constant definitions for the Heltec V3 firmware.
#ifndef CONFIG_H
#define CONFIG_H

#define FIRMWARE_VERSION "0.2.0" 

// --- Device Identification ---
#define DEVICE_ID "AkitaNode01"

// --- LoRa Configuration ---
#define LORA_REGION EU868

// --- Connectivity Options ---
#define ENABLE_BLE true
#define ENABLE_SERIAL true 

// --- BLE Configuration (UUIDs MUST match Config.java) ---
#ifdef ENABLE_BLE
  #define BLE_SERVICE_UUID        "YOUR_SERVICE_UUID"
  #define BLE_COT_CHARACTERISTIC_UUID "YOUR_COT_CHARACTERISTIC_UUID"
  #define BLE_WRITE_CHARACTERISTIC_UUID "YOUR_WRITE_CHARACTERISTIC_UUID"
#endif

// --- COMMAND CONSTANTS ---
#define CMD_GET_BATT "CMD:GET_BATT"
#define CMD_ALERT_SOS "CMD:ALERT:SOS"
#define CMD_GET_VERSION "CMD:GET_VERSION"
#define STATUS_BATT_PREFIX "STATUS:BATT:"
#define STATUS_VERSION_PREFIX "STATUS:VERSION:"

// --- Display Enable ---
#define ENABLE_DISPLAY true

// --- Power Management Settings ---
#define BATTERY_CHECK_INTERVAL 60000 
float readBatteryVoltage();
void processIncomingCommand(const String& cmd);

#endif
