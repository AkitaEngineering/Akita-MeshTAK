package com.akitaengineering.meshtak.ui;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;

public class AkitaTool extends AbstractPluginTool {

    public AkitaTool(Context context) {
        super(context,
                context.getString(com.akitaengineering.meshtak.R.string.app_name),
                context.getString(com.akitaengineering.meshtak.R.string.app_desc),
                context.getDrawable(android.R.drawable.ic_menu_send),
                AkitaDropDownReceiver.SHOW_AKITA_MESHTAK);
    }

    public void dispose() {
    }
}