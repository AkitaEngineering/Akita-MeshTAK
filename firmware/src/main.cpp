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

void setupConfig() {
    // Placeholder function for config setup (data is read from config.h)
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
  
  // Initialize security (keys should be provisioned securely - NOT hardcoded!)
  // TODO: Replace with secure key provisioning in production
  uint8_t default_aes_key[AES_KEY_SIZE] = {0}; // MUST be replaced with secure keys!
  uint8_t default_hmac_key[HMAC_KEY_SIZE] = {0}; // MUST be replaced with secure keys!
  
  // For now, generate random keys (in production, use secure provisioning)
  for (int i = 0; i < AES_KEY_SIZE; i++) {
    default_aes_key[i] = (uint8_t)esp_random();
  }
  for (int i = 0; i < HMAC_KEY_SIZE; i++) {
    default_hmac_key[i] = (uint8_t)esp_random();
  }
  
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

#ifdef ENABLE_BLE
  if (!setupBLE()) {
    Serial.println("Warning: BLE initialization failed.");
  }
#endif

#ifdef ENABLE_SERIAL
  setupSerialBridge();
#endif

#ifdef ENABLE_MQTT
  if (!setupMQTT()) {
    Serial.println("Warning: MQTT initialization failed.");
  }
#endif

#ifdef ENABLE_DISPLAY
  setupDisplay();
  displayMessage("Akita MeshTAK v" FIRMWARE_VERSION);
#endif

  setupPowerManagement();

  Serial.println("Setup complete.");
}

void loop() {
  loopMeshtastic();

#ifdef ENABLE_BLE
  loopBLE();
#endif

#ifdef ENABLE_SERIAL
  loopSerialBridge();
#endif

#ifdef ENABLE_MQTT
  loopMQTT();
#endif

#ifdef ENABLE_DISPLAY
  loopDisplay();
#endif

  loopPowerManagement();

  delay(10);
}
