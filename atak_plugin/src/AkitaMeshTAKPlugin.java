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
import com.akitaengineering.meshtak.AuditLogger;
import com.akitaengineering.meshtak.services.BLEService;
import com.akitaengineering.meshtak.services.SerialService;
import com.akitaengineering.meshtak.ui.AkitaMockSettings;
import com.akitaengineering.meshtak.ui.AkitaMissionProfile;
import com.akitaengineering.meshtak.ui.AkitaProvisioningManager;
import com.akitaengineering.meshtak.ui.AkitaToolbar;
import com.akitaengineering.meshtak.ui.AkitaTheme;
import com.akitaengineering.meshtak.ui.ConnectionStatusOverlay;
import com.akitaengineering.meshtak.ui.MissionMapOverlay;
import com.akitaengineering.meshtak.ui.SendDataView;
import com.akitaengineering.meshtak.ui.SettingsFragment;

public class AkitaMeshTAKPlugin implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "AkitaMeshTAKPlugin";
    private Context pluginContext;
    private MapView mapView;
    private BLEService bleService;
    private SerialService serialService;
    private AkitaToolbar akitaToolbar;
    private ConnectionStatusOverlay connectionStatusOverlay;
    private MissionMapOverlay missionMapOverlay;
    private SendDataView sendDataView;
    private boolean bleBound;
    private boolean serialBound;

    // --- Service Connection Handlers ---

    private final ServiceConnection bleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            bleBound = true;
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            bleService = binder.getService();
            bleService.setMapView(mapView);
            bleService.setAkitaToolbar(akitaToolbar);
            bleService.setBleStatusListener(bleStatusListener);

            if (akitaToolbar != null) akitaToolbar.setServices(bleService, serialService);
            if (sendDataView != null) sendDataView.setServices(bleService, serialService);
            if (missionMapOverlay != null) missionMapOverlay.setBleStatus(bleService.getConnectionStatus());
            Log.i(TAG, "BLE Service bound and configured.");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bleBound = false;
            bleService = null;
            Log.i(TAG, "BLE Service unbound.");
            if (sendDataView != null) sendDataView.setServices(null, serialService);
            if (connectionStatusOverlay != null) {
                connectionStatusOverlay.setBleStatus("Disconnected");
            }
            if (missionMapOverlay != null) {
                missionMapOverlay.setBleStatus("Disconnected");
            }
        }
    };

    private final ServiceConnection serialConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            serialBound = true;
            SerialService.LocalBinder binder = (SerialService.LocalBinder) service;
            serialService = binder.getService();
            serialService.setMapView(mapView);
            serialService.setAkitaToolbar(akitaToolbar);
            serialService.setSerialStatusListener(serialStatusListener);

            if (akitaToolbar != null) akitaToolbar.setServices(bleService, serialService);
            if (sendDataView != null) sendDataView.setServices(bleService, serialService);
            if (missionMapOverlay != null) missionMapOverlay.setSerialStatus(serialService.getConnectionStatus());
            Log.i(TAG, "Serial Service bound and configured.");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serialBound = false;
            serialService = null;
            Log.i(TAG, "Serial Service unbound.");
            if (sendDataView != null) sendDataView.setServices(bleService, null);
            if (connectionStatusOverlay != null) {
                connectionStatusOverlay.setSerialStatus("Disconnected");
            }
            if (missionMapOverlay != null) {
                missionMapOverlay.setSerialStatus("Disconnected");
            }
        }
    };

    private final BLEService.BleStatusListener bleStatusListener = (status) -> {
        if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus(status);
        if (connectionStatusOverlay != null) connectionStatusOverlay.setBleStatus(status);
        if (missionMapOverlay != null) missionMapOverlay.setBleStatus(status);
    };

    private final SerialService.SerialStatusListener serialStatusListener = (status) -> {
        if (akitaToolbar != null) akitaToolbar.setDetailedSerialStatus(status);
        if (connectionStatusOverlay != null) connectionStatusOverlay.setSerialStatus(status);
        if (missionMapOverlay != null) missionMapOverlay.setSerialStatus(status);
    };

    // --- Plugin Lifecycle ---

    public void onCreate(Context context, MapView view) {
        this.pluginContext = context;
        this.mapView = view;
        Log.d(TAG, "Plugin created.");

        // Register for preference changes globally to handle connection method swaps
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this);

        AuditLogger.getInstance().initialize(context.getApplicationContext());

        akitaToolbar = new AkitaToolbar(context);
        connectionStatusOverlay = new ConnectionStatusOverlay(context, view);
        missionMapOverlay = new MissionMapOverlay(context, view);

        if (isMockModeEnabled()) {
            applyMockState();
        } else {
            startAndBindServices();
        }
    }

    public void onDestroy() {
        Log.d(TAG, "Plugin destroyed. Stopping services and unbinding.");

        PreferenceManager.getDefaultSharedPreferences(pluginContext)
                .unregisterOnSharedPreferenceChangeListener(this);

        stopAndUnbindServices();
    }

    /** Starts/Binds both services */
    private void startAndBindServices() {
        if (isMockModeEnabled()) {
            applyMockState();
            return;
        }
        Intent bleServiceIntent = new Intent(pluginContext, BLEService.class);
        pluginContext.startService(bleServiceIntent);
        if (!bleBound) bleBound = pluginContext.bindService(bleServiceIntent, bleConnection, Context.BIND_AUTO_CREATE);

        Intent serialServiceIntent = new Intent(pluginContext, SerialService.class);
        pluginContext.startService(serialServiceIntent);
        if (!serialBound) serialBound = pluginContext.bindService(serialServiceIntent, serialConnection, Context.BIND_AUTO_CREATE);
    }

    /** Stops/Unbinds both services */
    private void stopAndUnbindServices() {
        if (bleBound) {
            pluginContext.unbindService(bleConnection);
            bleBound = false;
        }
        pluginContext.stopService(new Intent(pluginContext, BLEService.class));

        if (serialBound) {
            pluginContext.unbindService(serialConnection);
            serialBound = false;
        }
        pluginContext.stopService(new Intent(pluginContext, SerialService.class));

        bleService = null;
        serialService = null;
        if (akitaToolbar != null) akitaToolbar.setServices(null, null);
        if (sendDataView != null) sendDataView.setServices(null, null);
    }

    private boolean isMockModeEnabled() {
        return AkitaMockSettings.isEnabled(PreferenceManager.getDefaultSharedPreferences(pluginContext));
    }

    private void applyMockState() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(pluginContext);
        String bleStatus = AkitaMockSettings.getBleStatus(preferences);
        String serialStatus = AkitaMockSettings.getSerialStatus(preferences);
        String batteryStatus = AkitaMockSettings.getBatteryLabel(preferences);

        if (akitaToolbar != null) {
            akitaToolbar.setServices(null, null);
            akitaToolbar.updateConnectionMethodDisplay();
            akitaToolbar.setDetailedBleStatus(bleStatus);
            akitaToolbar.setDetailedSerialStatus(serialStatus);
            akitaToolbar.setBatteryStatus(batteryStatus);
        }
        if (connectionStatusOverlay != null) {
            connectionStatusOverlay.setBleStatus(bleStatus);
            connectionStatusOverlay.setSerialStatus(serialStatus);
        }
        if (missionMapOverlay != null) {
            missionMapOverlay.setBleStatus(bleStatus);
            missionMapOverlay.setSerialStatus(serialStatus);
        }
        if (sendDataView != null) {
            sendDataView.setServices(null, null);
        }
        if (mapView != null) {
            mapView.invalidate();
        }
    }

    /** Reloads the entire plugin connection state (used when Connection Method is changed) */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (AkitaMockSettings.PREF_MOCK_MODE.equals(key)) {
            if (isMockModeEnabled()) {
                stopAndUnbindServices();
                applyMockState();
            } else {
                startAndBindServices();
            }
            return;
        }

        if (AkitaMockSettings.PREF_MOCK_BLE_STATUS.equals(key)
                || AkitaMockSettings.PREF_MOCK_SERIAL_STATUS.equals(key)
                || AkitaMockSettings.PREF_MOCK_BATTERY_LEVEL.equals(key)) {
            if (isMockModeEnabled()) {
                applyMockState();
            }
            return;
        }

        if (key.equals("connection_method")) {
            if (isMockModeEnabled()) {
                if (akitaToolbar != null) akitaToolbar.updateConnectionMethodDisplay();
                applyMockState();
                return;
            }
            Log.i(TAG, "Connection method preference changed. Reloading connection strategy.");

            // 1. Unbind/Stop everything cleanly
            stopAndUnbindServices();

            // 2. Restart services to force new connection attempts with new settings
            startAndBindServices();

            // 3. Update the toolbar display instantly
            if (akitaToolbar != null) akitaToolbar.updateConnectionMethodDisplay();
        } else if (AkitaProvisioningManager.PREF_PROVISIONING_SECRET_SIGNAL.equals(key)
                || AkitaProvisioningManager.PREF_ENCRYPTION_ENABLED.equals(key)) {
            if (bleService != null) {
                bleService.reloadSecurityConfiguration();
            }
            if (serialService != null) {
                serialService.reloadSecurityConfiguration();
            }
            if (mapView != null) {
                mapView.invalidate();
            }
        } else if ("ble_device_name".equals(key)) {
            String deviceName = sharedPreferences.getString("ble_device_name", "AkitaNode01");
            if (bleService != null) bleService.setTargetDeviceName(deviceName);
            if (serialService != null) serialService.reloadSecurityConfiguration();
        } else if ((AkitaTheme.PREF_UI_THEME.equals(key)
                || AkitaMissionProfile.PREF_MISSION_PROFILE.equals(key)) && mapView != null) {
            mapView.invalidate();
            if (isMockModeEnabled()) {
                applyMockState();
            }
        }
    }

    public AkitaToolbar getToolbar() {
        return akitaToolbar;
    }

    public ConnectionStatusOverlay getConnectionStatusOverlay() {
        return connectionStatusOverlay;
    }

    public MissionMapOverlay getMissionMapOverlay() {
        return missionMapOverlay;
    }

    public SendDataView getSendDataView() {
        if (sendDataView == null && pluginContext != null && mapView != null) {
            sendDataView = new SendDataView(pluginContext, mapView, bleService, serialService);
        }
        return sendDataView;
    }
}
