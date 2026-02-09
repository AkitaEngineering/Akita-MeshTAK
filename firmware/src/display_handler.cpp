// firmware/src/display_handler.cpp
#include "config.h"
#if defined(ENABLE_DISPLAY) && ENABLE_DISPLAY
#include "display_handler.h"
#include "meshtastic_setup.h"

bool setupDisplay() {
  Serial.println("Initializing Display...");
  Heltec.display->init();
  Heltec.display->flipScreenVertically();
  Heltec.display->setFont(ArialMT_Plain_10);
  Heltec.display->clear();
  Heltec.display->display();
  return true;
}

void loopDisplay() {
  //  Update the display
  static unsigned long lastDisplayUpdate = 0;
  if (millis() - lastDisplayUpdate > 5000) {
    String displayInfo = "Node: " + getLocalNodeId();
    displayMessage(displayInfo);
    lastDisplayUpdate = millis();
  }
}

void displayMessage(const String& message) {
  Heltec.display->clear();
  Heltec.display->drawString(0, 0, message);
  Heltec.display->display();
}
#endif
