package com.akitaengineering.meshtak;

import android.content.SharedPreferences;

import java.util.Locale;

/**
 * Operator-facing identity used in firmware CoT and plugin-generated GeoChat.
 */
public final class OperatorIdentity {

    public static final String PREF_CALLSIGN = "operator_callsign";
    public static final String PREF_TEAM = "operator_team";
    public static final String PREF_ROLE = "operator_role";
    public static final String PREF_STALE_SECONDS = "cot_stale_seconds";
    public static final String PREF_GEOCHAT_ROOM = "geochat_chatroom";
    public static final String PREF_GEOCHAT_DIRECT = "geochat_direct_callsign";
    public static final String PREF_OPENTAKSERVER_MISSION_NAME = "opentakserver_mission_name";

    public static final String DEFAULT_TEAM = "Cyan";
    public static final String DEFAULT_ROLE = "Team Member";
    public static final String DEFAULT_CHATROOM = "All Chat Rooms";
    public static final int DEFAULT_STALE_SECONDS = 120;
    public static final int MIN_STALE_SECONDS = 30;
    public static final int MAX_STALE_SECONDS = 3600;
    public static final int MAX_CALLSIGN_LENGTH = 32;
    public static final int MAX_TEAM_LENGTH = 24;
    public static final int MAX_ROLE_LENGTH = 24;
    public static final int MAX_MISSION_LENGTH = 64;
    public static final int MAX_CHATROOM_LENGTH = 64;

    private OperatorIdentity() {
    }

    public static String getCallsign(SharedPreferences preferences) {
        String configured = sanitizeToken(preferences.getString(PREF_CALLSIGN, ""), MAX_CALLSIGN_LENGTH);
        if (!configured.isEmpty()) {
            return configured;
        }
        String deviceName = sanitizeToken(preferences.getString("ble_device_name", "AkitaNode01"), MAX_CALLSIGN_LENGTH);
        return deviceName.isEmpty() ? "AkitaNode01" : deviceName;
    }

    public static String getTeam(SharedPreferences preferences) {
        String team = sanitizeToken(preferences.getString(PREF_TEAM, DEFAULT_TEAM), MAX_TEAM_LENGTH);
        return team.isEmpty() ? DEFAULT_TEAM : team;
    }

    public static String getRole(SharedPreferences preferences) {
        String role = sanitizeToken(preferences.getString(PREF_ROLE, DEFAULT_ROLE), MAX_ROLE_LENGTH);
        return role.isEmpty() ? DEFAULT_ROLE : role;
    }

    public static int getStaleSeconds(SharedPreferences preferences) {
        int staleSeconds;
        try {
            staleSeconds = Integer.parseInt(preferences.getString(PREF_STALE_SECONDS, String.valueOf(DEFAULT_STALE_SECONDS)).trim());
        } catch (NumberFormatException exception) {
            staleSeconds = DEFAULT_STALE_SECONDS;
        }
        if (staleSeconds < MIN_STALE_SECONDS) {
            return MIN_STALE_SECONDS;
        }
        if (staleSeconds > MAX_STALE_SECONDS) {
            return MAX_STALE_SECONDS;
        }
        return staleSeconds;
    }

    public static long getStaleThresholdMillis(SharedPreferences preferences) {
        return getStaleSeconds(preferences) * 1000L;
    }

    public static String getChatRoom(SharedPreferences preferences) {
        String room = sanitizeToken(preferences.getString(PREF_GEOCHAT_ROOM, DEFAULT_CHATROOM), MAX_CHATROOM_LENGTH);
        return room.isEmpty() ? DEFAULT_CHATROOM : room;
    }

    public static String getDirectCallsign(SharedPreferences preferences) {
        return sanitizeToken(preferences.getString(PREF_GEOCHAT_DIRECT, ""), MAX_CALLSIGN_LENGTH);
    }

    public static String getMissionName(SharedPreferences preferences) {
        return sanitizeMissionName(preferences.getString(PREF_OPENTAKSERVER_MISSION_NAME, ""));
    }

    public static String encodeIdentityCommand(SharedPreferences preferences) {
        return Config.CMD_COT_IDENTITY_PREFIX
                + getCallsign(preferences) + "|"
                + getTeam(preferences) + "|"
                + getRole(preferences);
    }

    public static String encodeStaleCommand(SharedPreferences preferences) {
        return Config.CMD_COT_STALE_PREFIX + getStaleSeconds(preferences);
    }

    public static String sanitizeToken(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder();
        for (int index = 0; index < value.length() && sanitized.length() < maxLength; index++) {
            char c = value.charAt(index);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ' ' || c == '.') {
                sanitized.append(c);
            }
        }
        return sanitized.toString().trim();
    }

    public static String sanitizeMissionName(String missionName) {
        return sanitizeToken(missionName, MAX_MISSION_LENGTH);
    }

    public static boolean isValidStaleSeconds(String value) {
        try {
            int staleSeconds = Integer.parseInt(value.trim());
            return staleSeconds >= MIN_STALE_SECONDS && staleSeconds <= MAX_STALE_SECONDS;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static String summarize(SharedPreferences preferences) {
        String direct = getDirectCallsign(preferences);
        String chatTarget = direct.isEmpty() ? getChatRoom(preferences) : ("DM " + direct);
        return String.format(Locale.US, "%s • %s/%s • stale %ds • chat %s",
                getCallsign(preferences),
                getTeam(preferences),
                getRole(preferences),
                getStaleSeconds(preferences),
                chatTarget);
    }

    public static boolean hasConfiguredCallsign(SharedPreferences preferences) {
        return !sanitizeToken(preferences.getString(PREF_CALLSIGN, ""), MAX_CALLSIGN_LENGTH).isEmpty();
    }
}
