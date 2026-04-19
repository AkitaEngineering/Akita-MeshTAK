package com.atakmap.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CotPoint {

    private static final Pattern UID_PATTERN = Pattern.compile("uid=\"([^\"]+)\"");
    private static final Pattern TYPE_PATTERN = Pattern.compile("type=\"([^\"]+)\"");
    private static final Pattern LAT_PATTERN = Pattern.compile("lat=\"([^\"]+)\"");
    private static final Pattern LON_PATTERN = Pattern.compile("lon=\"([^\"]+)\"");
    private static final Pattern CALLSIGN_PATTERN = Pattern.compile("<contact[^>]*callsign=\"([^\"]+)\"");

    private final String uid;
    private final String type;
    private final double latitude;
    private final double longitude;
    private final Map<String, Map<String, String>> detail;

    private CotPoint(String uid,
                     String type,
                     double latitude,
                     double longitude,
                     Map<String, Map<String, String>> detail) {
        this.uid = uid;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.detail = detail;
    }

    public static CotPoint fromXml(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return null;
        }

        String uid = match(xml, UID_PATTERN, "stub-cot");
        String type = match(xml, TYPE_PATTERN, null);
        double latitude = parseDouble(match(xml, LAT_PATTERN, "0"));
        double longitude = parseDouble(match(xml, LON_PATTERN, "0"));
        String callsign = match(xml, CALLSIGN_PATTERN, null);

        Map<String, Map<String, String>> detail = new HashMap<>();
        if (callsign != null) {
            Map<String, String> contact = new HashMap<>();
            contact.put("callsign", callsign);
            detail.put("contact", contact);
        }

        return new CotPoint(uid, type, latitude, longitude, detail);
    }

    public String getUid() {
        return uid;
    }

    public String getType() {
        return type;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Map<String, Map<String, String>> getDetail() {
        return Collections.unmodifiableMap(detail);
    }

    private static String match(String source, Pattern pattern, String fallback) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0d;
        }
    }
}
