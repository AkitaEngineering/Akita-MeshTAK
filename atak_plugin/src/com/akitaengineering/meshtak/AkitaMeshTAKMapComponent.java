package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.akitaengineering.meshtak.ui.AkitaDropDownReceiver;
import com.akitaengineering.meshtak.ui.AkitaPreferenceFragment;
import com.akitaengineering.meshtak.ui.AkitaTool;

public class AkitaMeshTAKMapComponent extends DropDownMapComponent {

    private static final String TOOL_PREFERENCE_KEY = "akitaMeshTAKPreference";

    private final AkitaMeshTAKPlugin runtimePlugin = new AkitaMeshTAKPlugin();
    private AkitaDropDownReceiver dropDownReceiver;
    private AkitaTool akitaTool;

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        super.onCreate(context, intent, view);
        runtimePlugin.onCreate(context, view);

        attachPersistentViews(view);

        dropDownReceiver = new AkitaDropDownReceiver(view, context, runtimePlugin);
        AtakBroadcast.DocumentedIntentFilter dropDownFilter = new AtakBroadcast.DocumentedIntentFilter();
        dropDownFilter.addAction(AkitaDropDownReceiver.SHOW_AKITA_MESHTAK,
                "Show the Akita MeshTAK dashboard drop-down.");
        registerDropDownReceiver(dropDownReceiver, dropDownFilter);

        akitaTool = new AkitaTool(context);

        ToolsPreferenceFragment.register(
                new ToolsPreferenceFragment.ToolPreference(
                        context.getString(R.string.app_name),
                        context.getString(R.string.app_desc),
                        TOOL_PREFERENCE_KEY,
                        context.getDrawable(android.R.drawable.ic_menu_manage),
                        new AkitaPreferenceFragment(context)));
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        detachPersistentViews();
        if (dropDownReceiver != null) {
            dropDownReceiver.dispose();
            dropDownReceiver = null;
        }
        if (akitaTool != null) {
            akitaTool.dispose();
            akitaTool = null;
        }
        ToolsPreferenceFragment.unregister(TOOL_PREFERENCE_KEY);
        runtimePlugin.onDestroy();
        super.onDestroyImpl(context, view);
    }

    AkitaMeshTAKPlugin getRuntimePlugin() {
        return runtimePlugin;
    }

    private void attachPersistentViews(MapView view) {
        ViewGroup parent = view.getParent() instanceof ViewGroup ? (ViewGroup) view.getParent() : null;
        if (parent == null) {
            return;
        }

        addIfMissing(parent, runtimePlugin.getConnectionStatusOverlay(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addIfMissing(parent, runtimePlugin.getMissionMapOverlay(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addIfMissing(parent, runtimePlugin.getToolbar(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void detachPersistentViews() {
        removeFromParent(runtimePlugin.getToolbar());
        removeFromParent(runtimePlugin.getConnectionStatusOverlay());
        removeFromParent(runtimePlugin.getMissionMapOverlay());
    }

    private void addIfMissing(ViewGroup parent, View child, ViewGroup.LayoutParams layoutParams) {
        if (child == null || child.getParent() == parent) {
            return;
        }
        removeFromParent(child);
        parent.addView(child, layoutParams);
    }

    private void removeFromParent(View child) {
        if (child != null && child.getParent() instanceof ViewGroup) {
            ((ViewGroup) child.getParent()).removeView(child);
        }
    }
}