#ifndef CONFIG_H
#define CONFIG_H

#define FIRMWARE_VERSION "0.1.0"

// --- Device Identification ---
#define DEVICE_ID "AkitaNode01"

// --- LoRa Configuration ---
#define LORA_REGION US915  //  Set this to your region!

// --- Connectivity Options ---
#define ENABLE_BLE true
#define ENABLE_SERIAL false
#define ENABLE_MQTT false

// --- BLE Configuration ---
#ifdef ENABLE_BLE
  #define BLE_SERVICE_UUID        "YOUR_SERVICE_UUID"         // Replace
  #define BLE_COT_CHARACTERISTIC_UUID "YOUR_CHARACTERISTIC_UUID" // Replace
  #define BLE_WRITE_CHARACTERISTIC_UUID "YOUR_WRITE_CHARACTERISTIC_UUID" // Add this
#endif

// --- MQTT Settings (if ENABLE_MQTT is true) ---
#ifdef ENABLE_MQTT
  #define MQTT_SERVER "your_mqtt_server.com"      // Replace
  #define MQTT_PORT 1883
  #define MQTT_USERNAME "your_username"          // Replace
  #define MQTT_PASSWORD "your_password"          // Replace
  #define MQTT_TOPIC_PREFIX "akita/meshtak/"
#endif

// --- Display Enable ---
#define ENABLE_DISPLAY true

// --- Power Management Settings ---
#define BATTERY_CHECK_INTERVAL 60000
// Define pins for battery monitoring if applicable

#endif
