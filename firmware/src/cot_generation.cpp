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

  // Use a fixed buffer with snprintf to avoid heap fragmentation from repeated
  // String += concatenation on the ESP32's constrained heap.
  char buf[512];
  int n = snprintf(buf, sizeof(buf),
      "<event version='2.0' type='a-f-G-U-U'>"
      "<uid generator='%s' uniqueid='%s'/>"
      "<point lat='%.7f' lon='%.7f' hae='%.2f' ce='10' le='10'/>"
      "<detail>"
      "<contact callsign='%s'/>"
      "<precisionlocation geopointsrc='GPS'/>"
      "</detail>"
      "</event>",
      safeDeviceId.c_str(),
      uniqueId.c_str(),
      (double)latitude,
      (double)longitude,
      (double)altitude,
      safeDeviceId.c_str());

  if (n < 0 || n >= (int)sizeof(buf)) {
    // Truncated — return whatever fits.
    buf[sizeof(buf) - 1] = '\0';
  }
  return String(buf);
}
