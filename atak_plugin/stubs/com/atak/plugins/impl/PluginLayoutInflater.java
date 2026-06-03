package com.atak.plugins.impl;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public final class PluginLayoutInflater {

    private PluginLayoutInflater() {
    }

    public static View inflate(Context context, int resource, ViewGroup root, boolean attachToRoot) {
        return View.inflate(context, resource, root);
    }

    public static void dispose() {
    }
}
