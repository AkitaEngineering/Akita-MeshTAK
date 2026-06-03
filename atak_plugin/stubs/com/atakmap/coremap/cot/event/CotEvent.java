package com.atakmap.coremap.cot.event;

import com.atakmap.coremap.maps.coords.GeoPoint;

public class CotEvent {

    private final String uid;
    private final String type;
    private final GeoPoint geoPoint;
    private final CotDetail contactDetail;

    private CotEvent(String uid, String type, GeoPoint geoPoint, CotDetail contactDetail) {
        this.uid = uid;
        this.type = type;
        this.geoPoint = geoPoint;
        this.contactDetail = contactDetail;
    }

    public static CotEvent parse(String xml) {
        String uid = readAttribute(xml, "uid");
        String type = readAttribute(xml, "type");
        double lat = parseDouble(readAttribute(xml, "lat"));
        double lon = parseDouble(readAttribute(xml, "lon"));
        CotDetail contact = new CotDetail();
        String callsign = readAttribute(xml, "callsign");
        if (callsign != null) {
            contact.setAttribute("callsign", callsign);
        }
        return new CotEvent(uid, type, new GeoPoint(lat, lon), contact);
    }

    public boolean isValid() {
        return uid != null && geoPoint != null;
    }

    public String getUID() {
        return uid;
    }

    public String getType() {
        return type;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public CotDetail findDetail(String name) {
        return "contact".equals(name) ? contactDetail : null;
    }

    private static String readAttribute(String xml, String name) {
        if (xml == null) {
            return null;
        }
        String prefix = name + "=\"";
        int start = xml.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = xml.indexOf('"', start);
        return end > start ? xml.substring(start, end) : null;
    }

    private static double parseDouble(String value) {
        if (value == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0d;
        }
    }
}
