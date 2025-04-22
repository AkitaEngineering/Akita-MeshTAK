// firmware/src/cot_generation.cpp
#include "cot_generation.h"
#include <TinyGPS++.h>

String generateLocationCoT(const String& deviceId, float latitude, float longitude, float altitude) {
  String cot = "<event version='2.0' type='a-f-G-U-U'>";
  cot += "<uid generator='" + deviceId + "' uniqueid='" + String(millis()) + "'/>";
  cot += "<point lat='" + String(latitude, 7) + "' lon='" + String(longitude, 7) + "' hae='" + String(altitude, 2) + "' ce='10' le='10'/>";
  cot += "<detail>";
  cot += "<contact callsign='" + deviceId + "'/>";
  cot += "<precisionlocation geopointsrc='GPS'/>";
  cot += "</detail>";
  cot += "</event>";
  return cot;
}
