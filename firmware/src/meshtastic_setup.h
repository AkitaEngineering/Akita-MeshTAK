// File: firmware/src/meshtastic_setup.h
// Description: Declares Meshtastic setup functions and the global Meshtastic object.
#ifndef MESHTASTIC_SETUP_H
#define MESHTASTIC_SETUP_H

#include <Meshtastic.h>

bool setupMeshtastic();
void loopMeshtastic();
String getNodeId(uint32_t from);
String getLocalNodeId();
bool sendMailboxPayloadOverMesh(const String& messageId, const String& format, const String& payload);

#endif
