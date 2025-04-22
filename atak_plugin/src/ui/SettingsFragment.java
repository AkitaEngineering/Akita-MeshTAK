package com.akitaengineering.meshtak.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.atakmap.android.maps.MapView;
import com.akitaengineering.meshtak.R;
import com.akitaengineering.meshtak.services.BLEService;
import com.akitaengineering.meshtak.services.SerialService;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";
    private MapView mapView;
    private BLEService bleService;
    private SerialService serialService;

    private final ServiceConnection bleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            bleService = binder.getService();
            bleService.setMapView(mapView);
            Log.i(TAG, "SettingsFragment bound to BLE Service");
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

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        ListPreference connectionMethodPref = findPreference("connection_method");
        EditTextPreference bleDeviceNamePref = findPreference("ble_device_name");
        EditTextPreference serialPortPathPref = findPreference("serial_port_path");
        EditTextPreference serialBaudRatePref = findPreference("serial_baud_rate");
        Preference sendTestMessagePref = findPreference("send_test_message");

        if (connectionMethodPref != null) {
            connectionMethodPref.setOnPreferenceChangeListener(this);
        }
        if (bleDeviceNamePref != null) {
            bleDeviceNamePref.setOnPreferenceChangeListener(this);
        }
        if (serialPortPathPref != null) {
            serialPortPathPref.setOnPreferenceChangeListener(this);
        }
        if (serialBaudRatePref != null) {
            serialBaudRatePref.setOnPreferenceChangeListener(this);
        }
         if (sendTestMessagePref != null) {
            sendTestMessagePref.setOnPreferenceClickListener(preference -> {
                sendTestMessageToDevice();
                return true;
            });
        }

        //  Set initial enablement based on default value
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

     private void loadAndApplyPreferences() {
        if (getActivity() != null) {
            String connectionMethod = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString("connection_method", "ble");
            String bleDeviceName = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString("ble_device_name", "AkitaNode01");
            //  String serialPortPath = ...; // Services handle their own preference loading now
            //  String serialBaudRate = ...;

            if (bleService != null) {
                bleService.setTargetDeviceName(bleDeviceName);
            }
            // SerialService loads its preferences directly
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActiv
