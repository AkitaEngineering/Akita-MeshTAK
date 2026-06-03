package com.atakmap.android.dropdown;

import android.content.BroadcastReceiver;
import android.view.View;

import com.atakmap.android.maps.MapView;

public abstract class DropDownReceiver extends BroadcastReceiver {

    protected static final double HALF_WIDTH = 0.5d;
    protected static final double FULL_WIDTH = 1.0d;
    protected static final double HALF_HEIGHT = 0.5d;
    protected static final double FULL_HEIGHT = 1.0d;

    protected DropDownReceiver(MapView mapView) {
    }

    protected boolean isClosed() {
        return true;
    }

    protected void unhideDropDown() {
    }

    protected void showDropDown(View view, double width, double height, double landscapeWidth,
                                double landscapeHeight, boolean ignoreBackButton) {
    }

    protected void setAssociationKey(String key) {
    }

    public void dispose() {
        disposeImpl();
    }

    public void disposeImpl() {
    }
}
