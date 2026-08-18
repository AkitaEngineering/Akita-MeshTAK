// File: firmware/src/config.h
// Description: Main configuration and constant definitions for the Heltec V3 firmware.
#ifndef CONFIG_H
#define CONFIG_H

#include <Arduino.h>

#ifndef AKITA_VERSION_NAME
#define AKITA_VERSION_NAME "0.2.1"
#endif

#define FIRMWARE_VERSION AKITA_VERSION_NAME

// --- Device Identification ---
#ifndef DEVICE_ID
#define DEVICE_ID "AkitaNode01"
#endif

// --- Security Provisioning ---
// Secrets are accepted only during the physical-presence provisioning window
// and are stored in NVS; deployment secrets are never compiled into firmware.
#ifndef PROVISION_BUTTON_PIN
#define PROVISION_BUTTON_PIN 0
#endif
#define PROVISIONING_WINDOW_MS 120000UL

// --- Serial input safety ---
#define MAX_SERIAL_LINE_LENGTH 2048

// --- Rate limiting ---
// Minimum milliseconds between accepted commands on each transport.
#define CMD_RATE_LIMIT_MS 50
#define ENCRYPTED_PAYLOAD_PREFIX "ENC:"
#define ENCRYPTED_PAYLOAD_VERSION "v2"
#define KEY_ID_K1 "k1"
#define KEY_ID_K2 "k2"
#define ENCRYPTED_KEY_ID KEY_ID_K1

static inline bool isKnownKeyId(const String& keyId) {
  return keyId == KEY_ID_K1 || keyId == KEY_ID_K2;
}

static inline String nextKeyId(const String& keyId) {
  return keyId == KEY_ID_K2 ? String(KEY_ID_K1) : String(KEY_ID_K2);
}

// --- LoRa Configuration ---
#define LORA_REGION EU868

// --- Meshtastic Serial Bridge Configuration ---
#ifndef MESH_SERIAL_RX_PIN
#define MESH_SERIAL_RX_PIN -1
#endif
#ifndef MESH_SERIAL_TX_PIN
#define MESH_SERIAL_TX_PIN -1
#endif
#define MESH_SERIAL_BAUD 9600

#if !defined(ALLOW_UNWIRED_MESH_BRIDGE)
  static_assert(MESH_SERIAL_RX_PIN >= 0 && MESH_SERIAL_TX_PIN >= 0,
    "This application is a Meshtastic companion controller. Set non-negative "
    "MESH_SERIAL_RX_PIN and MESH_SERIAL_TX_PIN values connected to a separate "
    "Meshtastic node before building a deployment image.");
#endif

// --- Connectivity Options ---
#define ENABLE_BLE 1
#define ENABLE_SERIAL 1
#define ENABLE_MQTT 0

// --- BLE Configuration (UUIDs MUST match Config.java) ---
#if defined(ENABLE_BLE) && ENABLE_BLE
  #ifndef BLE_SERVICE_UUID
    #define BLE_SERVICE_UUID        "YOUR_SERVICE_UUID"
  #endif
  #ifndef BLE_COT_CHARACTERISTIC_UUID
    #define BLE_COT_CHARACTERISTIC_UUID "YOUR_COT_CHARACTERISTIC_UUID"
  #endif
  #ifndef BLE_WRITE_CHARACTERISTIC_UUID
    #define BLE_WRITE_CHARACTERISTIC_UUID "YOUR_WRITE_CHARACTERISTIC_UUID"
  #endif

  // Compile-time guard: fail the build if the BLE UUIDs still contain
  // placeholder values. Comment out only for bench testing.
  #if !defined(ALLOW_PLACEHOLDER_SECRET)
    static_assert(
      __builtin_strcmp(BLE_SERVICE_UUID, "YOUR_SERVICE_UUID") != 0,
      "BLE_SERVICE_UUID still contains the placeholder. "
      "Replace it with the real UUID from your firmware before deploying.");
    static_assert(
      __builtin_strcmp(BLE_COT_CHARACTERISTIC_UUID, "YOUR_COT_CHARACTERISTIC_UUID") != 0,
      "BLE_COT_CHARACTERISTIC_UUID still contains the placeholder. "
      "Replace it with the real UUID from your firmware before deploying.");
    static_assert(
      __builtin_strcmp(BLE_WRITE_CHARACTERISTIC_UUID, "YOUR_WRITE_CHARACTERISTIC_UUID") != 0,
      "BLE_WRITE_CHARACTERISTIC_UUID still contains the placeholder. "
      "Replace it with the real UUID from your firmware before deploying.");
  #endif
#endif

// --- COMMAND CONSTANTS ---
#define CMD_GET_BATT "CMD:GET_BATT"
#define CMD_ALERT_SOS "CMD:ALERT:SOS"
#define CMD_GET_VERSION "CMD:GET_VERSION"
#define CMD_GET_SEC_STATE "CMD:GET_SEC_STATE"
#define CMD_TIME_SYNC_PREFIX "CMD:TIME:SYNC:"
#define CMD_COT_MISSION_PREFIX "CMD:COT:MISSION:"
#define CMD_MAILBOX_PUT_PREFIX "CMD:MAILBOX:PUT:"
#define CMD_PROVISION_STAGE_PREFIX "CMD:PROV:STAGE:"
#define STATUS_BATT_PREFIX "STATUS:BATT:"
#define STATUS_VERSION_PREFIX "STATUS:VERSION:"
#define STATUS_SEC_STATE_PREFIX "STATUS:SEC_STATE:"
#define STATUS_TIME_SYNC_PREFIX "STATUS:TIME:SYNC:"
#define STATUS_COT_MISSION_PREFIX "STATUS:COT:MISSION:"
#define STATUS_MAILBOX_ACK_PREFIX "STATUS:MAILBOX:ACK:"
#define STATUS_MAILBOX_RX_PREFIX "STATUS:MAILBOX:RX:"
#define STATUS_PROVISION_STAGED_PREFIX "STATUS:PROV:STAGED:"
#define STATUS_PROVISION_FAILED_PREFIX "STATUS:PROV:FAILED:"

// --- Display Enable ---
#define ENABLE_DISPLAY 1

// --- MQTT Configuration ---
#if defined(ENABLE_MQTT) && ENABLE_MQTT
  // The current MQTT client uses a plaintext TCP bearer. It is intentionally
  // blocked unless a bench-only override is explicit so it cannot be enabled
  // accidentally in a field image. Production MQTT requires a future TLS client.
  #if !defined(ALLOW_INSECURE_MQTT)
    #error "MQTT is not production-authorized without TLS. Keep ENABLE_MQTT=0, or define ALLOW_INSECURE_MQTT for isolated bench testing only."
  #endif

  #ifndef MQTT_SERVER
    #define MQTT_SERVER "YOUR_MQTT_SERVER"
  #endif
  #ifndef MQTT_PORT
    #define MQTT_PORT 1883
  #endif
  #ifndef MQTT_TOPIC_PREFIX
    #define MQTT_TOPIC_PREFIX "akita/meshtak/"
  #endif
  #ifndef MQTT_WIFI_SSID
    #define MQTT_WIFI_SSID "YOUR_WIFI_SSID"
  #endif
  #ifndef MQTT_WIFI_PASSWORD
    #define MQTT_WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
  #endif
  #ifndef MQTT_USERNAME
    #define MQTT_USERNAME "YOUR_MQTT_USERNAME"
  #endif
  #ifndef MQTT_PASSWORD
    #define MQTT_PASSWORD "YOUR_MQTT_PASSWORD"
  #endif

  // Compile-time guard: fail the build when MQTT is enabled but credentials
  // still contain placeholders.
  #if !defined(ALLOW_PLACEHOLDER_SECRET)
    static_assert(
      __builtin_strcmp(MQTT_SERVER, "YOUR_MQTT_SERVER") != 0,
      "MQTT_SERVER still contains the placeholder. "
      "Set the deployment broker address before enabling MQTT.");
    static_assert(
      __builtin_strcmp(MQTT_WIFI_SSID, "YOUR_WIFI_SSID") != 0,
      "MQTT_WIFI_SSID still contains the placeholder. "
      "Set real WiFi credentials in config.h or define ALLOW_PLACEHOLDER_SECRET.");
    static_assert(
      __builtin_strcmp(MQTT_WIFI_PASSWORD, "YOUR_WIFI_PASSWORD") != 0,
      "MQTT_WIFI_PASSWORD still contains the placeholder. "
      "Set real WiFi credentials in config.h or define ALLOW_PLACEHOLDER_SECRET.");
    static_assert(
      __builtin_strcmp(MQTT_USERNAME, "YOUR_MQTT_USERNAME") != 0,
      "MQTT_USERNAME still contains the placeholder. "
      "Set the deployment MQTT username before enabling MQTT.");
    static_assert(
      __builtin_strcmp(MQTT_PASSWORD, "YOUR_MQTT_PASSWORD") != 0,
      "MQTT_PASSWORD still contains the placeholder. "
      "Set the deployment MQTT password before enabling MQTT.");
  #endif
#endif

// --- Power Management Settings ---
#define BATTERY_CHECK_INTERVAL 60000

#endif
