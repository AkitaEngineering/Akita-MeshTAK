// firmware/src/cot_generation.h
#ifndef COT_GENERATION_H
#define COT_GENERATION_H

#include <Arduino.h>

String generateLocationCoT(const String& deviceId, float latitude, float longitude, float altitude);
// Add other CoT generation functions as needed

#endif
