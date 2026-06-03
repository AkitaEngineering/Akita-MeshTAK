package com.akitaengineering.meshtak;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginLifecycle;

public class AkitaMeshTAKLifecycle extends AbstractPluginLifecycle {

    public AkitaMeshTAKLifecycle(Context context) {
        super(context, new AkitaMeshTAKMapComponent());
    }
}