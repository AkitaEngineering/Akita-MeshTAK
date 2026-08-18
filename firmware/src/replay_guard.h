#ifndef REPLAY_GUARD_H
#define REPLAY_GUARD_H

#include <Arduino.h>

void setupReplayGuard();
bool replayGuardHasSeen(const String& keyId, const String& nonceHex);
void replayGuardRemember(const String& keyId, const String& nonceHex, unsigned long long timestamp);

#endif
