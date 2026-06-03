package com.atakmap.app.preferences;

import android.graphics.drawable.Drawable;

public final class ToolsPreferenceFragment {

    private ToolsPreferenceFragment() {
    }

    public static void register(ToolPreference preference) {
    }

    public static void unregister(String key) {
    }

    public static class ToolPreference {
        public ToolPreference(String title, String summary, String key, Drawable icon, Object fragment) {
        }
    }
}
