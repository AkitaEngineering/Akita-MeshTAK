// firmware/src/cot_generation.cpp
#include "cot_generation.h"
#include <TinyGPS++.h>

static String escapeXmlAttribute(const String& input) {
  String out = "";
  out.reserve(input.length() + 8);
  for (size_t i = 0; i < input.length(); i++) {
    char c = input.charAt(i);
    switch (c) {
      case '&': out += "&amp;"; break;
      case '<': out += "&lt;"; break;
      case '>': out += "&gt;"; break;
      case '"': out += "&quot;"; break;
      case '\'': out += "&apos;"; break;
      default: out += c; break;
    }
  }
  return out;
}

String generateLocationCoT(const String& deviceId, float latitude, float longitude, float altitude) {
  static uint32_t uidCounter = 0;
  String safeDeviceId = escapeXmlAttribute(deviceId);
  String uniqueId = safeDeviceId + "_" + String(millis()) + "_" + String(uidCounter++);

  String cot = "<event version='2.0' type='a-f-G-U-U'>";
  cot += "<uid generator='" + safeDeviceId + "' uniqueid='" + uniqueId + "'/>";
  cot += "<point lat='" + String(latitude, 7) + "' lon='" + String(longitude, 7) + "' hae='" + String(altitude, 2) + "' ce='10' le='10'/>";
  cot += "<detail>";
  cot += "<contact callsign='" + safeDeviceId + "'/>";
  cot += "<precisionlocation geopointsrc='GPS'/>";
  cot += "</detail>";
  cot += "</event>";
  return cot;
}
