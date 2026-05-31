// firmware/src/cot_generation.h
#ifndef COT_GENERATION_H
#define COT_GENERATION_H

#include <Arduino.h>

void setCotMissionName(const String& missionName);
String getCotMissionName();
String generateLocationCoT(const String& deviceId, float latitude, float longitude, float altitude);
// Add other CoT generation functions as needed

#endif
