// firmware/src/meshtastic_setup.h
#ifndef MESHTASTIC_SETUP_H
#define MESHTASTIC_SETUP_H

#include <Meshtastic.h>

bool setupMeshtastic();
void loopMeshtastic();
String getNodeId(const uint8_t *from); // Add this

#endif
