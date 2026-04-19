package com.atakmap.android.plugin.ui;

import android.graphics.Canvas;

import com.atakmap.android.maps.MapView;

public class PluginMapOverlay {

    private final MapView mapView;

    public PluginMapOverlay(MapView mapView) {
        this.mapView = mapView;
    }

    protected MapView getMapView() {
        return mapView;
    }

    public void draw(Canvas canvas, MapView mapView) {
    }
}
