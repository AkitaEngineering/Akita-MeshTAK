// firmware/src/cot_generation.cpp
#include "cot_generation.h"
#include "config.h"
#include <TinyGPS++.h>
#include <time.h>
#include <math.h>

static String g_cotMissionName = "";
static String g_cotCallsign = "";
static String g_cotTeam = "Cyan";
static String g_cotRole = "Team Member";
static int g_cotStaleSeconds = 120;

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

static String sanitizeIdentityToken(const String& input, size_t maxLength) {
  String sanitized = "";
  sanitized.reserve(input.length());
  for (size_t i = 0; i < input.length() && sanitized.length() < maxLength; i++) {
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

void setCotIdentity(const String& callsign, const String& team, const String& role) {
  String sanitizedCallsign = sanitizeIdentityToken(callsign, 32);
  String sanitizedTeam = sanitizeIdentityToken(team, 24);
  String sanitizedRole = sanitizeIdentityToken(role, 24);
  if (sanitizedCallsign.length() > 0) {
    g_cotCallsign = sanitizedCallsign;
  }
  if (sanitizedTeam.length() > 0) {
    g_cotTeam = sanitizedTeam;
  }
  if (sanitizedRole.length() > 0) {
    g_cotRole = sanitizedRole;
  }
}

String getCotCallsign() {
  return g_cotCallsign;
}

String getCotTeam() {
  return g_cotTeam;
}

String getCotRole() {
  return g_cotRole;
}

void setCotStaleSeconds(int staleSeconds) {
  if (staleSeconds < 30) {
    g_cotStaleSeconds = 30;
  } else if (staleSeconds > 3600) {
    g_cotStaleSeconds = 3600;
  } else {
    g_cotStaleSeconds = staleSeconds;
  }
}

int getCotStaleSeconds() {
  return g_cotStaleSeconds;
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
  return generateLocationCoT(deviceId, "", latitude, longitude, altitude, -1, 0, 0);
}

String generateLocationCoT(const String& deviceId, const String& callsign, float latitude, float longitude,
                           float altitude, int batteryPercent, uint32_t speedMps, uint16_t courseDeg) {
  if (!isfinite(latitude) || !isfinite(longitude) || !isfinite(altitude)
      || latitude < -90.0f || latitude > 90.0f
      || longitude < -180.0f || longitude > 180.0f) {
    return "";
  }
  static uint32_t uidCounter = 0;
  String safeDeviceId = escapeXmlAttribute(deviceId);
  String eventUid = safeDeviceId + "-" + String(millis()) + "-" + String(uidCounter++);
  String safeVersion = escapeXmlAttribute(FIRMWARE_VERSION);
  String safeMission = escapeXmlAttribute(g_cotMissionName);
  String resolvedCallsign = callsign.length() > 0 ? callsign : (g_cotCallsign.length() > 0 ? g_cotCallsign : deviceId);
  String safeCallsign = escapeXmlAttribute(resolvedCallsign);
  String safeTeam = escapeXmlAttribute(g_cotTeam);
  String safeRole = escapeXmlAttribute(g_cotRole);
  String missionDest = safeMission.length() > 0 ? "<dest mission='" + safeMission + "'/>" : "";
  String statusXml = batteryPercent >= 0
      ? "<status battery='" + String(batteryPercent > 100 ? 100 : batteryPercent) + "'/>"
      : "";
  String trackXml = (speedMps > 0 || courseDeg > 0)
      ? "<track course='" + String(courseDeg) + "' speed='" + String(speedMps) + "'/>"
      : "";
  time_t now = currentCotEpoch();
  String cotTime = formatCotTime(now);
  String cotStale = formatCotTime(now + g_cotStaleSeconds);

  // Use a fixed buffer with snprintf to avoid heap fragmentation from repeated
  // String += concatenation on the ESP32's constrained heap.
  char buf[960];
  int n = snprintf(buf, sizeof(buf),
      "<event version='2.0' uid='%s' type='a-f-G-U-U' how='m-g' time='%s' start='%s' stale='%s'>"
      "%s"
      "<point lat='%.7f' lon='%.7f' hae='%.2f' ce='10' le='10'/>"
      "<detail>"
      "<contact callsign='%s'/>"
      "<takv device='Heltec V3' platform='Akita MeshTAK' os='ESP32' version='%s'/>"
      "<__group name='%s' role='%s'/>"
      "%s%s"
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
      safeCallsign.c_str(),
      safeVersion.c_str(),
      safeTeam.c_str(),
      safeRole.c_str(),
      statusXml.c_str(),
      trackXml.c_str());

  if (n < 0 || n >= (int)sizeof(buf)) {
    memset(buf, 0, sizeof(buf));
    return "";
  }
  return String(buf);
}
