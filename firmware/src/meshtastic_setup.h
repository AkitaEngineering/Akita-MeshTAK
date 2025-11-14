// File: firmware/src/meshtastic_setup.h
// Description: Declares Meshtastic setup functions and the global Meshtastic object.
#ifndef MESHTASTIC_SETUP_H
#define MESHTASTIC_SETUP_H

#include <Meshtastic.h>

extern MeshtasticClass Meshtastic; // <-- ADDED THIS LINE

bool setupMeshtastic();
void loopMeshtastic();
String getNodeId(const uint8_t *from);

#endif
