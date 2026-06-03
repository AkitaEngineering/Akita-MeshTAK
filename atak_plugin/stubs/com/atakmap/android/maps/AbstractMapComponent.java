package com.atakmap.android.maps;

import android.content.Context;
import android.content.Intent;

public abstract class AbstractMapComponent extends MapComponent {

    @Override
    public void onCreate(Context context, Intent intent, MapView mapView) {
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        onDestroyImpl(context, mapView);
    }

    protected abstract void onDestroyImpl(Context context, MapView mapView);
}