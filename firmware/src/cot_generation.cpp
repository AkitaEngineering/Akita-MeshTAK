// firmware/src/cot_generation.cpp
#include "cot_generation.h"
#include "config.h"
#include <TinyGPS++.h>
#include <time.h>

static String g_cotMissionName = "";

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

static String sanitizeCotMissionName(const String& input) {
  String sanitized = "";
  sanitized.reserve(input.length());
  for (size_t i = 0; i < input.length() && sanitized.length() < 64; i++) {
    char c = input.charAt(i);
    if (isalnum(c) || c == '-' || c == '_' || c == ' ' || c == '.') {
      sanitized += c;
    }
  }
  sanitized.trim();
  return sanitized;
}

void setCotMissionName(const String& missionName) {
  g_cotMissionName = sanitizeCotMissionName(missionName);
}

String getCotMissionName() {
  return g_cotMissionName;
}

static String formatCotTime(time_t timestamp) {
  struct tm tmUtc;
  gmtime_r(&timestamp, &tmUtc);

  char buf[25];
  snprintf(buf, sizeof(buf),
      "%04d-%02d-%02dT%02d:%02d:%02dZ",
      tmUtc.tm_year + 1900,
      tmUtc.tm_mon + 1,
      tmUtc.tm_mday,
      tmUtc.tm_hour,
      tmUtc.tm_min,
      tmUtc.tm_sec);
  return String(buf);
}

static time_t currentCotEpoch() {
  time_t now = time(nullptr);
  if (now >= 1609459200) {
    return now;
  }

  // If the ESP32 has not learned wall-clock time yet, still emit valid
  // ISO-8601 CoT timestamps so TAK servers can parse and age the event.
  return 1609459200 + (millis() / 1000);
}

String generateLocationCoT(const String& deviceId, float latitude, float longitude, float altitude) {
  static uint32_t uidCounter = 0;
  String safeDeviceId = escapeXmlAttribute(deviceId);
  String eventUid = safeDeviceId + "-" + String(millis()) + "-" + String(uidCounter++);
  String safeVersion = escapeXmlAttribute(FIRMWARE_VERSION);
  String safeMission = escapeXmlAttribute(g_cotMissionName);
  String missionDest = safeMission.length() > 0 ? "<dest mission='" + safeMission + "'/>" : "";
  time_t now = currentCotEpoch();
  String cotTime = formatCotTime(now);
  String cotStale = formatCotTime(now + 120);

  // Use a fixed buffer with snprintf to avoid heap fragmentation from repeated
  // String += concatenation on the ESP32's constrained heap.
  char buf[768];
  int n = snprintf(buf, sizeof(buf),
      "<event version='2.0' uid='%s' type='a-f-G-U-U' how='m-g' time='%s' start='%s' stale='%s'>"
      "%s"
      "<point lat='%.7f' lon='%.7f' hae='%.2f' ce='10' le='10'/>"
      "<detail>"
      "<contact callsign='%s'/>"
      "<takv device='Heltec V3' platform='Akita MeshTAK' os='ESP32' version='%s'/>"
      "<__group name='Cyan' role='Team Member'/>"
      "<precisionlocation geopointsrc='GPS' altsrc='GPS'/>"
      "</detail>"
      "</event>",
      eventUid.c_str(),
      cotTime.c_str(),
      cotTime.c_str(),
      cotStale.c_str(),
      missionDest.c_str(),
      (double)latitude,
      (double)longitude,
      (double)altitude,
      safeDeviceId.c_str(),
      safeVersion.c_str());

  if (n < 0 || n >= (int)sizeof(buf)) {
    // Truncated — return whatever fits.
    buf[sizeof(buf) - 1] = '\0';
  }
  return String(buf);
}
