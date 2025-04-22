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
import com.atakengineering.meshtak.services.BLEService;
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
            bleService.setBleStatusListener(bleStatusListener);
            bleService.setAkitaToolbar(akitaToolbar);
            Log.i(TAG, "BLE Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bleService = null;
            Log.i(TAG, "BLE Service disconnected");
        }
    };

    private final ServiceConnection serialConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SerialService.LocalBinder binder = (SerialService.LocalBinder) service;
            serialService = binder.getService();
            serialService.setMapView(mapView);
            serialService.setSerialStatusListener(serialStatusListener);
            serialService.setAkitaToolbar(akitaToolbar);
            Log.i(TAG, "Serial Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serialService = null;
            Log.i(TAG, "Serial Service disconnected");
        }
    };

    private BLEService.BleStatusListener bleStatusListener = new BLEService.BleStatusListener() {
        @Override
        public void onBleStatusChanged(final String status) {
            if (akitaToolbar != null) {
                akitaToolbar.setBleStatus(status);
            }
            if (connectionStatusOverlay != null) {
                connectionStatusOverlay.setBleStatus(status);
            }
        }
    };

    private SerialService.SerialStatusListener serialStatusListener = new SerialService.SerialStatusListener() {
        @Override
        public void onSerialStatusChanged(final String status) {
            if (akitaToolbar != null) {
                akitaToolbar.setSerialStatus(status);
            }
            if (connectionStatusOverlay != null) {
                connectionStatusOverlay.setSerialStatus(status);
            }
        }
    };

    @Override
    public void onCreate(Context context, MapView view) {
        super.onCreate(context, view);
        this.pluginContext = context;
        this.mapView = view;
        Log.d(TAG, "Plugin created.");

        //  Start and Bind Services
        Intent bleServiceIntent = new Intent(pluginContext, BLEService.class);
        pluginContext.startService(bleServiceIntent);
        pluginContext.bindService(bleServiceIntent, bleConnection, Context.BIND_AUTO_CREATE);

        Intent serialServiceIntent = new Intent(pluginContext, SerialService.class);
        pluginContext.startService(serialServiceIntent);
        pluginContext.bindService(serialServiceIntent, serialConnection, Context.BIND_AUTO_CREATE);

        //  Create UI elements
        akitaToolbar = new AkitaToolbar(context);
        connectionStatusOverlay = new ConnectionStatusOverlay(context, view);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Plugin destroyed.");
        //  Unbind services
        if (bleService != null) pluginContext.unbindService(bleConnection);
        Intent bleServiceIntent = new Intent(pluginContext, BLEService.class);
        pluginContext.stopService(bleServiceIntent);

        if (serialService != null) pluginContext.unbindService(serialConnection);
        Intent serialServiceIntent = new Intent(pluginContext, SerialService.class);
        pluginContext.stopService(serialServiceIntent);

        super.onDestroy();
    }

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
    public PluginLayoutInflater getLayoutInflater(PluginLayoutInflater parent) {
        return null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //  Handle intents
    }

    @Override
    public PluginContextMenu getContextMenu(Object caller) {
        return null;
    }

    @Override
    public PluginPreferenceFragment getPreferenceFragment() {
        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setMapView(mapView);
        return settingsFragment;
    }

    @Override
    public PluginView onCreateView(String viewId, PluginLayoutInflater inflater) {
        if (viewId.equals("com.akitaengineering.meshtak.send_data_view")) {
            sendDataPluginView = new SendDataView(pluginContext, mapView, bleService, serialService);
            return sendDataPluginView;
        }
        return null;
    }

    @Override
    public void onUnbind(Intent intent) {
        if (sendDataPluginView != null) {
            sendDataPluginView = null;
        }
    }
}
