package com.akitaengineering.meshtak.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.atakmap.android.preference.PluginPreferenceFragment;
import com.akitaengineering.meshtak.R;

public class AkitaPreferenceFragment extends PluginPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TITLE = "Akita MeshTAK Preferences";
    private static final String SUBTITLE = "Mission transport configuration";
    private static android.content.Context staticPluginContext;

    public AkitaPreferenceFragment() {
        super(staticPluginContext, R.xml.preferences);
    }

    public AkitaPreferenceFragment(android.content.Context pluginContext) {
        super(pluginContext, R.xml.preferences);
        staticPluginContext = pluginContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindPreferenceSummary("connection_method");
        bindPreferenceSummary(AkitaTheme.PREF_UI_THEME);
        bindPreferenceSummary(AkitaMissionProfile.PREF_MISSION_PROFILE);
        bindPreferenceSummary("ble_device_name");
        bindPreferenceSummary("serial_port_path");
        bindPreferenceSummary("serial_baud_rate");
        bindPreferenceSummary("opentakserver_mission_name");
        bindPreferenceSummary(AkitaMockSettings.PREF_MOCK_BLE_STATUS);
        bindPreferenceSummary(AkitaMockSettings.PREF_MOCK_SERIAL_STATUS);
        bindPreferenceSummary(AkitaMockSettings.PREF_MOCK_BATTERY_LEVEL);
    }

    @Override
    public String getSubTitle() {
        return getSubTitle(TITLE, SUBTITLE);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        preference.setSummary(newValue == null ? "" : String.valueOf(newValue));
        return true;
    }

    private void bindPreferenceSummary(String key) {
        Preference preference = findPreference(key);
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceChangeListener(this);
        Object currentValue = getPreferenceManager().getSharedPreferences().getAll().get(key);
        if (currentValue != null) {
            preference.setSummary(String.valueOf(currentValue));
        }
    }
}