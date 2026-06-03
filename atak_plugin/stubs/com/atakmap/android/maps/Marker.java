package com.atakmap.android.maps;

import com.atakmap.coremap.maps.coords.GeoPoint;

public class Marker extends MapItem {

    private GeoPoint point;
    private String title;
    private String type;

    public Marker(GeoPoint point, String uid) {
        super(uid);
        this.point = point;
    }

    public GeoPoint getPoint() {
        return point;
    }

    public void setPoint(GeoPoint point) {
        this.point = point;
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
