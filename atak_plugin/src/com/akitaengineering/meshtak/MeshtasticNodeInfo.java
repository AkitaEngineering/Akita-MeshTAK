package com.akitaengineering.meshtak;

/**
 * Aligns Meshtastic NodeInfo/position fields with OpenTAKServer CoT contact metadata.
 */
public final class MeshtasticNodeInfo {

    public final String nodeId;
    public final String longName;
    public final String shortName;
    public final double latitude;
    public final double longitude;
    public final double altitude;
    public final int batteryPercent;
    public final int speedMps;
    public final int courseDeg;

    public MeshtasticNodeInfo(String nodeId,
                              String longName,
                              String shortName,
                              double latitude,
                              double longitude,
                              double altitude,
                              int batteryPercent,
                              int speedMps,
                              int courseDeg) {
        this.nodeId = nodeId == null ? "" : nodeId.trim();
        this.longName = longName == null ? "" : longName.trim();
        this.shortName = shortName == null ? "" : shortName.trim();
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.batteryPercent = batteryPercent;
        this.speedMps = speedMps;
        this.courseDeg = courseDeg;
    }

    public String callsign() {
        if (!longName.isEmpty()) {
            return OperatorIdentity.sanitizeToken(longName, OperatorIdentity.MAX_CALLSIGN_LENGTH);
        }
        if (!shortName.isEmpty()) {
            return OperatorIdentity.sanitizeToken(shortName, OperatorIdentity.MAX_CALLSIGN_LENGTH);
        }
        return OperatorIdentity.sanitizeToken(nodeId, OperatorIdentity.MAX_CALLSIGN_LENGTH);
    }

    public String uid() {
        String id = nodeId.isEmpty() ? callsign() : nodeId;
        if (id.startsWith("!")) {
            return id.substring(1).toUpperCase(java.util.Locale.ROOT);
        }
        return id.toUpperCase(java.util.Locale.ROOT);
    }

    public String locationCoT(String team, String role, String missionName, long epochMillis, int staleSeconds) {
        return CotEventFactory.locationEvent(
                uid(),
                callsign(),
                team,
                role,
                missionName,
                latitude,
                longitude,
                altitude,
                epochMillis,
                staleSeconds,
                batteryPercent,
                speedMps,
                courseDeg);
    }

    public AtakPluginPacket.Packet toPli(String team, String role) {
        return AtakPluginPacket.Packet.pli(
                new AtakPluginPacket.Contact(callsign(), uid()),
                team,
                role,
                batteryPercent,
                latitude,
                longitude,
                (int) Math.round(altitude),
                speedMps,
                courseDeg);
    }
}
