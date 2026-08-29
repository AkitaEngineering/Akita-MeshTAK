package com.akitaengineering.meshtak;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * OpenTAKServer Meshtastic MQTT JSON mapping for mailbox, position, and NodeInfo.
 */
public final class MeshtasticMqttCodec {

    public static final String DEFAULT_ROOT = "msh/2/json";
    public static final String DEFAULT_CHANNEL_NAME = "LongFast";
    public static final String TYPE_POSITION = "position";
    public static final String TYPE_NODEINFO = "nodeinfo";
    public static final String TYPE_TEXT = "sendtext";
    public static final String PREF_ROOT = "meshtastic_mqtt_root";
    public static final String PREF_CHANNEL_NAME = "meshtastic_mqtt_channel";
    public static final String PREF_ATAK_PLUGIN = "meshtastic_atak_plugin";

    public static final class Envelope {
        public final String topic;
        public final String type;
        public final String sender;
        public final long from;
        public final String json;

        Envelope(String topic, String type, String sender, long from, String json) {
            this.topic = topic;
            this.type = type;
            this.sender = sender;
            this.from = from;
            this.json = json;
        }
    }

    private MeshtasticMqttCodec() {
    }

    public static String nodeIdToSender(String nodeId) {
        String hex = nodeId == null ? "" : nodeId.trim();
        if (hex.startsWith("!")) {
            hex = hex.substring(1);
        }
        hex = hex.toLowerCase(Locale.US);
        if (hex.length() > 8) {
            hex = hex.substring(hex.length() - 8);
        }
        while (hex.length() < 8) {
            hex = "0" + hex;
        }
        return "!" + hex;
    }

    public static long nodeIdToFrom(String nodeId) {
        String hex = nodeIdToSender(nodeId).substring(1);
        try {
            return Long.parseLong(hex, 16);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    public static String topic(String root, String channelName, String nodeId) {
        String safeRoot = (root == null || root.trim().isEmpty()) ? DEFAULT_ROOT : root.trim();
        if (safeRoot.endsWith("/")) {
            safeRoot = safeRoot.substring(0, safeRoot.length() - 1);
        }
        String channel = (channelName == null || channelName.trim().isEmpty())
                ? DEFAULT_CHANNEL_NAME
                : channelName.trim();
        return safeRoot + "/" + channel + "/" + nodeIdToSender(nodeId);
    }

    public static Envelope position(String root,
                                    String channelName,
                                    String nodeId,
                                    double latitude,
                                    double longitude,
                                    int altitude,
                                    int speed,
                                    int battery,
                                    long timestampSeconds) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("latitude_i", AtakPluginPacket.latitudeToFixed(latitude));
        payload.put("longitude_i", AtakPluginPacket.latitudeToFixed(longitude));
        payload.put("altitude", altitude);
        payload.put("speed", Math.max(0, speed));
        payload.put("battery_level", Math.max(0, Math.min(100, battery)));
        return envelope(root, channelName, nodeId, TYPE_POSITION, payload, timestampSeconds);
    }

    public static Envelope nodeInfo(String root,
                                    String channelName,
                                    String nodeId,
                                    String longName,
                                    String shortName,
                                    long timestampSeconds) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("id", nodeIdToSender(nodeId));
        payload.put("longname", longName == null ? "" : longName);
        payload.put("shortname", shortName == null || shortName.isEmpty()
                ? shorten(longName, nodeId)
                : shortName);
        payload.put("hardware", 0);
        payload.put("role", 0);
        return envelope(root, channelName, nodeId, TYPE_NODEINFO, payload, timestampSeconds);
    }

    public static Envelope text(String root,
                                String channelName,
                                String nodeId,
                                String message,
                                long timestampSeconds) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("text", message == null ? "" : message);
        return envelope(root, channelName, nodeId, TYPE_TEXT, payload, timestampSeconds);
    }

    public static Envelope fromMailbox(String root,
                                       String channelName,
                                       String nodeId,
                                       String mailboxPayload,
                                       long timestampSeconds) throws JSONException {
        CotEventFactory.ChatPayload chat = CotEventFactory.parseMailboxChat(mailboxPayload);
        if (chat != null) {
            return text(root, channelName, nodeId, chat.message, timestampSeconds);
        }
        return text(root, channelName, nodeId, mailboxPayload, timestampSeconds);
    }

    public static String parseTextPayload(String json) throws JSONException {
        JSONObject envelope = new JSONObject(json);
        String type = envelope.optString("type", "");
        JSONObject payload = envelope.optJSONObject("payload");
        if (payload == null) {
            return "";
        }
        if (TYPE_TEXT.equals(type) || payload.has("text")) {
            return payload.optString("text", "");
        }
        return "";
    }

    public static String parseSender(String json) throws JSONException {
        JSONObject envelope = new JSONObject(json);
        String sender = envelope.optString("sender", "");
        if (!sender.isEmpty()) {
            return sender;
        }
        return nodeIdToSender(Long.toHexString(envelope.optLong("from", 0L)));
    }

    private static Envelope envelope(String root,
                                     String channelName,
                                     String nodeId,
                                     String type,
                                     JSONObject payload,
                                     long timestampSeconds) throws JSONException {
        String sender = nodeIdToSender(nodeId);
        long from = nodeIdToFrom(nodeId);
        JSONObject json = new JSONObject();
        json.put("channel", 0);
        json.put("from", from);
        json.put("id", timestampSeconds);
        json.put("payload", payload);
        json.put("sender", sender);
        json.put("timestamp", timestampSeconds);
        json.put("type", type);
        return new Envelope(topic(root, channelName, nodeId), type, sender, from, json.toString());
    }

    private static String shorten(String longName, String nodeId) {
        if (longName != null && longName.trim().length() >= 2) {
            String compact = longName.trim().replace(" ", "");
            return compact.substring(0, Math.min(4, compact.length())).toUpperCase(Locale.US);
        }
        String sender = nodeIdToSender(nodeId);
        return sender.substring(Math.max(1, sender.length() - 4)).toUpperCase(Locale.US);
    }
}
