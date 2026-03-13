// firmware/src/display_handler.cpp
#include "config.h"
#if defined(ENABLE_DISPLAY) && ENABLE_DISPLAY
#include "display_handler.h"
#include "meshtastic_setup.h"

bool setupDisplay() {
  Serial.println("Initializing Display...");
  // Initialize Heltec framework (display=true, LoRa=false, serial=false)
  // LoRa is managed by Meshtastic; Serial is already initialized in main.
  Heltec.begin(true, false, false);
  if (Heltec.display == nullptr) {
    Serial.println("ERROR: Display initialization failed - display is null");
    return false;
  }
  Heltec.display->flipScreenVertically();
  Heltec.display->setFont(ArialMT_Plain_10);
  Heltec.display->clear();
  Heltec.display->display();
  return true;
}

void loopDisplay() {
  if (Heltec.display == nullptr) return;
  static unsigned long lastDisplayUpdate = 0;
  if (millis() - lastDisplayUpdate > 5000) {
    String displayInfo = "Node: " + getLocalNodeId();
    displayMessage(displayInfo);
    lastDisplayUpdate = millis();
  }
}

void displayMessage(const String& message) {
  if (Heltec.display == nullptr) return;
  Heltec.display->clear();
  Heltec.display->drawString(0, 0, message);
  Heltec.display->display();
}
#endif
