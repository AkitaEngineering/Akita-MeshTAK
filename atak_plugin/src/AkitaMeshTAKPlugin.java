// File: atak_plugin/src/AkitaMeshTAKPlugin.java
// Description: Main plugin lifecycle manager, binding services and initializing UI components.
package com.akitaengineering.meshtak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.plugin.AbstractPlugin;
import com.atakmap.android.plugin.PluginLayoutInflater;
import com.atakmap.android.plugin.ui.PluginContextMenu;
import com.atakmap.android.plugin.ui.PluginMapOverlay;
import com.atakmap.android.plugin.ui.PluginPreferenceFragment;
import com.atakmap.android.plugin.ui.PluginToolbar;
import com.atakmap.android.plugin.ui.PluginView;
import com.akitaengineering.meshtak.services.BLEService;
import com.akitaengineering.meshtak.services.SerialService;
import com.akitaengineering.meshtak.ui.AkitaToolbar;
import com.akitaengineering.meshtak.ui.ConnectionStatusOverlay;
import com.akitaengineering.meshtak.ui.SendDataView;
import com.akitaengineering.meshtak.ui.SettingsFragment;

import java.util.ArrayList;
import java.util.List;

public class AkitaMeshTAKPlugin extends AbstractPlugin {

    private static final String TAG = "AkitaMeshTAKPlugin";
    private Context pluginContext;
    private MapView mapView;
    private BLEService bleService;
    private SerialService serialService;
    private AkitaToolbar akitaToolbar;
    private ConnectionStatusOverlay connectionStatusOverlay;
    private PluginView sendDataPluginView;

    private final ServiceConnection bleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            bleService = binder.getService();
            bleService.setMapView(mapView);
            // Pass the toolbar instance so the service can update the UI
            bleService.setAkitaToolbar(akitaToolbar); 
            // Register for status updates
            bleService.setBleStatusListener(bleStatusListener); 
            
            // Pass services to the toolbar now that they are bound
            if (akitaToolbar != null) akitaToolbar.setServices(bleService, serialService);
            Log.i(TAG, "BLE Service bound and configured.");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bleService = null;
            Log.i(TAG, "BLE Service unbound.");
            if (connectionStatusOverlay != null) {
                connectionStatusOverlay.setBleStatus("Disconnected");
            }
        }
    };

    private final ServiceConnection serialConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SerialService.LocalBinder binder = (SerialService.LocalBinder) service;
            serialService = binder.getService();
            serialService.setMapView(mapView);
            // Pass the toolbar instance so the service can update the UI
            serialService.setAkitaToolbar(akitaToolbar); 
             // Register for status updates
            serialService.setSerialStatusListener(serialStatusListener);
            
            // Pass services to the toolbar now that they are bound
            if (akitaToolbar != null) akitaToolbar.setServices(bleService, serialService);
            Log.i(TAG, "Serial Service bound and configured.");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serialService = null;
            Log.i(TAG, "Serial Service unbound.");
            if (connectionStatusOverlay != null) {
                connectionStatusOverlay.setSerialStatus("Disconnected");
            }
        }
    };

    private final BLEService.BleStatusListener bleStatusListener = (status) -> {
        if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus(status);
        if (connectionStatusOverlay != null) connectionStatusOverlay.setBleStatus(status);
    };

    private final SerialService.SerialStatusListener serialStatusListener = (status) -> {
        if (akitaToolbar != null) akitaToolbar.setDetailedSerialStatus(status);
        if (connectionStatusOverlay != null) connectionStatusOverlay.setSerialStatus(status);
    };

    @Override
    public void onCreate(Context context, MapView view) {
        super.onCreate(context, view);
        this.pluginContext = context;
        this.mapView = view;
        Log.d(TAG, "Plugin created.");

        // Create UI elements before starting services so they can be referenced immediately
        akitaToolbar = new AkitaToolbar(context);
        connectionStatusOverlay = new ConnectionStatusOverlay(context, view);

        // Start and Bind BLE Service
        Intent bleServiceIntent = new Intent(pluginContext, BLEService.class);
        pluginContext.startService(bleServiceIntent);
        pluginContext.bindService(bleServiceIntent, bleConnection, Context.BIND_AUTO_CREATE);

        // Start and Bind Serial Service
        Intent serialServiceIntent = new Intent(pluginContext, SerialService.class);
        pluginContext.startService(serialServiceIntent);
        pluginContext.bindService(serialServiceIntent, serialConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Plugin destroyed. Stopping services and unbinding.");
        
        // Stop and Unbind services
        if (bleService != null) pluginContext.unbindService(bleConnection);
        Intent bleServiceIntent = new Intent(pluginContext, BLEService.class);
        pluginContext.stopService(bleServiceIntent);

        if (serialService != null) pluginContext.unbindService(serialConnection);
        Intent serialServiceIntent = new Intent(pluginContext, SerialService.class);
        pluginContext.stopService(serialServiceIntent);

        super.onDestroy();
    }

    // --- Plugin Interface Methods ---

    @Override
    public List<PluginMapOverlay> getOverlays() {
        List<PluginMapOverlay> overlays = new ArrayList<>();
        overlays.add(connectionStatusOverlay);
        return overlays;
    }

    @Override
    public List<PluginToolbar> getToolbars() {
        List<PluginToolbar> toolbars = new ArrayList<>();
        toolbars.add(akitaToolbar);
        return toolbars;
    }

    @Override
    public PluginView onCreateView(String viewId, PluginLayoutInflater inflater) {
        if (viewId.equals("com.akitaengineering.meshtak.send_data_view")) {
            // Ensure services are passed, even if still binding (they will be updated later)
            return new SendDataView(pluginContext, mapView, bleService, serialService);
        }
        return null;
    }

    @Override
    public PluginPreferenceFragment getPreferenceFragment() {
        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setMapView(mapView);
        return settingsFragment;
    }

    // Unused overrides for completeness
    @Override public PluginLayoutInflater getLayoutInflater(PluginLayoutInflater parent) { return null; }
    @Override public void onReceive(Context context, Intent intent) {}
    @Override public PluginContextMenu getContextMenu(Object caller) { return null; }
    @Override public void onUnbind(Intent intent) { super.onUnbind(intent); }
}
