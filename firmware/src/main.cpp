// File: firmware/src/main.cpp
// Description: Main application loop and setup for Akita MeshTAK firmware.
#include <Arduino.h>
#include "config.h"
#include "meshtastic_setup.h"
#include "ble_setup.h"
#include "serial_bridge.h"
#include "mqtt_client.h"
#include "power_management.h"
#include "display_handler.h"
#include "audit_log.h"        // For audit logging
#include "security.h"         // For security initialization
#include <mbedtls/sha256.h>
#include <mbedtls/pkcs5.h>
#include <mbedtls/md.h>
#include <string.h>

void setupConfig() {
    // Placeholder function for config setup (data is read from config.h)
}

static void deriveProvisionedKey(const char* purpose, uint8_t* outKey, size_t outLen) {
  // Build salt from deviceId + purpose so each derived key is unique.
  String salt = String(DEVICE_ID) + ":" + String(purpose);

  // PBKDF2-HMAC-SHA256, 100 000 iterations – matches security.cpp.
  mbedtls_pkcs5_hmac_ext(
      MBEDTLS_MD_SHA256,
      reinterpret_cast<const unsigned char*>(PROVISIONING_SECRET),
      strlen(PROVISIONING_SECRET),
      reinterpret_cast<const unsigned char*>(salt.c_str()),
      salt.length(),
      100000,
      outLen,
      outKey);

  for (unsigned int i = 0; i < salt.length(); i++) {
    salt.setCharAt(i, '\0');
  }
}

void setup() {
  Serial.begin(115200);
  Serial.println("Akita MeshTAK Firmware v" FIRMWARE_VERSION " Starting...");
  Serial.printf("Device ID: %s\n", DEVICE_ID);

  setupConfig();
  
  // Initialize audit logging FIRST for accountability
  if (!initAuditLog()) {
    Serial.println("WARNING: Audit log initialization failed.");
  }
  
  // Initialize security from deterministic provisioning material.
  uint8_t default_aes_key[AES_KEY_SIZE] = {0};
  uint8_t default_hmac_key[HMAC_KEY_SIZE] = {0};
  deriveProvisionedKey("aes", default_aes_key, AES_KEY_SIZE);
  deriveProvisionedKey("hmac", default_hmac_key, HMAC_KEY_SIZE);
  
  if (!initSecurity(default_aes_key, default_hmac_key, SECURITY_MODE_AES256_HMAC)) {
    Serial.println("WARNING: Security initialization failed.");
    logAuditEvent(AUDIT_EVENT_ERROR, 2, DEVICE_ID, "Security init failed", false);
  } else {
    logAuditEvent(AUDIT_EVENT_CONNECTION, 0, DEVICE_ID, "Security initialized", true);
  }

  if (!setupMeshtastic()) {
    Serial.println("Critical Error: Meshtastic initialization failed. Halting.");
    logAuditEvent(AUDIT_EVENT_ERROR, 3, DEVICE_ID, "Meshtastic init failed - halting", false);
    while (true) {
      delay(1000);
    }
  }

#if defined(ENABLE_BLE) && ENABLE_BLE
  if (!setupBLE()) {
    Serial.println("Warning: BLE initialization failed.");
  }
#endif

#if defined(ENABLE_SERIAL) && ENABLE_SERIAL
  setupSerialBridge();
#endif

#if defined(ENABLE_MQTT) && ENABLE_MQTT
  if (!setupMQTT()) {
    Serial.println("Warning: MQTT initialization failed.");
  }
#endif

#if defined(ENABLE_DISPLAY) && ENABLE_DISPLAY
  setupDisplay();
  displayMessage("Akita MeshTAK v" FIRMWARE_VERSION);
#endif

  setupPowerManagement();

  Serial.println("Setup complete.");
}

void loop() {
  loopMeshtastic();

#if defined(ENABLE_BLE) && ENABLE_BLE
  loopBLE();
#endif

#if defined(ENABLE_SERIAL) && ENABLE_SERIAL
  loopSerialBridge();
#endif

#if defined(ENABLE_MQTT) && ENABLE_MQTT
  loopMQTT();
#endif

#if defined(ENABLE_DISPLAY) && ENABLE_DISPLAY
  loopDisplay();
#endif

  loopPowerManagement();

  delay(10);
}
