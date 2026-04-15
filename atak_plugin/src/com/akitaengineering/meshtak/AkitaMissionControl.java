package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AkitaMissionControl {

    private static final String TAG = "AkitaMissionControl";
    public static final String PREF_MAILBOX_RECORDS = "mission_mailbox_records";
    public static final String PREF_REPLAY_EVENTS = "mission_replay_events";
    public static final String PREF_AUTO_FAILOVER = "transport_auto_failover";

    public static final String ROUTE_BLE = "ble";
    public static final String ROUTE_SERIAL = "serial";
    public static final String ROUTE_MOCK = "mock";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_FLIGHT = "IN_FLIGHT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_FAILED = "FAILED";

    private static final int MAX_MAILBOX_RECORDS = 48;
    private static final int MAX_REPLAY_EVENTS = 160;
    private static final String PREVIEW_MESSAGE_ID = "MSGPREVIEW";

    private static AkitaMissionControl instance;

    private final SharedPreferences preferences;

    private AkitaMissionControl(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public static synchronized AkitaMissionControl getInstance(Context context) {
        if (instance == null) {
            instance = new AkitaMissionControl(context);
        }
        return instance;
    }

    public interface RouteSender {
        boolean isRouteAvailable(String route);

        boolean send(String route, byte[] data);
    }

    public static final class MailboxRecord {
        public final String messageId;
        public final String format;
        public final String payload;
        public final String preferredRoute;
        public final String lastRoute;
        public final String status;
        public final int attempts;
        public final long createdAt;
        public final long updatedAt;
        public final String detail;
        public final int payloadBytes;

        private MailboxRecord(String messageId,
                              String format,
                              String payload,
                              String preferredRoute,
                              String lastRoute,
                              String status,
                              int attempts,
                              long createdAt,
                              long updatedAt,
                              String detail,
                              int payloadBytes) {
            this.messageId = messageId;
            this.format = format;
            this.payload = payload;
            this.preferredRoute = preferredRoute;
            this.lastRoute = lastRoute;
            this.status = status;
            this.attempts = attempts;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.detail = detail;
            this.payloadBytes = payloadBytes;
        }

        private JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("messageId", messageId);
            json.put("format", format);
            json.put("payload", payload);
            json.put("preferredRoute", preferredRoute);
            json.put("lastRoute", lastRoute);
            json.put("status", status);
            json.put("attempts", attempts);
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
            json.put("detail", detail);
            json.put("payloadBytes", payloadBytes);
            return json;
        }

        private static MailboxRecord fromJson(JSONObject json) {
            return new MailboxRecord(
                    json.optString("messageId", ""),
                    json.optString("format", "Plain Text"),
                    json.optString("payload", ""),
                    json.optString("preferredRoute", ROUTE_BLE),
                    json.optString("lastRoute", ""),
                    json.optString("status", STATUS_PENDING),
                    json.optInt("attempts", 0),
                    json.optLong("createdAt", 0L),
                    json.optLong("updatedAt", 0L),
                    json.optString("detail", ""),
                    json.optInt("payloadBytes", 0));
        }
    }

    public static final class ReplayEvent {
        public final long timestamp;
        public final String eventType;
        public final String route;
        public final String status;
        public final String format;
        public final String payload;
        public final String detail;

        private ReplayEvent(long timestamp,
                            String eventType,
                            String route,
                            String status,
                            String format,
                            String payload,
                            String detail) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.route = route;
            this.status = status;
            this.format = format;
            this.payload = payload;
            this.detail = detail;
        }

        private JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("timestamp", timestamp);
            json.put("eventType", eventType);
            json.put("route", route);
            json.put("status", status);
            json.put("format", format);
            json.put("payload", payload);
            json.put("detail", detail);
            return json;
        }

        private static ReplayEvent fromJson(JSONObject json) {
            return new ReplayEvent(
                    json.optLong("timestamp", 0L),
                    json.optString("eventType", ""),
                    json.optString("route", ""),
                    json.optString("status", ""),
                    json.optString("format", ""),
                    json.optString("payload", ""),
                    json.optString("detail", ""));
        }

        public String getPayloadSnippet() {
            if (TextUtils.isEmpty(payload)) {
                return detail;
            }
            String normalized = payload.replace('\n', ' ').replace('\r', ' ').trim();
            if (normalized.length() <= 96) {
                return normalized;
            }
            return normalized.substring(0, 93) + "...";
        }
    }

    public static final class QueueSnapshot {
        public final int pendingCount;
        public final int inFlightCount;
        public final int deliveredCount;
        public final int failedCount;
        public final int replayCheckpointCount;
        public final String summary;
        public final String failoverSummary;
        public final String lastEventSummary;

        private QueueSnapshot(int pendingCount,
                              int inFlightCount,
                              int deliveredCount,
                              int failedCount,
                              int replayCheckpointCount,
                              String summary,
                              String failoverSummary,
                              String lastEventSummary) {
            this.pendingCount = pendingCount;
            this.inFlightCount = inFlightCount;
            this.deliveredCount = deliveredCount;
            this.failedCount = failedCount;
            this.replayCheckpointCount = replayCheckpointCount;
            this.summary = summary;
            this.failoverSummary = failoverSummary;
            this.lastEventSummary = lastEventSummary;
        }
    }

    public static final class DispatchBatchResult {
        public final int dispatchedCount;
        public final int queuedCount;
        public final int failoverCount;
        public final boolean anyFailures;
        public final String lastRoute;
        public final String summary;

        private DispatchBatchResult(int dispatchedCount,
                                    int queuedCount,
                                    int failoverCount,
                                    boolean anyFailures,
                                    String lastRoute,
                                    String summary) {
            this.dispatchedCount = dispatchedCount;
            this.queuedCount = queuedCount;
            this.failoverCount = failoverCount;
            this.anyFailures = anyFailures;
            this.lastRoute = lastRoute;
            this.summary = summary;
        }
    }

    public synchronized MailboxRecord queueMessage(String format, String payload, String preferredRoute) {
        List<MailboxRecord> records = readMailboxRecords();
        long now = System.currentTimeMillis();
        MailboxRecord record = new MailboxRecord(
                createMessageId(),
                normalizeFormat(format),
                payload == null ? "" : payload,
                normalizeRoute(preferredRoute),
                "",
                STATUS_PENDING,
                0,
                now,
                now,
                "Queued for bearer assignment",
                buildMailboxCommand(PREVIEW_MESSAGE_ID, format, payload).getBytes(StandardCharsets.UTF_8).length);
        records.add(record);
        trimMailboxRecords(records);

        List<ReplayEvent> events = readReplayEvents();
        events.add(new ReplayEvent(now,
                "MAILBOX_QUEUED",
                record.preferredRoute,
                STATUS_PENDING,
                record.format,
                record.payload,
                "Message queued for guaranteed delivery"));
        trimReplayEvents(events);
        writeState(records, events);
        return record;
    }

    public synchronized DispatchBatchResult dispatchPendingMessages(RouteSender sender,
                                                                    boolean autoFailover,
                                                                    boolean mockMode) {
        List<MailboxRecord> records = readMailboxRecords();
        List<ReplayEvent> events = readReplayEvents();
        int dispatchedCount = 0;
        int queuedCount = 0;
        int failoverCount = 0;
        boolean anyFailures = false;
        String lastRoute = "";

        List<MailboxRecord> updatedRecords = new ArrayList<>();
        for (MailboxRecord record : records) {
            if (!STATUS_PENDING.equals(record.status) && !STATUS_FAILED.equals(record.status)) {
                updatedRecords.add(record);
                continue;
            }

            DispatchOutcome outcome = dispatchSingleRecord(record, sender, autoFailover, mockMode, events);
            updatedRecords.add(outcome.record);
            if (outcome.dispatched) {
                dispatchedCount++;
                lastRoute = outcome.record.lastRoute;
            } else {
                queuedCount++;
            }
            if (outcome.usedFailover) {
                failoverCount++;
            }
            anyFailures |= outcome.failed;
        }

        trimMailboxRecords(updatedRecords);
        trimReplayEvents(events);
        writeState(updatedRecords, events);

        String summary;
        if (dispatchedCount > 0) {
            summary = failoverCount > 0
                    ? String.format(Locale.US, "%d queued frame(s) advanced with %d failover reroute(s).", dispatchedCount, failoverCount)
                    : String.format(Locale.US, "%d queued frame(s) advanced to the active bearer.", dispatchedCount);
        } else if (queuedCount > 0) {
            summary = "Mailbox preserved queued traffic until a bearer becomes available.";
        } else {
            summary = "Mailbox is clear.";
        }

        return new DispatchBatchResult(dispatchedCount, queuedCount, failoverCount, anyFailures, lastRoute, summary);
    }

    public synchronized boolean consumeIncomingStatus(String line, String route) {
        if (TextUtils.isEmpty(line)) {
            return false;
        }

        String normalizedRoute = normalizeRoute(route);
        if (line.startsWith(Config.STATUS_MAILBOX_ACK_PREFIX)) {
            String body = line.substring(Config.STATUS_MAILBOX_ACK_PREFIX.length());
            int separator = body.indexOf(':');
            if (separator <= 0) {
                return false;
            }
            String messageId = body.substring(0, separator).trim();
            String state = body.substring(separator + 1).trim();
            updateMailboxStatus(messageId, state, normalizedRoute, describeAckState(state));
            return true;
        }

        if (line.startsWith(Config.STATUS_MAILBOX_RX_PREFIX)) {
            String body = line.substring(Config.STATUS_MAILBOX_RX_PREFIX.length());
            InboundMailboxStatus inboundStatus = parseInboundMailboxStatus(body);
            if (inboundStatus == null) {
                return false;
            }
            List<ReplayEvent> events = readReplayEvents();
            events.add(new ReplayEvent(
                    System.currentTimeMillis(),
                    "MAILBOX_RX",
                    normalizedRoute,
                    STATUS_DELIVERED,
                    inboundStatus.format,
                    inboundStatus.payload,
                    inboundStatus.messageId.isEmpty()
                            ? "Inbound mission traffic from " + inboundStatus.nodeId
                            : "Inbound mission traffic from " + inboundStatus.nodeId + " • " + inboundStatus.messageId));
            trimReplayEvents(events);
            persistReplayEvents(events);
            return true;
        }

        if (line.startsWith(Config.STATUS_PROVISION_STAGED_PREFIX)) {
            recordProvisioningEvent("PROVISIONING_STAGED", line.substring(Config.STATUS_PROVISION_STAGED_PREFIX.length()).trim(), normalizedRoute);
            return true;
        }

        if (line.startsWith(Config.STATUS_PROVISION_FAILED_PREFIX)) {
            recordProvisioningEvent("PROVISIONING_FAILED", line.substring(Config.STATUS_PROVISION_FAILED_PREFIX.length()).trim(), normalizedRoute);
            return true;
        }

        return false;
    }

    public synchronized void recordProvisioningEvent(String eventType, String detail, String route) {
        List<ReplayEvent> events = readReplayEvents();
        events.add(new ReplayEvent(
                System.currentTimeMillis(),
                eventType,
                normalizeRoute(route),
                STATUS_DELIVERED,
                "Provisioning",
                "",
                detail));
        trimReplayEvents(events);
            persistReplayEvents(events);
    }

    public synchronized QueueSnapshot getQueueSnapshot(boolean autoFailover) {
        List<MailboxRecord> records = readMailboxRecords();
        List<ReplayEvent> events = readReplayEvents();
        int pendingCount = 0;
        int inFlightCount = 0;
        int deliveredCount = 0;
        int failedCount = 0;
        for (MailboxRecord record : records) {
            if (STATUS_PENDING.equals(record.status)) {
                pendingCount++;
            } else if (STATUS_IN_FLIGHT.equals(record.status)) {
                inFlightCount++;
            } else if (STATUS_DELIVERED.equals(record.status)) {
                deliveredCount++;
            } else if (STATUS_FAILED.equals(record.status)) {
                failedCount++;
            }
        }

        ReplayEvent lastEvent = events.isEmpty() ? null : events.get(events.size() - 1);
        String summary;
        if (pendingCount > 0) {
            summary = String.format(Locale.US, "%d frame(s) are preserved in the mailbox and waiting for a bearer.", pendingCount);
        } else if (inFlightCount > 0) {
            summary = String.format(Locale.US, "%d frame(s) are in flight and awaiting a peer receipt acknowledgement.", inFlightCount);
        } else if (failedCount > 0) {
            summary = String.format(Locale.US, "%d frame(s) require another relay attempt.", failedCount);
        } else if (deliveredCount > 0) {
            summary = String.format(Locale.US, "%d frame(s) have been acknowledged by a peer mailbox over the mesh.", deliveredCount);
        } else {
            summary = "Mailbox is clear. New traffic will queue if no bearer is available.";
        }

        String failoverSummary = autoFailover
                ? "Auto failover armed: BLE and Serial can preserve the queue together."
                : "Failover locked: queued frames will wait for the selected bearer.";
        String lastEventSummary = lastEvent == null
                ? "No replay checkpoints recorded yet."
                : lastEvent.eventType + " • " + lastEvent.getPayloadSnippet();

        return new QueueSnapshot(
                pendingCount,
                inFlightCount,
                deliveredCount,
                failedCount,
                events.size(),
                summary,
                failoverSummary,
                lastEventSummary);
    }

    public synchronized List<ReplayEvent> getReplayTimeline() {
        return new ArrayList<>(readReplayEvents());
    }

    public synchronized List<ReplayEvent> getReplayableTimeline() {
        List<ReplayEvent> replayable = new ArrayList<>();
        for (ReplayEvent event : readReplayEvents()) {
            if (!TextUtils.isEmpty(event.payload)) {
                replayable.add(event);
            }
        }
        return replayable;
    }

    public static boolean isAutoFailoverEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(PREF_AUTO_FAILOVER, true);
    }

    public static int getPreparedMailboxCommandBytes(String format, String payload) {
        return buildMailboxCommand(PREVIEW_MESSAGE_ID, format, payload).getBytes(StandardCharsets.UTF_8).length;
    }

    public static String buildMailboxCommand(String messageId, String format, String payload) {
        return Config.CMD_MAILBOX_PUT_PREFIX
                + sanitizeMessageId(messageId)
                + ":"
                + encodeFormat(format)
                + ":"
                + escapeMailboxPayload(payload == null ? "" : payload);
    }

    public static String escapeMailboxPayload(String payload) {
        return (payload == null ? "" : payload)
                .replace("%", "%25")
                .replace("\r", "%0D")
                .replace("\n", "%0A");
    }

    public static String unescapeMailboxPayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(payload.length());
        for (int index = 0; index < payload.length(); index++) {
            char current = payload.charAt(index);
            if (current == '%' && index + 2 <= payload.length() - 1) {
                String token = payload.substring(index, index + 3);
                if ("%0A".equalsIgnoreCase(token)) {
                    builder.append('\n');
                    index += 2;
                    continue;
                }
                if ("%0D".equalsIgnoreCase(token)) {
                    builder.append('\r');
                    index += 2;
                    continue;
                }
                if ("%25".equalsIgnoreCase(token)) {
                    builder.append('%');
                    index += 2;
                    continue;
                }
            }
            builder.append(current);
        }
        return builder.toString();
    }

    public static String inferFormat(String payload) {
        if (payload == null) {
            return "Plain Text";
        }
        String trimmed = payload.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return "JSON";
        }
        if (trimmed.startsWith("CUSTOM:")) {
            return "Custom";
        }
        return "Plain Text";
    }

    public static String routeLabel(String route) {
        if (ROUTE_SERIAL.equalsIgnoreCase(route)) {
            return "Serial";
        }
        if (ROUTE_MOCK.equalsIgnoreCase(route)) {
            return "Mock";
        }
        return "BLE";
    }

    private DispatchOutcome dispatchSingleRecord(MailboxRecord record,
                                                 RouteSender sender,
                                                 boolean autoFailover,
                                                 boolean mockMode,
                                                 List<ReplayEvent> events) {
        List<String> candidateRoutes = buildCandidateRoutes(record.preferredRoute, autoFailover);
        boolean anyRouteAvailable = false;
        for (String candidateRoute : candidateRoutes) {
            if (!sender.isRouteAvailable(candidateRoute)) {
                continue;
            }
            anyRouteAvailable = true;
            boolean sendSuccess = sender.send(candidateRoute,
                    buildMailboxCommand(record.messageId, record.format, record.payload).getBytes(StandardCharsets.UTF_8));
            if (sendSuccess) {
                long now = System.currentTimeMillis();
                String status = mockMode ? STATUS_DELIVERED : STATUS_IN_FLIGHT;
                String detail = mockMode
                        ? "Simulated mailbox delivery completed"
                        : "Transport write accepted; awaiting device acknowledgement";
                MailboxRecord updated = new MailboxRecord(
                        record.messageId,
                        record.format,
                        record.payload,
                        record.preferredRoute,
                        candidateRoute,
                        status,
                        record.attempts + 1,
                        record.createdAt,
                        now,
                        detail,
                        record.payloadBytes);
                if (!record.preferredRoute.equals(candidateRoute)) {
                    events.add(new ReplayEvent(now,
                            "ROUTE_FAILOVER",
                            candidateRoute,
                            status,
                            record.format,
                            record.payload,
                            "Queued traffic rerouted from " + routeLabel(record.preferredRoute) + " to " + routeLabel(candidateRoute)));
                }
                events.add(new ReplayEvent(now,
                        mockMode ? "MAILBOX_DELIVERED" : "MAILBOX_IN_FLIGHT",
                        candidateRoute,
                        status,
                        record.format,
                        record.payload,
                        detail));
                return new DispatchOutcome(updated, true, !record.preferredRoute.equals(candidateRoute), false);
            }
        }

        long now = System.currentTimeMillis();
        MailboxRecord updated = new MailboxRecord(
                record.messageId,
                record.format,
                record.payload,
                record.preferredRoute,
                record.lastRoute,
                anyRouteAvailable ? STATUS_FAILED : STATUS_PENDING,
                record.attempts + (anyRouteAvailable ? 1 : 0),
                record.createdAt,
                now,
                anyRouteAvailable ? "Transport write failed; mailbox retained the frame" : "Waiting for BLE or Serial to become available",
                record.payloadBytes);
        events.add(new ReplayEvent(now,
                anyRouteAvailable ? "MAILBOX_FAILED" : "MAILBOX_WAITING",
                record.preferredRoute,
                updated.status,
                record.format,
                record.payload,
                updated.detail));
        return new DispatchOutcome(updated, false, false, anyRouteAvailable);
    }

    private synchronized void updateMailboxStatus(String messageId, String status, String route, String detail) {
        List<MailboxRecord> records = readMailboxRecords();
        List<ReplayEvent> events = readReplayEvents();
        String normalizedStatus = normalizeMailboxStatus(status);
        boolean updated = false;
        for (int index = 0; index < records.size(); index++) {
            MailboxRecord record = records.get(index);
            if (!record.messageId.equals(messageId)) {
                continue;
            }
            MailboxRecord replacement = new MailboxRecord(
                    record.messageId,
                    record.format,
                    record.payload,
                    record.preferredRoute,
                    normalizeRoute(route),
                    normalizedStatus,
                    record.attempts,
                    record.createdAt,
                    System.currentTimeMillis(),
                    detail,
                    record.payloadBytes);
            records.set(index, replacement);
            events.add(new ReplayEvent(
                    replacement.updatedAt,
                    STATUS_DELIVERED.equals(replacement.status) ? "PEER_ACK" : "MAILBOX_ACK",
                    replacement.lastRoute,
                    replacement.status,
                    replacement.format,
                    replacement.payload,
                    replacement.status + " • " + detail));
            updated = true;
            break;
        }
        if (!updated) {
            events.add(new ReplayEvent(
                    System.currentTimeMillis(),
                    "MAILBOX_ACK_ORPHAN",
                    normalizeRoute(route),
                    normalizedStatus,
                    "",
                    "",
                    "Device ack received for unknown message " + sanitizeMessageId(messageId)));
        }
        trimMailboxRecords(records);
        trimReplayEvents(events);
        writeState(records, events);
    }

    private List<MailboxRecord> readMailboxRecords() {
        List<MailboxRecord> records = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences.getString(PREF_MAILBOX_RECORDS, "[]"));
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.optJSONObject(index);
                if (item != null) {
                    records.add(MailboxRecord.fromJson(item));
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse mailbox records from preferences", e);
        }
        Collections.sort(records, Comparator.comparingLong(record -> record.createdAt));
        return records;
    }

    private List<ReplayEvent> readReplayEvents() {
        List<ReplayEvent> events = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences.getString(PREF_REPLAY_EVENTS, "[]"));
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.optJSONObject(index);
                if (item != null) {
                    events.add(ReplayEvent.fromJson(item));
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse replay events from preferences", e);
        }
        Collections.sort(events, Comparator.comparingLong(event -> event.timestamp));
        return events;
    }

    private void writeState(List<MailboxRecord> records, List<ReplayEvent> events) {
        preferences.edit()
                .putString(PREF_MAILBOX_RECORDS, writeMailboxRecords(records))
                .putString(PREF_REPLAY_EVENTS, writeReplayEvents(events))
                .apply();
    }

    private void persistReplayEvents(List<ReplayEvent> events) {
        preferences.edit().putString(PREF_REPLAY_EVENTS, writeReplayEvents(events)).apply();
    }

    private String writeMailboxRecords(List<MailboxRecord> records) {
        JSONArray array = new JSONArray();
        for (MailboxRecord record : records) {
            try {
                array.put(record.toJson());
            } catch (JSONException e) {
                Log.w(TAG, "Failed to serialize mailbox record", e);
            }
        }
        return array.toString();
    }

    private String writeReplayEvents(List<ReplayEvent> events) {
        JSONArray array = new JSONArray();
        for (ReplayEvent event : events) {
            try {
                array.put(event.toJson());
            } catch (JSONException e) {
                Log.w(TAG, "Failed to serialize replay event", e);
            }
        }
        return array.toString();
    }

    private void trimMailboxRecords(List<MailboxRecord> records) {
        while (records.size() > MAX_MAILBOX_RECORDS) {
            int removableIndex = findRemovableMailboxIndex(records);
            if (removableIndex < 0) {
                // All records are PENDING or IN_FLIGHT — refuse to drop active work.
                break;
            }
            records.remove(removableIndex);
        }
    }

    private void trimReplayEvents(List<ReplayEvent> events) {
        while (events.size() > MAX_REPLAY_EVENTS) {
            events.remove(0);
        }
    }

    private int findRemovableMailboxIndex(List<MailboxRecord> records) {
        for (int index = 0; index < records.size(); index++) {
            String status = records.get(index).status;
            if (STATUS_DELIVERED.equals(status) || STATUS_FAILED.equals(status)) {
                return index;
            }
        }
        return -1;
    }

    private static List<String> buildCandidateRoutes(String preferredRoute, boolean autoFailover) {
        String normalizedPreferred = normalizeRoute(preferredRoute);
        List<String> candidates = new ArrayList<>();
        candidates.add(normalizedPreferred);
        if (autoFailover) {
            if (ROUTE_BLE.equals(normalizedPreferred)) {
                candidates.add(ROUTE_SERIAL);
            } else if (ROUTE_SERIAL.equals(normalizedPreferred)) {
                candidates.add(ROUTE_BLE);
            }
        }
        return candidates;
    }

    private static String normalizeRoute(String route) {
        if (ROUTE_SERIAL.equalsIgnoreCase(route)) {
            return ROUTE_SERIAL;
        }
        if (ROUTE_MOCK.equalsIgnoreCase(route)) {
            return ROUTE_MOCK;
        }
        return ROUTE_BLE;
    }

    private static String normalizeFormat(String format) {
        if ("JSON".equalsIgnoreCase(format)) {
            return "JSON";
        }
        if ("Custom".equalsIgnoreCase(format)) {
            return "Custom";
        }
        return "Plain Text";
    }

    private static String encodeFormat(String format) {
        if ("JSON".equalsIgnoreCase(format)) {
            return "JSON";
        }
        if ("Custom".equalsIgnoreCase(format)) {
            return "CUSTOM";
        }
        return "TEXT";
    }

    private static String normalizeMailboxStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.US);
        if (normalized.contains(STATUS_DELIVERED)) {
            return STATUS_DELIVERED;
        }
        if (normalized.contains(STATUS_FAILED)) {
            return STATUS_FAILED;
        }
        if (normalized.contains(STATUS_IN_FLIGHT)) {
            return STATUS_IN_FLIGHT;
        }
        return STATUS_PENDING;
    }

    private static String createMessageId() {
        return sanitizeMessageId("MSG" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.US));
    }

    private static String sanitizeMessageId(String messageId) {
        String source = messageId == null ? PREVIEW_MESSAGE_ID : messageId;
        return source.replaceAll("[^A-Za-z0-9_-]", "");
    }

    private static String describeAckState(String rawState) {
        String state = rawState == null ? "" : rawState.trim();
        String normalized = state.toUpperCase(Locale.US);
        if (normalized.startsWith(STATUS_DELIVERED)) {
            int separator = state.indexOf(':');
            if (separator > 0 && separator + 1 < state.length()) {
                return "Peer acknowledgement received from " + state.substring(separator + 1).trim();
            }
            return "Peer acknowledgement received";
        }
        if (normalized.startsWith(STATUS_IN_FLIGHT)) {
            return "Radio accepted frame; awaiting peer receipt";
        }
        if (normalized.startsWith(STATUS_FAILED)) {
            return "Mesh relay failed before peer acknowledgement";
        }
        return "Mailbox acknowledgement received";
    }

    private static InboundMailboxStatus parseInboundMailboxStatus(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        int firstSeparator = body.indexOf(':');
        if (firstSeparator <= 0) {
            return null;
        }

        int secondSeparator = body.indexOf(':', firstSeparator + 1);
        int thirdSeparator = secondSeparator < 0 ? -1 : body.indexOf(':', secondSeparator + 1);
        if (secondSeparator > 0 && thirdSeparator > secondSeparator) {
            String nodeId = body.substring(0, firstSeparator).trim();
            String messageId = body.substring(firstSeparator + 1, secondSeparator).trim();
            String format = decodeFormat(body.substring(secondSeparator + 1, thirdSeparator).trim());
            String payload = unescapeMailboxPayload(body.substring(thirdSeparator + 1));
            return new InboundMailboxStatus(nodeId, messageId, format, payload);
        }

        String nodeId = body.substring(0, firstSeparator).trim();
        String payload = unescapeMailboxPayload(body.substring(firstSeparator + 1));
        return new InboundMailboxStatus(nodeId, "", inferFormat(payload), payload);
    }

    private static String decodeFormat(String formatToken) {
        if ("JSON".equalsIgnoreCase(formatToken)) {
            return "JSON";
        }
        if ("CUSTOM".equalsIgnoreCase(formatToken)) {
            return "Custom";
        }
        return "Plain Text";
    }

    private static final class InboundMailboxStatus {
        private final String nodeId;
        private final String messageId;
        private final String format;
        private final String payload;

        private InboundMailboxStatus(String nodeId, String messageId, String format, String payload) {
            this.nodeId = nodeId;
            this.messageId = messageId;
            this.format = format;
            this.payload = payload;
        }
    }

    private static final class DispatchOutcome {
        private final MailboxRecord record;
        private final boolean dispatched;
        private final boolean usedFailover;
        private final boolean failed;

        private DispatchOutcome(MailboxRecord record, boolean dispatched, boolean usedFailover, boolean failed) {
            this.record = record;
            this.dispatched = dispatched;
            this.usedFailover = usedFailover;
            this.failed = failed;
        }
    }
}