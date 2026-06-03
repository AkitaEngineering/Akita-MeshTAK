package com.atak.plugins.impl;

import android.content.Context;
import android.content.res.Configuration;

import com.atakmap.android.maps.MapComponent;

public abstract class AbstractPluginLifecycle {

    protected AbstractPluginLifecycle(Context context, MapComponent mapComponent) {
    }

    public final void onConfigurationChanged(Configuration configuration) {
    }

    public final void onDestroy() {
    }

    public final void onFinish() {
    }

    public final void onPause() {
    }

    public final void onResume() {
    }

    public final void onStart() {
    }

    public final void onStop() {
    }
}