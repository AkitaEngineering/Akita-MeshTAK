package com.atakmap.api.map;

import com.atakmap.api.Point2;

public class Marker extends MapItem {

    private Point2 geoPoint;
    private String title;
    private String type;

    public Marker(Point2 geoPoint) {
        this.geoPoint = geoPoint;
    }

    public Point2 getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(Point2 geoPoint) {
        this.geoPoint = geoPoint;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
