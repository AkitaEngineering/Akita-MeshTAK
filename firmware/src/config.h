// File: firmware/src/config.h
// Description: Main configuration and constant definitions for the Heltec V3 firmware.
#ifndef CONFIG_H
#define CONFIG_H

#include <Arduino.h>

#define FIRMWARE_VERSION "0.2.0" 

// --- Device Identification ---
#define DEVICE_ID "AkitaNode01"

// --- Security Provisioning ---
// Replace this secret during provisioning. Keep firmware/plugin values aligned.
#define PROVISIONING_SECRET "REPLACE_WITH_DEPLOYMENT_SECRET"

// Compile-time guard: fail the build if the placeholder secret has not been
// replaced. Comment out only for local-bench testing behind ENABLE_MQTT=0.
#if !defined(ALLOW_PLACEHOLDER_SECRET)
  #define _AKITA_STRINGIFY(x) #x
  #define _AKITA_TO_STRING(x) _AKITA_STRINGIFY(x)
  static_assert(
    __builtin_strcmp(_AKITA_TO_STRING(PROVISIONING_SECRET), "REPLACE_WITH_DEPLOYMENT_SECRET") != 0,
    "PROVISIONING_SECRET still contains the placeholder value. "
    "Replace it before deploying, or define ALLOW_PLACEHOLDER_SECRET for bench testing.");
#endif

// --- Serial input safety ---
#define MAX_SERIAL_LINE_LENGTH 2048

// --- Rate limiting ---
// Minimum milliseconds between accepted commands on each transport.
#define CMD_RATE_LIMIT_MS 50
#define ENCRYPTED_PAYLOAD_PREFIX "ENC:"
#define ENCRYPTED_PAYLOAD_VERSION "v1"
#define ENCRYPTED_KEY_ID "k1"

// --- LoRa Configuration ---
#define LORA_REGION EU868

// --- Meshtastic Serial Bridge Configuration ---
#define MESH_SERIAL_RX_PIN -1
#define MESH_SERIAL_TX_PIN -1
#define MESH_SERIAL_BAUD 9600

// --- Connectivity Options ---
#define ENABLE_BLE 1
#define ENABLE_SERIAL 1
#define ENABLE_MQTT 0

// --- BLE Configuration (UUIDs MUST match Config.java) ---
#if defined(ENABLE_BLE) && ENABLE_BLE
  #define BLE_SERVICE_UUID        "YOUR_SERVICE_UUID"
  #define BLE_COT_CHARACTERISTIC_UUID "YOUR_COT_CHARACTERISTIC_UUID"
  #define BLE_WRITE_CHARACTERISTIC_UUID "YOUR_WRITE_CHARACTERISTIC_UUID"

  // Compile-time guard: fail the build if the BLE UUIDs still contain
  // placeholder values. Comment out only for bench testing.
  #if !defined(ALLOW_PLACEHOLDER_SECRET)
    static_assert(
      __builtin_strcmp(BLE_SERVICE_UUID, "YOUR_SERVICE_UUID") != 0,
      "BLE_SERVICE_UUID still contains the placeholder. "
      "Replace it with the real UUID from your firmware before deploying.");
  #endif
#endif

// --- COMMAND CONSTANTS ---
#define CMD_GET_BATT "CMD:GET_BATT"
#define CMD_ALERT_SOS "CMD:ALERT:SOS"
#define CMD_GET_VERSION "CMD:GET_VERSION"
#define CMD_MAILBOX_PUT_PREFIX "CMD:MAILBOX:PUT:"
#define CMD_PROVISION_STAGE_PREFIX "CMD:PROV:STAGE:"
#define STATUS_BATT_PREFIX "STATUS:BATT:"
#define STATUS_VERSION_PREFIX "STATUS:VERSION:"
#define STATUS_MAILBOX_ACK_PREFIX "STATUS:MAILBOX:ACK:"
#define STATUS_MAILBOX_RX_PREFIX "STATUS:MAILBOX:RX:"
#define STATUS_PROVISION_STAGED_PREFIX "STATUS:PROV:STAGED:"
#define STATUS_PROVISION_FAILED_PREFIX "STATUS:PROV:FAILED:"

// --- Display Enable ---
#define ENABLE_DISPLAY 1

// --- MQTT Configuration ---
#if defined(ENABLE_MQTT) && ENABLE_MQTT
  #define MQTT_SERVER "YOUR_MQTT_SERVER"
  #define MQTT_PORT 1883
  #define MQTT_TOPIC_PREFIX "akita/meshtak/"
  #define MQTT_WIFI_SSID "YOUR_WIFI_SSID"
  #define MQTT_WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
  #define MQTT_USERNAME "YOUR_MQTT_USERNAME"
  #define MQTT_PASSWORD "YOUR_MQTT_PASSWORD"

  // Compile-time guard: fail the build when MQTT is enabled but credentials
  // still contain placeholders.
  #if !defined(ALLOW_PLACEHOLDER_SECRET)
    static_assert(
      __builtin_strcmp(MQTT_WIFI_SSID, "YOUR_WIFI_SSID") != 0,
      "MQTT_WIFI_SSID still contains the placeholder. "
      "Set real WiFi credentials in config.h or define ALLOW_PLACEHOLDER_SECRET.");
  #endif
#endif

// --- Power Management Settings ---
#define BATTERY_CHECK_INTERVAL 60000 

#endif
