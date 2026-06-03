package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atak.plugins.impl.PluginLayoutInflater;
import com.akitaengineering.meshtak.AkitaMeshTAKPlugin;

public class AkitaDropDownReceiver extends DropDownReceiver {

    public static final String SHOW_AKITA_MESHTAK = "com.akitaengineering.meshtak.SHOW_AKITA_MESHTAK";

    private final AkitaMeshTAKPlugin runtimePlugin;

    public AkitaDropDownReceiver(MapView mapView, Context pluginContext, AkitaMeshTAKPlugin runtimePlugin) {
        super(mapView);
        this.runtimePlugin = runtimePlugin;
    }

    @Override
    public void disposeImpl() {
        PluginLayoutInflater.dispose();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !SHOW_AKITA_MESHTAK.equals(intent.getAction())) {
            return;
        }

        if (!isClosed()) {
            unhideDropDown();
            return;
        }

        showDropDown(runtimePlugin.getSendDataView(), HALF_WIDTH, FULL_HEIGHT,
                FULL_WIDTH, HALF_HEIGHT, false);
        setAssociationKey("akitaMeshTAKPreference");
    }
}