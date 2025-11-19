// File: atak_plugin/src/ui/SettingsFragment.java
// Description: Fragment for handling plugin settings and applying changes to services.
package com.akitaengineering.meshtak.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.atakmap.android.maps.MapView;
import com.akitaengineering.meshtak.R;
import com.akitaengineering.meshtak.services.BLEService;
import com.akitaengineering.meshtak.services.SerialService;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";
    private MapView mapView;
    private BLEService bleService;
    private SerialService serialService;

    // --- Service Connection Handlers ---

    private final ServiceConnection bleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            bleService = binder.getService();
            bleService.setMapView(mapView);
            Log.i(TAG, "SettingsFragment bound to BLE Service");
            // Apply current preferences once service is available
            loadAndApplyPreferences();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bleService = null;
            Log.i(TAG, "SettingsFragment unbound from BLE Service");
        }
    };

    private final ServiceConnection serialConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SerialService.LocalBinder binder = (SerialService.LocalBinder) service;
            serialService = binder.getService();
            serialService.setMapView(mapView);
            Log.i(TAG, "SettingsFragment bound to Serial Service");
            // Apply current preferences once service is available
            loadAndApplyPreferences();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serialService = null;
            Log.i(TAG, "SettingsFragment unbound from Serial Service");
        }
    };

    public void setMapView(MapView view) {
        this.mapView = view;
    }

    // --- Lifecycle and Preference Initialization ---

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        ListPreference connectionMethodPref = findPreference("connection_method");
        EditTextPreference bleDeviceNamePref = findPreference("ble_device_name");
        EditTextPreference serialPortPathPref = findPreference("serial_port_path");
        EditTextPreference serialBaudRatePref = findPreference("serial_baud_rate");
        Preference sendTestMessagePref = findPreference("send_test_message");

        // Set listeners for preference changes
        if (connectionMethodPref != null) connectionMethodPref.setOnPreferenceChangeListener(this);
        if (bleDeviceNamePref != null) bleDeviceNamePref.setOnPreferenceChangeListener(this);
        if (serialPortPathPref != null) serialPortPathPref.setOnPreferenceChangeListener(this);
        if (serialBaudRatePref != null) serialBaudRatePref.setOnPreferenceChangeListener(this);
         if (sendTestMessagePref != null) {
            sendTestMessagePref.setOnPreferenceClickListener(preference -> {
                sendTestMessageToDevice();
                return true;
            });
        }

        // Apply initial enablement based on default value
        ListPreference connPref = findPreference("connection_method");
        EditTextPreference blePref = findPreference("ble_device_name");
        EditTextPreference serialPathPref = findPreference("serial_port_path");
        EditTextPreference serialBaudPref = findPreference("serial_baud_rate");

        if (connPref != null) {
            String defaultMethod = connPref.getValue();
            if (blePref != null) blePref.setEnabled(defaultMethod.equals("ble"));
            if (serialPathPref != null) serialPathPref.setEnabled(defaultMethod.equals("serial"));
            if (serialBaudPref != null) serialBaudPref.setEnabled(defaultMethod.equals("serial"));
        }
    }

    // --- Service Binding and Unbinding ---

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null) {
            // Bind services to allow fragment to communicate changes
            getActivity().bindService(new Intent(getActivity(), BLEService.class), bleConnection, Context.BIND_AUTO_CREATE);
            getActivity().bindService(new Intent(getActivity(), SerialService.class), serialConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null) {
            // Unbind services
            if (bleService != null) getActivity().unbindService(bleConnection);
            if (serialService != null) getActivity().unbindService(serialConnection);
            bleService = null;
            serialService = null;
        }
    }

    // --- Custom Logic ---

     private void loadAndApplyPreferences() {
        if (getActivity() != null) {
            // Load BLE device name preference
            String bleDeviceName = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString("ble_device_name", "AkitaNode01");
            
            // Pass settings to the bound services
            if (bleService != null) {
                // This call triggers the service to start scanning/connecting with the right name
                bleService.setTargetDeviceName(bleDeviceName);
            }
            // SerialService loads its baud rate internally upon connection attempt
        }
    }
    
    private void sendTestMessageToDevice() {
        String testMessage = "ATAK Test Message!";
        String connectionMethod = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString("connection_method", "ble");

        if (connectionMethod.equals("ble") && bleService != null) {
            bleService.sendData(testMessage.getBytes());
            Toast.makeText(getActivity(), "Sent via BLE: " + testMessage, Toast.LENGTH_SHORT).show();
        } else if (connectionMethod.equals("serial") && serialService != null) {
            serialService.sendData(testMessage.getBytes());
            Toast.makeText(getActivity(), "Sent via Serial: " + testMessage, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Not connected or connection method not selected.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Preference Change Listener ---

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key.equals("connection_method")) {
            String selectedMethod = (String) newValue;
            EditTextPreference blePref = findPreference("ble_device_name");
            EditTextPreference serialPathPref = findPreference("serial_port_path");
            EditTextPreference serialBaudPref = findPreference("serial_baud_rate");

            // Enable/disable UI elements based on selected method
            if (blePref != null) blePref.setEnabled(selectedMethod.equals("ble"));
            if (serialPathPref != null) serialPathPref.setEnabled(selectedMethod.equals("serial"));
            if (serialBaudPref != null) serialBaudPref.setEnabled(selectedMethod.equals("serial"));
        } else if (key.equals("ble_device_name") && bleService != null) {
            // Update BLE service immediately when name preference changes
            bleService.setTargetDeviceName((String) newValue);
        }
        return true;
    }
}
