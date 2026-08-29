// firmware/src/cot_generation.h
#ifndef COT_GENERATION_H
#define COT_GENERATION_H

#include <Arduino.h>

void setCotMissionName(const String& missionName);
String getCotMissionName();
void setCotIdentity(const String& callsign, const String& team, const String& role);
String getCotCallsign();
String getCotTeam();
String getCotRole();
void setCotStaleSeconds(int staleSeconds);
int getCotStaleSeconds();
String generateLocationCoT(const String& deviceId, float latitude, float longitude, float altitude);
String generateLocationCoT(const String& deviceId, const String& callsign, float latitude, float longitude,
                           float altitude, int batteryPercent, uint32_t speedMps, uint16_t courseDeg);

#endif
