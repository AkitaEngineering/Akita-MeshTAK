package com.akitaengineering.meshtak;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Builds OpenTAKServer-compatible CoT XML for location, GeoChat, and test events.
 */
public final class CotEventFactory {

    public static final String LOCATION_TYPE = "a-f-G-U-U";
    public static final String GEOCHAT_TYPE = "b-t-f";
    public static final String CHAT_MAILBOX_PREFIX = "CHAT|";
    public static final String CHAT_DM_MAILBOX_PREFIX = "CHATDM|";

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private CotEventFactory() {
    }

    public static String locationEvent(String uid,
                                       String callsign,
                                       String team,
                                       String role,
                                       String missionName,
                                       double latitude,
                                       double longitude,
                                       double altitude,
                                       long epochMillis,
                                       int staleSeconds) {
        return locationEvent(uid, callsign, team, role, missionName, latitude, longitude, altitude,
                epochMillis, staleSeconds, -1, 0, 0);
    }

    public static String locationEvent(String uid,
                                       String callsign,
                                       String team,
                                       String role,
                                       String missionName,
                                       double latitude,
                                       double longitude,
                                       double altitude,
                                       long epochMillis,
                                       int staleSeconds,
                                       int batteryPercent,
                                       int speedMps,
                                       int courseDeg) {
        String safeUid = escapeXml(firstNonEmpty(uid, callsign, "AkitaNode"));
        String safeCallsign = escapeXml(firstNonEmpty(callsign, "AkitaNode"));
        String safeTeam = escapeXml(firstNonEmpty(team, OperatorIdentity.DEFAULT_TEAM));
        String safeRole = escapeXml(firstNonEmpty(role, OperatorIdentity.DEFAULT_ROLE));
        String time = formatCotTime(epochMillis);
        String stale = formatCotTime(epochMillis + (Math.max(staleSeconds, OperatorIdentity.MIN_STALE_SECONDS) * 1000L));
        String missionDest = missionDest(missionName);
        String status = batteryPercent >= 0
                ? "<status battery='" + Math.min(100, batteryPercent) + "'/>"
                : "";
        String track = (speedMps > 0 || courseDeg > 0)
                ? "<track course='" + Math.max(0, courseDeg) + "' speed='"
                    + String.format(Locale.US, "%.1f", (double) Math.max(0, speedMps)) + "'/>"
                : "";
        return "<event version='2.0' uid='" + safeUid + "' type='" + LOCATION_TYPE + "' how='m-g' time='"
                + time + "' start='" + time + "' stale='" + stale + "'>"
                + missionDest
                + "<point lat='" + formatCoordinate(latitude) + "' lon='" + formatCoordinate(longitude)
                + "' hae='" + String.format(Locale.US, "%.2f", altitude) + "' ce='10' le='10'/>"
                + "<detail>"
                + "<contact callsign='" + safeCallsign + "'/>"
                + "<takv device='ATAK' platform='Akita MeshTAK' os='Android' version='"
                + escapeXml(BuildConfig.VERSION_NAME) + "'/>"
                + "<__group name='" + safeTeam + "' role='" + safeRole + "'/>"
                + status
                + track
                + "<precisionlocation geopointsrc='GPS' altsrc='GPS'/>"
                + "</detail></event>";
    }

    public static String testLocationEvent(String callsign,
                                           String team,
                                           String role,
                                           String missionName,
                                           long epochMillis,
                                           int staleSeconds) {
        String uid = firstNonEmpty(callsign, "AkitaNode") + "-TEST-" + Long.toHexString(epochMillis);
        return locationEvent(uid, callsign, team, role, missionName, 0.0d, 0.0d, 0.0d, epochMillis, staleSeconds);
    }

    public static String geoChatEvent(String senderCallsign,
                                      String destination,
                                      String message,
                                      boolean directMessage,
                                      String missionName,
                                      double latitude,
                                      double longitude,
                                      long epochMillis,
                                      int staleSeconds) {
        String sender = firstNonEmpty(senderCallsign, "AkitaNode");
        String dest = firstNonEmpty(destination, OperatorIdentity.DEFAULT_CHATROOM);
        String messageId = UUID.nameUUIDFromBytes((sender + dest + epochMillis + message).getBytes()).toString();
        String uid = "GeoChat." + sender + "." + dest + "." + messageId;
        String time = formatCotTime(epochMillis);
        String stale = formatCotTime(epochMillis + (Math.max(staleSeconds, OperatorIdentity.MIN_STALE_SECONDS) * 1000L));
        String chatroom = directMessage ? dest : dest;
        String destElement = directMessage
                ? "<dest callsign='" + escapeXml(dest) + "'/>"
                : "<dest callsign='" + escapeXml(dest) + "'/>";
        String missionDest = missionDest(missionName);
        return "<event version='2.0' uid='" + escapeXml(uid) + "' type='" + GEOCHAT_TYPE
                + "' how='h-g-i-g-o' time='" + time + "' start='" + time + "' stale='" + stale + "'>"
                + missionDest
                + "<point lat='" + formatCoordinate(latitude) + "' lon='" + formatCoordinate(longitude)
                + "' hae='0.00' ce='9999999' le='9999999'/>"
                + "<detail>"
                + "<__chat parent='RootContactGroup' groupOwner='false' chatroom='" + escapeXml(chatroom)
                + "' id='" + escapeXml(chatroom) + "' senderCallsign='" + escapeXml(sender) + "'>"
                + "<chatgrp uid0='" + escapeXml(sender) + "' uid1='" + escapeXml(dest) + "' id='"
                + escapeXml(chatroom) + "'/>"
                + "</__chat>"
                + "<link uid='" + escapeXml(sender) + "' type='a-f-G-U-C' relation='p-p'/>"
                + "<remarks source='Akita.MeshTAK." + escapeXml(sender) + "' to='" + escapeXml(dest)
                + "' time='" + time + "'>" + escapeXml(message) + "</remarks>"
                + "<marti>" + destElement + "</marti>"
                + "</detail></event>";
    }

    public static String toMailboxChatPayload(String destination, String message, boolean directMessage) {
        String prefix = directMessage ? CHAT_DM_MAILBOX_PREFIX : CHAT_MAILBOX_PREFIX;
        return prefix + firstNonEmpty(destination, OperatorIdentity.DEFAULT_CHATROOM) + "|"
                + (message == null ? "" : message.trim());
    }

    public static ChatPayload parseMailboxChat(String payload) {
        if (payload == null) {
            return null;
        }
        String trimmed = payload.trim();
        if (trimmed.startsWith(CHAT_DM_MAILBOX_PREFIX)) {
            return splitChat(trimmed.substring(CHAT_DM_MAILBOX_PREFIX.length()), true);
        }
        if (trimmed.startsWith(CHAT_MAILBOX_PREFIX)) {
            return splitChat(trimmed.substring(CHAT_MAILBOX_PREFIX.length()), false);
        }
        return null;
    }

    public static long parseStaleEpochMillis(String xml) {
        String stale = readAttribute(xml, "stale");
        if (stale == null || stale.isEmpty()) {
            return 0L;
        }
        SimpleDateFormat format = utcFormat();
        try {
            Date parsed = format.parse(stale);
            return parsed == null ? 0L : parsed.getTime();
        } catch (ParseException exception) {
            return 0L;
        }
    }

    public static String readAttribute(String xml, String name) {
        if (xml == null || name == null) {
            return null;
        }
        String doubleQuoted = name + "=\"";
        int start = xml.indexOf(doubleQuoted);
        if (start >= 0) {
            start += doubleQuoted.length();
            int end = xml.indexOf('"', start);
            return end > start ? xml.substring(start, end) : null;
        }
        String singleQuoted = name + "='";
        start = xml.indexOf(singleQuoted);
        if (start < 0) {
            return null;
        }
        start += singleQuoted.length();
        int end = xml.indexOf('\'', start);
        return end > start ? xml.substring(start, end) : null;
    }

    public static String formatCotTime(long epochMillis) {
        return utcFormat().format(new Date(epochMillis));
    }

    public static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            switch (c) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&apos;");
                    break;
                default:
                    if (c >= 32) {
                        escaped.append(c);
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    private static ChatPayload splitChat(String body, boolean directMessage) {
        int separator = body.indexOf('|');
        if (separator <= 0 || separator >= body.length() - 1) {
            return null;
        }
        String destination = OperatorIdentity.sanitizeToken(body.substring(0, separator), OperatorIdentity.MAX_CHATROOM_LENGTH);
        String message = body.substring(separator + 1).trim();
        if (destination.isEmpty() || message.isEmpty()) {
            return null;
        }
        return new ChatPayload(destination, message, directMessage);
    }

    private static String missionDest(String missionName) {
        String sanitized = OperatorIdentity.sanitizeMissionName(missionName);
        if (sanitized.isEmpty()) {
            return "";
        }
        return "<dest mission='" + escapeXml(sanitized) + "'/>";
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String formatCoordinate(double value) {
        return String.format(Locale.US, "%.7f", value);
    }

    private static SimpleDateFormat utcFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(UTC);
        return format;
    }

    public static final class ChatPayload {
        public final String destination;
        public final String message;
        public final boolean directMessage;

        private ChatPayload(String destination, String message, boolean directMessage) {
            this.destination = destination;
            this.message = message;
            this.directMessage = directMessage;
        }
    }
}
