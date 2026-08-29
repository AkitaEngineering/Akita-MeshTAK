package com.akitaengineering.meshtak.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AkitaMissionMarkerRegistry {

    private static final int MAX_MARKERS = 64;

    private final Map<String, TrackedMarker> trackedMarkers = new LinkedHashMap<>();

    private static AkitaMissionMarkerRegistry instance;

    private AkitaMissionMarkerRegistry() {
    }

    public static synchronized AkitaMissionMarkerRegistry getInstance() {
        if (instance == null) {
            instance = new AkitaMissionMarkerRegistry();
        }
        return instance;
    }

    public synchronized void recordMarker(String uid,
                                          String title,
                                          double latitude,
                                          double longitude,
                                          String source) {
        recordMarker(uid, title, latitude, longitude, source, 0L);
    }

    public synchronized void recordMarker(String uid,
                                          String title,
                                          double latitude,
                                          double longitude,
                                          String source,
                                          long staleAtMillis) {
        if (uid == null || uid.trim().isEmpty()) {
            return;
        }
        trackedMarkers.put(uid, new TrackedMarker(uid, title, latitude, longitude, source, System.currentTimeMillis(), staleAtMillis));
        trimToMaxSize();
    }

    public synchronized List<TrackedMarker> getTrackedMarkers() {
        List<TrackedMarker> markers = new ArrayList<>(trackedMarkers.values());
        markers.sort(Comparator.comparingLong((TrackedMarker marker) -> marker.lastUpdatedAt).reversed());
        return markers;
    }

    public synchronized List<TrackedMarker> getStaleMarkers(long thresholdMillis) {
        List<TrackedMarker> staleMarkers = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (TrackedMarker marker : trackedMarkers.values()) {
            long staleAt = marker.staleAtMillis > 0L ? marker.staleAtMillis : marker.lastUpdatedAt + thresholdMillis;
            if (now >= staleAt) {
                staleMarkers.add(marker);
            }
        }
        staleMarkers.sort(Comparator.comparingLong((TrackedMarker marker) -> marker.lastUpdatedAt));
        return staleMarkers;
    }

    public synchronized TrackedMarker getMostRecentMarker() {
        TrackedMarker latest = null;
        for (TrackedMarker marker : trackedMarkers.values()) {
            if (latest == null || marker.lastUpdatedAt > latest.lastUpdatedAt) {
                latest = marker;
            }
        }
        return latest;
    }

    public synchronized int getTrackedMarkerCount() {
        return trackedMarkers.size();
    }

    public synchronized void resetForTests() {
        trackedMarkers.clear();
    }

    private void trimToMaxSize() {
        while (trackedMarkers.size() > MAX_MARKERS) {
            String firstKey = trackedMarkers.keySet().iterator().next();
            trackedMarkers.remove(firstKey);
        }
    }

    public static final class TrackedMarker {
        public final String uid;
        public final String title;
        public final double latitude;
        public final double longitude;
        public final String source;
        public final long lastUpdatedAt;
        public final long staleAtMillis;

        private TrackedMarker(String uid,
                              String title,
                              double latitude,
                              double longitude,
                              String source,
                              long lastUpdatedAt,
                              long staleAtMillis) {
            this.uid = uid;
            this.title = title;
            this.latitude = latitude;
            this.longitude = longitude;
            this.source = source;
            this.lastUpdatedAt = lastUpdatedAt;
            this.staleAtMillis = staleAtMillis;
        }
    }
}