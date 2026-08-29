package com.akitaengineering.meshtak;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.UUID;

/**
 * Mesh-safe data-package handoff. Binary packages stay on ATAK/OpenTAKServer;
 * the mesh only carries a hashed reference plus a TAK fileshare CoT.
 */
public final class DataPackageHandoff {

    public static final String MAILBOX_TYPE = "datapackage";
    public static final String COT_TYPE = "b-f-t-file";
    public static final String PREF_NAME = "data_package_name";
    public static final String PREF_SHA256 = "data_package_sha256";
    public static final String PREF_SIZE = "data_package_size";
    public static final String PREF_URL = "data_package_url";

    public static final class Reference {
        public final String name;
        public final String sha256;
        public final long sizeBytes;
        public final String url;
        public final String missionName;

        public Reference(String name, String sha256, long sizeBytes, String url, String missionName) {
            this.name = name == null ? "" : name.trim();
            this.sha256 = normalizeHash(sha256);
            this.sizeBytes = Math.max(0L, sizeBytes);
            this.url = url == null ? "" : url.trim();
            this.missionName = OperatorIdentity.sanitizeMissionName(missionName);
        }

        public boolean isComplete() {
            return !name.isEmpty() && sha256.length() == 64;
        }
    }

    private DataPackageHandoff() {
    }

    public static String mailboxJson(Reference reference) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", MAILBOX_TYPE);
        json.put("name", reference.name);
        json.put("sha256", reference.sha256);
        json.put("sizeBytes", reference.sizeBytes);
        json.put("url", reference.url);
        json.put("mission", reference.missionName);
        json.put("transfer", "atak-opentakserver");
        return json.toString();
    }

    public static Reference parseMailboxJson(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(payload);
            if (!MAILBOX_TYPE.equals(json.optString("type", ""))) {
                return null;
            }
            return new Reference(
                    json.optString("name", ""),
                    json.optString("sha256", ""),
                    json.optLong("sizeBytes", 0L),
                    json.optString("url", ""),
                    json.optString("mission", ""));
        } catch (JSONException exception) {
            return null;
        }
    }

    public static String fileShareCoT(Reference reference,
                                      String callsign,
                                      String team,
                                      String role,
                                      double latitude,
                                      double longitude,
                                      long epochMillis,
                                      int staleSeconds) {
        String uid = "FILESHARE-" + (reference.sha256.isEmpty()
                ? UUID.nameUUIDFromBytes(reference.name.getBytes()).toString()
                : reference.sha256.substring(0, 12));
        String time = CotEventFactory.formatCotTime(epochMillis);
        String stale = CotEventFactory.formatCotTime(epochMillis + Math.max(staleSeconds, 30) * 1000L);
        String missionDest = reference.missionName.isEmpty()
                ? ""
                : "<dest mission='" + CotEventFactory.escapeXml(reference.missionName) + "'/>";
        return "<event version='2.0' uid='" + CotEventFactory.escapeXml(uid) + "' type='" + COT_TYPE
                + "' how='h-g-i-g-o' time='" + time + "' start='" + time + "' stale='" + stale + "'>"
                + missionDest
                + "<point lat='" + String.format(Locale.US, "%.7f", latitude)
                + "' lon='" + String.format(Locale.US, "%.7f", longitude)
                + "' hae='0.00' ce='9999999' le='9999999'/>"
                + "<detail>"
                + "<contact callsign='" + CotEventFactory.escapeXml(callsign) + "'/>"
                + "<__group name='" + CotEventFactory.escapeXml(team) + "' role='"
                + CotEventFactory.escapeXml(role) + "'/>"
                + "<fileshare filename='" + CotEventFactory.escapeXml(reference.name)
                + "' senderUrl='" + CotEventFactory.escapeXml(reference.url)
                + "' sizeInBytes='" + reference.sizeBytes
                + "' sha256hash='" + CotEventFactory.escapeXml(reference.sha256) + "'/>"
                + "</detail></event>";
    }

    public static String normalizeHash(String sha256) {
        if (sha256 == null) {
            return "";
        }
        StringBuilder hex = new StringBuilder();
        for (int index = 0; index < sha256.length() && hex.length() < 64; index++) {
            char c = Character.toLowerCase(sha256.charAt(index));
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) {
                hex.append(c);
            }
        }
        return hex.toString();
    }

    public static boolean isValidSha256(String sha256) {
        return normalizeHash(sha256).length() == 64;
    }

    public static String strategySummary() {
        return "Binary data packages stay on ATAK/OpenTAKServer. Mesh and mailbox carry a sha256 reference and fileshare CoT only.";
    }
}
