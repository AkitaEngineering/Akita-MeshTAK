package com.atakmap.android.plugin;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.plugin.ui.PluginContextMenu;
import com.atakmap.android.plugin.ui.PluginMapOverlay;
import com.atakmap.android.plugin.ui.PluginPreferenceFragment;
import com.atakmap.android.plugin.ui.PluginToolbar;
import com.atakmap.android.plugin.ui.PluginView;

import java.util.Collections;
import java.util.List;

public abstract class AbstractPlugin {

    public void onCreate(Context context, MapView view) {
    }

    public void onDestroy() {
    }

    public List<PluginMapOverlay> getOverlays() {
        return Collections.emptyList();
    }

    public List<PluginToolbar> getToolbars() {
        return Collections.emptyList();
    }

    public PluginView onCreateView(String viewId, PluginLayoutInflater inflater) {
        return null;
    }

    public PluginPreferenceFragment getPreferenceFragment() {
        return null;
    }

    public PluginLayoutInflater getLayoutInflater(PluginLayoutInflater parent) {
        return parent;
    }

    public void onReceive(Context context, Intent intent) {
    }

    public PluginContextMenu getContextMenu(Object caller) {
        return null;
    }

    public void onUnbind(Intent intent) {
    }
}
