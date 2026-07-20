#ifndef PROVISIONING_STORE_H
#define PROVISIONING_STORE_H

#include <Arduino.h>

// Opens a short plaintext provisioning window only when the physical button is
// held during boot. Operational traffic remains encrypted at all times.
void setupProvisioningStore();
bool isProvisioningWindowOpen();
bool isPlaintextProvisioningCommandAllowed(const String& command);
bool loadProvisioningSecret(String& secret);
bool persistProvisioningSecret(const String& secret);

#endif
