// File: atak_plugin/src/ui/SettingsFragment.java
// Description: Fragment for handling plugin settings and applying changes to services.
package com.akitaengineering.meshtak.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.plugin.ui.PluginPreferenceFragment;
import com.akitaengineering.meshtak.AkitaMissionControl;
import com.akitaengineering.meshtak.AuditLogger;
import com.akitaengineering.meshtak.R;
import com.akitaengineering.meshtak.services.BLEService;
import com.akitaengineering.meshtak.services.SerialService;
import com.akitaengineering.meshtak.ui.AkitaTheme;

public class SettingsFragment extends PreferenceFragmentCompat implements PluginPreferenceFragment, Preference.OnPreferenceChangeListener, android.content.SharedPreferences.OnSharedPreferenceChangeListener {

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
        ListPreference uiThemePref = findPreference(AkitaTheme.PREF_UI_THEME);
        ListPreference missionProfilePref = findPreference(AkitaMissionProfile.PREF_MISSION_PROFILE);
        SwitchPreferenceCompat autoFailoverPref = findPreference(AkitaMissionControl.PREF_AUTO_FAILOVER);
        SwitchPreferenceCompat encryptionEnabledPref = findPreference(AkitaProvisioningManager.PREF_ENCRYPTION_ENABLED);
        EditTextPreference provisioningSecretPref = findPreference(AkitaProvisioningManager.PREF_PROVISIONING_SECRET);
        Preference rotateSecretPref = findPreference("security_rotate_secret");
        EditTextPreference provisioningBundlePref = findPreference(AkitaProvisioningManager.PREF_PROVISIONING_BUNDLE);
        Preference generateBundlePref = findPreference("security_generate_bundle");
        Preference applyBundlePref = findPreference("security_apply_bundle");
        Preference stageDevicePref = findPreference("security_stage_device");
        Preference exportAuditPref = findPreference("security_export_audit");
        Preference reloadSecurityPref = findPreference("security_reload_state");
        Preference mockModePref = findPreference(AkitaMockSettings.PREF_MOCK_MODE);
        ListPreference mockBleStatusPref = findPreference(AkitaMockSettings.PREF_MOCK_BLE_STATUS);
        ListPreference mockSerialStatusPref = findPreference(AkitaMockSettings.PREF_MOCK_SERIAL_STATUS);
        EditTextPreference mockBatteryLevelPref = findPreference(AkitaMockSettings.PREF_MOCK_BATTERY_LEVEL);
        EditTextPreference bleDeviceNamePref = findPreference("ble_device_name");
        EditTextPreference serialPortPathPref = findPreference("serial_port_path");
        EditTextPreference serialBaudRatePref = findPreference("serial_baud_rate");
        Preference sendTestMessagePref = findPreference("send_test_message");

        // Set listeners for preference changes
        if (connectionMethodPref != null) connectionMethodPref.setOnPreferenceChangeListener(this);
        if (connectionMethodPref != null) connectionMethodPref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        if (uiThemePref != null) uiThemePref.setOnPreferenceChangeListener(this);
        if (uiThemePref != null) uiThemePref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        if (missionProfilePref != null) missionProfilePref.setOnPreferenceChangeListener(this);
        if (missionProfilePref != null) missionProfilePref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        if (autoFailoverPref != null) autoFailoverPref.setOnPreferenceChangeListener(this);
        if (encryptionEnabledPref != null) encryptionEnabledPref.setOnPreferenceChangeListener(this);
        if (provisioningSecretPref != null) provisioningSecretPref.setOnPreferenceChangeListener(this);
        if (provisioningSecretPref != null) provisioningSecretPref.setText("");
        if (provisioningSecretPref != null) provisioningSecretPref.setSummary(AkitaProvisioningManager.getProvisioningSummary(requireContext()));
        if (provisioningSecretPref != null) provisioningSecretPref.setOnBindEditTextListener(editText ->
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        if (provisioningBundlePref != null) provisioningBundlePref.setOnPreferenceChangeListener(this);
        if (provisioningBundlePref != null) provisioningBundlePref.setText("");
        if (provisioningBundlePref != null) provisioningBundlePref.setSummary(AkitaProvisioningManager.getProvisioningBundleSummary(requireContext()));
        if (mockModePref != null) mockModePref.setOnPreferenceChangeListener(this);
        if (mockBleStatusPref != null) mockBleStatusPref.setOnPreferenceChangeListener(this);
        if (mockBleStatusPref != null) mockBleStatusPref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        if (mockSerialStatusPref != null) mockSerialStatusPref.setOnPreferenceChangeListener(this);
        if (mockSerialStatusPref != null) mockSerialStatusPref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        if (mockBatteryLevelPref != null) mockBatteryLevelPref.setOnPreferenceChangeListener(this);
        if (mockBatteryLevelPref != null) mockBatteryLevelPref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        if (mockBatteryLevelPref != null) mockBatteryLevelPref.setOnBindEditTextListener(editText ->
            editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        if (bleDeviceNamePref != null) bleDeviceNamePref.setOnPreferenceChangeListener(this);
        if (serialPortPathPref != null) serialPortPathPref.setOnPreferenceChangeListener(this);
        if (serialBaudRatePref != null) serialBaudRatePref.setOnPreferenceChangeListener(this);
         if (sendTestMessagePref != null) {
            sendTestMessagePref.setOnPreferenceClickListener(preference -> {
                sendTestMessageToDevice();
                return true;
            });
        }
        if (rotateSecretPref != null) {
            rotateSecretPref.setSummary(AkitaProvisioningManager.getRotationSummary(requireContext()));
            rotateSecretPref.setOnPreferenceClickListener(preference -> {
                String rotatedSecret = AkitaProvisioningManager.rotateProvisioningSecret(requireContext());
                refreshProvisioningPreferenceSummaries();
                reloadBoundServiceSecurity();
                Toast.makeText(getActivity(), "Provisioning secret rotated: " + AkitaProvisioningManager.maskSecret(rotatedSecret), Toast.LENGTH_SHORT).show();
                return true;
            });
        }
        if (generateBundlePref != null) {
            generateBundlePref.setOnPreferenceClickListener(preference -> {
                String deviceAlias = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString("ble_device_name", "AkitaNode01");
                AkitaProvisioningManager.createProvisioningBundle(requireContext(), deviceAlias);
                refreshProvisioningPreferenceSummaries();
                Toast.makeText(getActivity(), "Provisioning bundle refreshed for " + deviceAlias + ".", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
        if (applyBundlePref != null) {
            applyBundlePref.setOnPreferenceClickListener(preference -> {
                try {
                    AkitaProvisioningManager.ProvisioningBundle bundle = AkitaProvisioningManager.applyProvisioningBundle(requireContext());
                    refreshProvisioningPreferenceSummaries();
                    reloadBoundServiceSecurity();
                    Toast.makeText(getActivity(), "Provisioning bundle applied for " + bundle.deviceAlias + ".", Toast.LENGTH_SHORT).show();
                } catch (IllegalArgumentException exception) {
                    Toast.makeText(getActivity(), exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
        if (stageDevicePref != null) {
            stageDevicePref.setOnPreferenceClickListener(preference -> {
                stageProvisioningToDevice();
                return true;
            });
        }
        if (exportAuditPref != null) {
            exportAuditPref.setOnPreferenceClickListener(preference -> {
                String exportPath = AuditLogger.getInstance().exportToFile();
                Toast.makeText(getActivity(), exportPath == null ? "Audit export failed." : "Audit exported to " + exportPath, Toast.LENGTH_LONG).show();
                return true;
            });
        }
        if (reloadSecurityPref != null) {
            reloadSecurityPref.setOnPreferenceClickListener(preference -> {
                reloadBoundServiceSecurity();
                Toast.makeText(getActivity(), "Security state reloaded.", Toast.LENGTH_SHORT).show();
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
        if (getPreferenceManager() != null && getPreferenceManager().getSharedPreferences() != null) {
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
        if (getActivity() != null && AkitaMockSettings.isEnabled(PreferenceManager.getDefaultSharedPreferences(getActivity()))) {
            return;
        }
        if (getActivity() != null) {
            // Bind services to allow fragment to communicate changes
            getActivity().bindService(new Intent(getActivity(), BLEService.class), bleConnection, Context.BIND_AUTO_CREATE);
            getActivity().bindService(new Intent(getActivity(), SerialService.class), serialConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getPreferenceManager() != null && getPreferenceManager().getSharedPreferences() != null) {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
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
            if (AkitaMockSettings.isEnabled(PreferenceManager.getDefaultSharedPreferences(getActivity()))) {
                return;
            }
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
        if (getActivity() == null) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String connectionMethod = preferences.getString("connection_method", "ble");
        AkitaMissionControl missionControl = AkitaMissionControl.getInstance(requireContext());
        missionControl.queueMessage("Plain Text", testMessage, connectionMethod);
        AkitaMissionControl.DispatchBatchResult result = missionControl.dispatchPendingMessages(
                buildRouteSender(AkitaMockSettings.isEnabled(preferences)),
                AkitaMissionControl.isAutoFailoverEnabled(preferences),
                AkitaMockSettings.isEnabled(preferences));
        Toast.makeText(getActivity(), result.summary, Toast.LENGTH_SHORT).show();
    }

    private void refreshProvisioningPreferenceSummaries() {
        EditTextPreference provisioningSecretPref = findPreference(AkitaProvisioningManager.PREF_PROVISIONING_SECRET);
        EditTextPreference provisioningBundlePref = findPreference(AkitaProvisioningManager.PREF_PROVISIONING_BUNDLE);
        Preference rotateSecretPref = findPreference("security_rotate_secret");
        Preference generateBundlePref = findPreference("security_generate_bundle");
        Preference applyBundlePref = findPreference("security_apply_bundle");
        Preference stageDevicePref = findPreference("security_stage_device");
        if (provisioningSecretPref != null && getContext() != null) {
            provisioningSecretPref.setSummary(AkitaProvisioningManager.getProvisioningSummary(getContext()));
        }
        if (provisioningBundlePref != null && getContext() != null) {
            provisioningBundlePref.setSummary(AkitaProvisioningManager.getProvisioningBundleSummary(getContext()));
        }
        if (rotateSecretPref != null && getContext() != null) {
            rotateSecretPref.setSummary(AkitaProvisioningManager.getRotationSummary(getContext()));
        }
        if (generateBundlePref != null) {
            generateBundlePref.setSummary("Refresh an offline provisioning bundle from the active plugin secret.");
        }
        if (applyBundlePref != null) {
            applyBundlePref.setSummary("Apply the staged air-gapped bundle to the plugin security profile.");
        }
        if (stageDevicePref != null) {
            stageDevicePref.setSummary("Send the staged or active secret to the connected device in plaintext for runtime reprovisioning.");
        }
    }

    private void reloadBoundServiceSecurity() {
        if (bleService != null) {
            bleService.reloadSecurityConfiguration();
        }
        if (serialService != null) {
            serialService.reloadSecurityConfiguration();
        }
    }

    @Override
    public void onSharedPreferenceChanged(android.content.SharedPreferences sharedPreferences, String key) {
        if (AkitaProvisioningManager.PREF_PROVISIONING_SECRET_SIGNAL.equals(key)
                || AkitaProvisioningManager.PREF_ENCRYPTION_ENABLED.equals(key)
                || AkitaProvisioningManager.PREF_PROVISIONING_BUNDLE_SIGNAL.equals(key)) {
            refreshProvisioningPreferenceSummaries();
            if (AkitaProvisioningManager.PREF_PROVISIONING_SECRET_SIGNAL.equals(key)
                    || AkitaProvisioningManager.PREF_ENCRYPTION_ENABLED.equals(key)) {
                new Handler(Looper.getMainLooper()).post(this::reloadBoundServiceSecurity);
            }
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
        } else if (AkitaProvisioningManager.PREF_PROVISIONING_SECRET.equals(key)) {
            String newSecret = String.valueOf(newValue).trim();
            if (!newSecret.isEmpty() && newSecret.length() < 12) {
                Toast.makeText(getActivity(), "Provisioning secret must be at least 12 characters or left blank to use the build-time secret.", Toast.LENGTH_SHORT).show();
                return false;
            }
            AkitaProvisioningManager.setCustomProvisioningSecret(requireContext(), newSecret);
            if (preference instanceof EditTextPreference) {
                ((EditTextPreference) preference).setText("");
            }
            refreshProvisioningPreferenceSummaries();
            Toast.makeText(getActivity(), newSecret.isEmpty()
                    ? "Custom provisioning secret cleared."
                    : "Custom provisioning secret updated.", Toast.LENGTH_SHORT).show();
            return false;
        } else if (AkitaProvisioningManager.PREF_PROVISIONING_BUNDLE.equals(key)) {
            String bundle = String.valueOf(newValue).trim();
            if (!bundle.isEmpty()) {
                try {
                    AkitaProvisioningManager.previewProvisioningBundle(bundle);
                } catch (IllegalArgumentException exception) {
                    Toast.makeText(getActivity(), exception.getMessage(), Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            AkitaProvisioningManager.setStagedProvisioningBundle(requireContext(), bundle);
            if (preference instanceof EditTextPreference) {
                ((EditTextPreference) preference).setText("");
            }
            refreshProvisioningPreferenceSummaries();
            Toast.makeText(getActivity(), bundle.isEmpty()
                    ? "Staged provisioning bundle cleared."
                    : "Provisioning bundle staged securely.", Toast.LENGTH_SHORT).show();
            return false;
        } else if (AkitaMockSettings.PREF_MOCK_BATTERY_LEVEL.equals(key)) {
            try {
                int batteryLevel = Integer.parseInt(String.valueOf(newValue));
                if (batteryLevel < 0 || batteryLevel > 100) {
                    Toast.makeText(getActivity(), "Mock battery level must be between 0 and 100.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Mock battery level must be numeric.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (key.equals("ble_device_name") && bleService != null) {
            // Update BLE service immediately when name preference changes
            bleService.setTargetDeviceName((String) newValue);
        }
        return true;
    }

    private void stageProvisioningToDevice() {
        if (getActivity() == null) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean mockMode = AkitaMockSettings.isEnabled(preferences);
        String connectionMethod = preferences.getString("connection_method", "ble");
        String command;
        try {
            command = AkitaProvisioningManager.buildProvisioningStageCommand(requireContext());
        } catch (IllegalArgumentException exception) {
            Toast.makeText(getActivity(), exception.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (mockMode) {
            AkitaMissionControl.getInstance(requireContext()).recordProvisioningEvent(
                    "PROVISIONING_STAGED",
                    "Mock runtime provisioning staged over " + AkitaMissionControl.routeLabel(connectionMethod),
                    AkitaMissionControl.ROUTE_MOCK);
            Toast.makeText(getActivity(), "Simulated runtime provisioning staged.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = sendPlaintextToPreferredRoute(connectionMethod, command.getBytes());
        if (success) {
            AkitaMissionControl.getInstance(requireContext()).recordProvisioningEvent(
                    "PROVISIONING_STAGE_REQUESTED",
                    "Plaintext staging command sent over " + AkitaMissionControl.routeLabel(connectionMethod),
                    connectionMethod);
            Toast.makeText(getActivity(), "Provisioning stage command sent. Apply the bundle locally if needed.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "No connected bearer available for provisioning stage.", Toast.LENGTH_SHORT).show();
        }
    }

    private AkitaMissionControl.RouteSender buildRouteSender(boolean mockMode) {
        return new AkitaMissionControl.RouteSender() {
            @Override
            public boolean isRouteAvailable(String route) {
                if (mockMode) {
                    return true;
                }
                if (AkitaMissionControl.ROUTE_SERIAL.equalsIgnoreCase(route)) {
                    return serialService != null && serialService.isReadyForTraffic();
                }
                return bleService != null && bleService.isReadyForTraffic();
            }

            @Override
            public boolean send(String route, byte[] data) {
                if (mockMode) {
                    return true;
                }
                if (AkitaMissionControl.ROUTE_SERIAL.equalsIgnoreCase(route)) {
                    return serialService != null && serialService.sendData(data);
                }
                return bleService != null && bleService.sendData(data);
            }
        };
    }

    private boolean sendPlaintextToPreferredRoute(String preferredRoute, byte[] data) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean autoFailover = AkitaMissionControl.isAutoFailoverEnabled(preferences);
        if (AkitaMissionControl.ROUTE_SERIAL.equalsIgnoreCase(preferredRoute)) {
            if (serialService != null && serialService.isReadyForTraffic() && serialService.sendPlaintextData(data)) {
                return true;
            }
            return autoFailover && bleService != null && bleService.isReadyForTraffic() && bleService.sendPlaintextData(data);
        }
        if (bleService != null && bleService.isReadyForTraffic() && bleService.sendPlaintextData(data)) {
            return true;
        }
        return autoFailover && serialService != null && serialService.isReadyForTraffic() && serialService.sendPlaintextData(data);
    }
}
