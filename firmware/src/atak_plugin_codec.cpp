#include "atak_plugin_codec.h"
#include "config.h"
#include "cot_generation.h"
#include "mailbox_escape.h"
#include "ble_setup.h"
#include "serial_bridge.h"
#include "audit_log.h"
#include "meshtastic/atak.pb.h"
#include "meshtastic/mesh.pb.h"
#include "meshtastic/portnums.pb.h"
#include "pb_encode.h"
#include "pb_decode.h"
#include <Meshtastic.h>
#include <math.h>
#include <string.h>

extern bool _mt_send_toRadio(meshtastic_ToRadio toRadio);

static bool g_atakPluginEnabled = false;

static void copyBounded(char* dest, size_t destSize, const String& value) {
  if (destSize == 0) {
    return;
  }
  size_t n = value.length();
  if (n >= destSize) {
    n = destSize - 1;
  }
  memcpy(dest, value.c_str(), n);
  dest[n] = '\0';
}

static meshtastic_Team teamFromName(const String& team) {
  String normalized = team;
  normalized.toLowerCase();
  if (normalized == "white") return meshtastic_Team_White;
  if (normalized == "yellow") return meshtastic_Team_Yellow;
  if (normalized == "orange") return meshtastic_Team_Orange;
  if (normalized == "magenta") return meshtastic_Team_Magenta;
  if (normalized == "red") return meshtastic_Team_Red;
  if (normalized == "maroon") return meshtastic_Team_Maroon;
  if (normalized == "purple") return meshtastic_Team_Purple;
  if (normalized == "dark blue") return meshtastic_Team_Dark_Blue;
  if (normalized == "blue") return meshtastic_Team_Blue;
  if (normalized == "teal") return meshtastic_Team_Teal;
  if (normalized == "green") return meshtastic_Team_Green;
  if (normalized == "dark green") return meshtastic_Team_Dark_Green;
  if (normalized == "brown") return meshtastic_Team_Brown;
  return meshtastic_Team_Cyan;
}

static meshtastic_MemberRole roleFromName(const String& role) {
  String normalized = role;
  normalized.toLowerCase();
  if (normalized == "team lead") return meshtastic_MemberRole_TeamLead;
  if (normalized == "hq") return meshtastic_MemberRole_HQ;
  if (normalized == "sniper") return meshtastic_MemberRole_Sniper;
  if (normalized == "medic") return meshtastic_MemberRole_Medic;
  if (normalized == "forward observer") return meshtastic_MemberRole_ForwardObserver;
  if (normalized == "rto") return meshtastic_MemberRole_RTO;
  if (normalized == "k9") return meshtastic_MemberRole_K9;
  return meshtastic_MemberRole_TeamMember;
}

static String teamToName(meshtastic_Team team) {
  switch (team) {
    case meshtastic_Team_White: return "White";
    case meshtastic_Team_Yellow: return "Yellow";
    case meshtastic_Team_Orange: return "Orange";
    case meshtastic_Team_Magenta: return "Magenta";
    case meshtastic_Team_Red: return "Red";
    case meshtastic_Team_Maroon: return "Maroon";
    case meshtastic_Team_Purple: return "Purple";
    case meshtastic_Team_Dark_Blue: return "Dark Blue";
    case meshtastic_Team_Blue: return "Blue";
    case meshtastic_Team_Teal: return "Teal";
    case meshtastic_Team_Green: return "Green";
    case meshtastic_Team_Dark_Green: return "Dark Green";
    case meshtastic_Team_Brown: return "Brown";
    default: return "Cyan";
  }
}

static String roleToName(meshtastic_MemberRole role) {
  switch (role) {
    case meshtastic_MemberRole_TeamLead: return "Team Lead";
    case meshtastic_MemberRole_HQ: return "HQ";
    case meshtastic_MemberRole_Sniper: return "Sniper";
    case meshtastic_MemberRole_Medic: return "Medic";
    case meshtastic_MemberRole_ForwardObserver: return "Forward Observer";
    case meshtastic_MemberRole_RTO: return "RTO";
    case meshtastic_MemberRole_K9: return "K9";
    default: return "Team Member";
  }
}

static void applyIdentity(const meshtastic_TAKPacket& packet) {
  String callsign = packet.has_contact ? String(packet.contact.callsign) : "";
  String team = packet.has_group ? teamToName(packet.group.team) : getCotTeam();
  String role = packet.has_group ? roleToName(packet.group.role) : getCotRole();
  if (callsign.length() > 0 || packet.has_group) {
    setCotIdentity(callsign, team, role);
  }
}

static void forwardToAtak(const String& payload) {
#if defined(ENABLE_SERIAL) && ENABLE_SERIAL
  sendDataSerial((const uint8_t*)payload.c_str(), payload.length());
#endif
#if defined(ENABLE_BLE) && ENABLE_BLE
  sendDataBLE((const uint8_t*)payload.c_str(), payload.length());
#endif
}

static bool sendTakPacket(const meshtastic_TAKPacket& packet) {
  uint8_t encoded[237];
  pb_ostream_t stream = pb_ostream_from_buffer(encoded, sizeof(encoded));
  if (!pb_encode(&stream, meshtastic_TAKPacket_fields, &packet) || stream.bytes_written == 0
      || stream.bytes_written > sizeof(encoded)) {
    return false;
  }

  meshtastic_MeshPacket meshPacket = meshtastic_MeshPacket_init_default;
  meshPacket.which_payload_variant = meshtastic_MeshPacket_decoded_tag;
  meshPacket.id = random(0x7FFFFFFF);
  meshPacket.decoded.portnum = meshtastic_PortNum_ATAK_PLUGIN;
  meshPacket.to = BROADCAST_ADDR;
  meshPacket.channel = 0;
  meshPacket.want_ack = true;
  meshPacket.decoded.payload.size = stream.bytes_written;
  memcpy(meshPacket.decoded.payload.bytes, encoded, stream.bytes_written);

  meshtastic_ToRadio toRadio = meshtastic_ToRadio_init_default;
  toRadio.which_payload_variant = meshtastic_ToRadio_packet_tag;
  toRadio.packet = meshPacket;
  return _mt_send_toRadio(toRadio);
}

void setAtakPluginEnabled(bool enabled) {
  g_atakPluginEnabled = enabled;
}

bool isAtakPluginEnabled() {
  return g_atakPluginEnabled;
}

bool sendAtakPluginPli(const String& callsign, const String& deviceCallsign,
                       float latitude, float longitude, float altitude,
                       uint32_t speed, uint16_t course, uint8_t battery) {
  if (!g_atakPluginEnabled) {
    return false;
  }
  meshtastic_TAKPacket packet = meshtastic_TAKPacket_init_zero;
  packet.has_contact = true;
  copyBounded(packet.contact.callsign, sizeof(packet.contact.callsign), callsign);
  copyBounded(packet.contact.device_callsign, sizeof(packet.contact.device_callsign),
              deviceCallsign.length() > 0 ? deviceCallsign : callsign);
  packet.has_group = true;
  packet.group.team = teamFromName(getCotTeam());
  packet.group.role = roleFromName(getCotRole());
  if (battery > 0) {
    packet.has_status = true;
    packet.status.battery = battery;
  }
  packet.which_payload_variant = meshtastic_TAKPacket_pli_tag;
  packet.payload_variant.pli.latitude_i = (int32_t)lroundf(latitude * 1e7f);
  packet.payload_variant.pli.longitude_i = (int32_t)lroundf(longitude * 1e7f);
  packet.payload_variant.pli.altitude = (int32_t)lroundf(altitude);
  packet.payload_variant.pli.speed = speed;
  packet.payload_variant.pli.course = course;
  return sendTakPacket(packet);
}

bool sendAtakPluginChat(const String& callsign, const String& message,
                        const String& to, const String& toCallsign) {
  if (!g_atakPluginEnabled || message.length() == 0) {
    return false;
  }
  meshtastic_TAKPacket packet = meshtastic_TAKPacket_init_zero;
  packet.has_contact = true;
  copyBounded(packet.contact.callsign, sizeof(packet.contact.callsign), callsign);
  copyBounded(packet.contact.device_callsign, sizeof(packet.contact.device_callsign), callsign);
  packet.has_group = true;
  packet.group.team = teamFromName(getCotTeam());
  packet.group.role = roleFromName(getCotRole());
  packet.which_payload_variant = meshtastic_TAKPacket_chat_tag;
  copyBounded(packet.payload_variant.chat.message, sizeof(packet.payload_variant.chat.message), message);
  if (to.length() > 0) {
    packet.payload_variant.chat.has_to = true;
    copyBounded(packet.payload_variant.chat.to, sizeof(packet.payload_variant.chat.to), to);
  }
  if (toCallsign.length() > 0) {
    packet.payload_variant.chat.has_to_callsign = true;
    copyBounded(packet.payload_variant.chat.to_callsign, sizeof(packet.payload_variant.chat.to_callsign), toCallsign);
  }
  return sendTakPacket(packet);
}

void handleAtakPluginPayload(uint32_t from, uint32_t to, uint8_t channel,
                             const uint8_t* payload, size_t length) {
  (void)to;
  (void)channel;
  if (!g_atakPluginEnabled || payload == nullptr || length == 0) {
    return;
  }
  meshtastic_TAKPacket packet = meshtastic_TAKPacket_init_zero;
  pb_istream_t stream = pb_istream_from_buffer(payload, length);
  if (!pb_decode(&stream, meshtastic_TAKPacket_fields, &packet)) {
    logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 1, "MESH", "ATAK_PLUGIN decode failed", false);
    return;
  }
  applyIdentity(packet);
  char nodeId[12];
  snprintf(nodeId, sizeof(nodeId), "%08lX", (unsigned long)from);
  if (packet.which_payload_variant == meshtastic_TAKPacket_pli_tag) {
    float lat = packet.payload_variant.pli.latitude_i / 1e7f;
    float lon = packet.payload_variant.pli.longitude_i / 1e7f;
    float alt = (float)packet.payload_variant.pli.altitude;
    uint8_t battery = packet.has_status ? packet.status.battery : 0;
    String cot = generateLocationCoT(String(nodeId),
        packet.has_contact ? String(packet.contact.callsign) : String(nodeId),
        lat, lon, alt, battery, packet.payload_variant.pli.speed,
        packet.payload_variant.pli.course);
    if (cot.length() > 0) {
      forwardToAtak(cot);
      logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "MESH", "ATAK_PLUGIN PLI forwarded", true);
    }
    return;
  }
  if (packet.which_payload_variant == meshtastic_TAKPacket_chat_tag) {
    String message = String(packet.payload_variant.chat.message);
    String dest = packet.payload_variant.chat.has_to_callsign
        ? String(packet.payload_variant.chat.to_callsign)
        : (packet.payload_variant.chat.has_to ? String(packet.payload_variant.chat.to) : String("All Chat Rooms"));
    String compact = packet.payload_variant.chat.has_to || packet.payload_variant.chat.has_to_callsign
        ? String("CHATDM|") + dest + "|" + message
        : String("CHAT|") + dest + "|" + message;
    String inbound = String(STATUS_MAILBOX_RX_PREFIX) + nodeId + ":" + escapeMailboxPayload(compact);
    forwardToAtak(inbound);
    logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "MESH", "ATAK_PLUGIN GeoChat forwarded", true);
  }
}
