package com.atakmap.android.dropdown;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;

public class DropDownMapComponent extends AbstractMapComponent {

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
    }

    protected void registerDropDownReceiver(BroadcastReceiver receiver, IntentFilter filter) {
    }
}
