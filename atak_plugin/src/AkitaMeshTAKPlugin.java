// File: atak_plugin/src/AkitaMeshTAKPlugin.java
// Description: Main plugin lifecycle manager, binding services and initializing UI components.
package com.akitaengineering.meshtak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.preference.PreferenceManager;

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

public class AkitaMeshTAKPlugin extends AbstractPlugin implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "AkitaMeshTAKPlugin";
    private Context pluginContext;
    private MapView mapView;
    private BLEService bleService;
    private SerialService serialService;
    private AkitaToolbar akitaToolbar;
    private ConnectionStatusOverlay connectionStatusOverlay;
    private PluginView sendDataPluginView;

    // --- Service Connection Handlers ---

    private final ServiceConnection bleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            bleService = binder.getService();
            bleService.setMapView(mapView);
            bleService.setAkitaToolbar(akitaToolbar); 
            bleService.setBleStatusListener(bleStatusListener); 
            
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
            serialService.setAkitaToolbar(akitaToolbar); 
            serialService.setSerialStatusListener(serialStatusListener);
            
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

    // --- Plugin Lifecycle ---
    
    @Override
    public void onCreate(Context context, MapView view) {
        super.onCreate(context, view);
        this.pluginContext = context;
        this.mapView = view;
        Log.d(TAG, "Plugin created.");

        // Register for preference changes globally to handle connection method swaps
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this);
        
        akitaToolbar = new AkitaToolbar(context);
        connectionStatusOverlay = new ConnectionStatusOverlay(context, view);
        
        // Start services on startup
        startAndBindServices();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Plugin destroyed. Stopping services and unbinding.");
        
        PreferenceManager.getDefaultSharedPreferences(pluginContext)
                .unregisterOnSharedPreferenceChangeListener(this);
        
        stopAndUnbindServices();
        super.onDestroy();
    }
    
    /** Starts/Binds both services */
    private void startAndBindServices() {
        Intent bleServiceIntent = new Intent(pluginContext, BLEService.class);
        pluginContext.startService(bleServiceIntent);
        pluginContext.bindService(bleServiceIntent, bleConnection, Context.BIND_AUTO_CREATE);

        Intent serialServiceIntent = new Intent(pluginContext, SerialService.class);
        pluginContext.startService(serialServiceIntent);
        pluginContext.bindService(serialServiceIntent, serialConnection, Context.BIND_AUTO_CREATE);
    }
    
    /** Stops/Unbinds both services */
    private void stopAndUnbindServices() {
        if (bleService != null) pluginContext.unbindService(bleConnection);
        pluginContext.stopService(new Intent(pluginContext, BLEService.class));

        if (serialService != null) pluginContext.unbindService(serialConnection);
        pluginContext.stopService(new Intent(pluginContext, SerialService.class));
        
        bleService = null;
        serialService = null;
    }

    /** Reloads the entire plugin connection state (used when Connection Method is changed) */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("connection_method")) {
            Log.i(TAG, "Connection method preference changed. Reloading connection strategy.");
            
            // 1. Unbind/Stop everything cleanly
            stopAndUnbindServices();
            
            // 2. Restart services to force new connection attempts with new settings
            startAndBindServices();
            
            // 3. Update the toolbar display instantly
            if (akitaToolbar != null) akitaToolbar.updateConnectionMethodDisplay();
        }
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
