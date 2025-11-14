// File: firmware/src/power_management.cpp
// Description: Implements power management, battery reading, and command processing.

#include "power_management.h"
#include "config.h"
#include "meshtastic_setup.h" // For Meshtastic object
#include "ble_setup.h"         // For sendDataBLE
#include "serial_bridge.h"   // For sendDataSerial
#include <Arduino.h>

// Standard Heltec V3 battery pin and voltage divider constants
const int VBAT_PIN = 37; 
const float VOLTAGE_DIVIDER_R1 = 100.0; // kOhms
const float VOLTAGE_DIVIDER_R2 = 100.0; // kOhms
const float ADC_READING_CONVERSION = (3.3 / 4095.0); // 12-bit ADC, 3.3V reference

bool setupPowerManagement() {
  Serial.println("Initializing Power Management...");
  pinMode(VBAT_PIN, INPUT);
  return true;
}

float readBatteryVoltage() {
    // Read the ADC value
    int adcValue = analogRead(VBAT_PIN);
    
    // Convert ADC value to voltage
    float vbatVoltage = adcValue * ADC_READING_CONVERSION;
    
    // Account for the voltage divider
    // Vout = Vin * (R2 / (R1 + R2))
    // Vin = Vout * ((R1 + R2) / R2)
    float actualVoltage = vbatVoltage * ((VOLTAGE_DIVIDER_R1 + VOLTAGE_DIVIDER_R2) / VOLTAGE_DIVIDER_R2);
    
    // Heltec V3 specific correction factor (often needed)
    actualVoltage *= 1.1; // Adjust this factor based on real-world testing
    
    return actualVoltage;
}

void loopPowerManagement() {
  // This function can be used for future power-saving logic
}

void processIncomingCommand(const String& cmd) {
    if (cmd.startsWith(CMD_ALERT_SOS)) {
        Serial.println("ALERT RECEIVED: SOS COMMAND TRIGGERED! BROADCASTING.");
        
        // --- Implement Meshtastic high-power emergency broadcast here ---
        String sosMessage = "SOS: Critical Alert from " + String(DEVICE_ID);
        // sendData(payload, len, channel, wantAck, hopLimit, txPower)
        // Using max power (20), default hop limit, on primary channel
        Meshtastic.sendText(sosMessage.c_str(), 0, true, 3, 20);
        
    } else if (cmd.startsWith(CMD_GET_BATT)) {
        float voltage = readBatteryVoltage();
        // Simple LiPo calculation (3.2V min, 4.2V max)
        int percent = (int)((voltage - 3.2) / (4.2 - 3.2) * 100); 
        if (percent > 100) percent = 100;
        if (percent < 0) percent = 0;

        String response = String(STATUS_BATT_PREFIX) + percent + "%";
        
        // Send response back over all enabled interfaces
        #ifdef ENABLE_SERIAL
        sendDataSerial((const uint8_t*)response.c_str(), response.length());
        #endif
        #ifdef ENABLE_BLE
        sendDataBLE((const uint8_t*)response.c_str(), response.length());
        #endif
    }
}
