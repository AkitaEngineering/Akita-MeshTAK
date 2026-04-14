// File: atak_plugin/src/ui/AkitaToolbar.java
// Description: Handles the custom ATAK toolbar UI, displaying connection status and battery health.
package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.atakmap.android.plugin.ui.PluginToolbar;
import com.akitaengineering.meshtak.R;
import com.akitaengineering.meshtak.services.BLEService;
import com.akitaengineering.meshtak.services.SerialService;
import com.akitaengineering.meshtak.AuditLogger;

import androidx.preference.PreferenceManager;

public class AkitaToolbar extends PluginToolbar implements SharedPreferences.OnSharedPreferenceChangeListener {

    private View attachedView;
    private View toolbarRoot;
    private TextView toolbarTitleTextView;
    private TextView toolbarSubtitleTextView;
    private TextView themeBadgeTextView;
    private TextView connectionMethodStatusTextView;
    private TextView missionProfileStatusTextView;
    private TextView securityPostureStatusTextView;
    private TextView bleStatusTextView;
    private TextView serialStatusTextView;
    private TextView batteryStatusTextView;
    private TextView toolbarHealthTextView;
    private Button sosButton;              
    
    private Context context;
    private BLEService bleService;
    private SerialService serialService;
    private String bleDetailedStatus = "Idle";
    private String serialDetailedStatus = "Idle";
    private int batteryPercent = -1;

    public AkitaToolbar(Context context) {
        super(context, R.layout.akita_toolbar);
        this.context = context;
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this);
    }
    
    public void setServices(BLEService ble, SerialService serial) {
        this.bleService = ble;
        this.serialService = serial;
        updateConnectionMethodDisplay();
        updateMissionProfileStatus();
        updateSecurityPostureStatus();
        setBatteryStatus("--%");
    }

    @Override
    public void onAttachedToView(View v) {
        attachedView = v;
        toolbarRoot = v.findViewById(R.id.toolbar_root);
        toolbarTitleTextView = v.findViewById(R.id.toolbar_title);
        toolbarSubtitleTextView = v.findViewById(R.id.toolbar_subtitle);
        themeBadgeTextView = v.findViewById(R.id.theme_badge);
        connectionMethodStatusTextView = v.findViewById(R.id.connection_method_status);
        missionProfileStatusTextView = v.findViewById(R.id.mission_profile_status);
        securityPostureStatusTextView = v.findViewById(R.id.security_posture_status);
        bleStatusTextView = v.findViewById(R.id.ble_status);
        serialStatusTextView = v.findViewById(R.id.serial_status);
        batteryStatusTextView = v.findViewById(R.id.battery_status);
        toolbarHealthTextView = v.findViewById(R.id.toolbar_health_label);
        sosButton = v.findViewById(R.id.sos_button);

        applyTheme();
        updateConnectionMethodDisplay();
        updateMissionProfileStatus();
        updateSecurityPostureStatus();
        setDetailedBleStatus(bleDetailedStatus);
        setDetailedSerialStatus(serialDetailedStatus);
        setBatteryStatus(batteryPercent >= 0 ? batteryPercent + "%" : "--%");

        if (sosButton != null) {
            sosButton.setOnClickListener(view -> triggerSosAlert());
        }
    }
    
    @Override
    public void onDetachedFromView(View v) {
        // Unregister the listener when the toolbar is removed
        PreferenceManager.getDefaultSharedPreferences(context)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("connection_method") || key.equals("ble_device_name") || key.equals("serial_baud_rate") || key.equals("serial_port_path")) {
            updateConnectionMethodDisplay();
            updateSecurityPostureStatus();
        } else if (AkitaMissionProfile.PREF_MISSION_PROFILE.equals(key)) {
            updateMissionProfileStatus();
        } else if (AkitaMockSettings.PREF_MOCK_MODE.equals(key)
                || AkitaMockSettings.PREF_MOCK_BLE_STATUS.equals(key)
                || AkitaMockSettings.PREF_MOCK_SERIAL_STATUS.equals(key)
                || AkitaMockSettings.PREF_MOCK_BATTERY_LEVEL.equals(key)) {
            updateConnectionMethodDisplay();
            updateSecurityPostureStatus();
        } else if (AkitaTheme.PREF_UI_THEME.equals(key)) {
            applyTheme();
        }
    }

    public void updateConnectionMethodDisplay() {
        if (connectionMethodStatusTextView != null && context != null) {
            String method = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("connection_method", "ble");
            String displayMethod;
            if (method.equalsIgnoreCase("ble")) {
                String deviceName = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString("ble_device_name", "AkitaNode01");
                displayMethod = "Secure route: BLE • Target " + deviceName;
            } else {
                String baudRate = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString("serial_baud_rate", "115200");
                String portPath = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString("serial_port_path", "/dev/ttyUSB0");
                displayMethod = "Secure route: Serial • " + portPath + " @ " + baudRate;
            }
            connectionMethodStatusTextView.setText(displayMethod);
            updateMissionProfileStatus();
            updateOperationalHealth();
        }
    }

    private void updateMissionProfileStatus() {
        if (missionProfileStatusTextView == null) {
            return;
        }
        String label = AkitaMissionProfile.getProfileLabel(PreferenceManager.getDefaultSharedPreferences(context));
        missionProfileStatusTextView.setText("Profile: " + label);
    }

    private void updateSecurityPostureStatus() {
        if (securityPostureStatusTextView == null) {
            return;
        }
        securityPostureStatusTextView.setText(AkitaOperationalReadiness.getToolbarSecurityStatus(context, isActiveTransportAvailable()));
        securityPostureStatusTextView.setTextColor(AkitaTheme.statusColor(
                securityPostureStatusTextView.getText().toString(),
                AkitaTheme.resolvePalette(context)));
    }

    private boolean isActiveTransportAvailable() {
        if (AkitaMockSettings.isEnabled(context)) {
            return true;
        }
        String method = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("connection_method", "ble");
        return "ble".equalsIgnoreCase(method) ? bleService != null : serialService != null;
    }
    
    private void triggerSosAlert() {
        if (AkitaMockSettings.isEnabled(context)) {
            Toast.makeText(context, "MOCK ALERT: SOS simulated for dashboard validation.", Toast.LENGTH_SHORT).show();
            AuditLogger.getInstance().log(AuditLogger.EventType.SOS_TRIGGERED,
                    AuditLogger.Severity.CRITICAL,
                    "AkitaToolbar",
                    "SOS alert simulated in mock mode",
                    true);
            return;
        }

        String method = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("connection_method", "ble");
        
        // CRITICAL: Log SOS trigger for accountability
        AuditLogger.getInstance().log(AuditLogger.EventType.SOS_TRIGGERED, 
                                    AuditLogger.Severity.CRITICAL,
                                    "AkitaToolbar", 
                                    "SOS alert triggered via " + method.toUpperCase(), 
                                    true);
                
        if (method.equals("ble") && bleService != null) {
            bleService.sendCriticalAlert();
            Toast.makeText(context, "ALERT: SOS sent via BLE!", Toast.LENGTH_SHORT).show();
        } else if (method.equals("serial") && serialService != null) {
            serialService.sendCriticalAlert();
            Toast.makeText(context, "ALERT: SOS sent via Serial!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Cannot send SOS: Device disconnected.", Toast.LENGTH_SHORT).show();
            AuditLogger.getInstance().log(AuditLogger.EventType.ERROR, 
                                        AuditLogger.Severity.ERROR,
                                        "AkitaToolbar", 
                                        "SOS send failed - device disconnected", 
                                        false);
        }
    }
    
    public void setBatteryStatus(final String status) {
        if (batteryStatusTextView != null) {
            batteryStatusTextView.post(() -> {
                AkitaTheme.Palette palette = AkitaTheme.resolvePalette(context);
                String label = "Awaiting telemetry";
                try {
                    int percent = Integer.parseInt(status.replace("%", "").trim());
                    batteryPercent = percent;
                    if (percent <= 20) {
                        label = "Reserve power";
                        batteryStatusTextView.setTextColor(palette.danger);
                    } else if (percent <= 50) {
                        label = "Operational";
                        batteryStatusTextView.setTextColor(palette.warning);
                    } else {
                        label = "Mission ready";
                        batteryStatusTextView.setTextColor(palette.success);
                    }
                } catch (NumberFormatException e) {
                    batteryPercent = -1;
                    batteryStatusTextView.setTextColor(palette.textPrimary);
                }
                batteryStatusTextView.setText("Battery: " + status + " • " + label);
                updateOperationalHealth();
            });
        }
    }

    public void setDetailedBleStatus(final String detailedStatus) {
        bleDetailedStatus = detailedStatus;
        if (bleStatusTextView != null) {
            bleStatusTextView.post(() -> {
                bleStatusTextView.setText("BLE: " + detailedStatus);
                updateStatusColor(bleStatusTextView, detailedStatus);
                updateOperationalHealth();
            });
        }
    }

    public void setDetailedSerialStatus(final String detailedStatus) {
        serialDetailedStatus = detailedStatus;
        if (serialStatusTextView != null) {
            serialStatusTextView.post(() -> {
                serialStatusTextView.setText("Serial: " + detailedStatus);
                updateStatusColor(serialStatusTextView, detailedStatus);
                updateOperationalHealth();
            });
        }
    }

    private void updateStatusColor(TextView textView, String status) {
        textView.setTextColor(AkitaTheme.statusColor(status, AkitaTheme.resolvePalette(context)));
    }

    private void updateOperationalHealth() {
        if (toolbarHealthTextView == null) {
            return;
        }

        String activeMethod = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("connection_method", "ble");
        String activeStatus = activeMethod.equalsIgnoreCase("ble") ? bleDetailedStatus : serialDetailedStatus;
        String normalizedStatus = activeStatus == null ? "" : activeStatus.toLowerCase();

        String healthText;
        if (normalizedStatus.contains("connected")) {
            healthText = batteryPercent >= 0 && batteryPercent <= 20
                    ? "Secure link: Connected, low power"
                    : "Secure link: Mission ready";
        } else if (normalizedStatus.contains("connecting") || normalizedStatus.contains("scanning") || normalizedStatus.contains("searching")) {
            healthText = "Secure link: Establishing route";
        } else if (normalizedStatus.contains("error") || normalizedStatus.contains("failed") || normalizedStatus.contains("disconnected")) {
            healthText = "Secure link: Degraded";
        } else {
            healthText = "Secure link: Standby";
        }

        toolbarHealthTextView.setText(healthText);
        toolbarHealthTextView.setTextColor(AkitaTheme.statusColor(healthText, AkitaTheme.resolvePalette(context)));
    }

    private void applyTheme() {
        if (attachedView == null) {
            return;
        }

        AkitaTheme.Palette palette = AkitaTheme.resolvePalette(context);

        if (toolbarRoot != null) {
            toolbarRoot.setBackground(AkitaTheme.createAccentPanelDrawable(context, palette));
        }
        if (toolbarTitleTextView != null) {
            toolbarTitleTextView.setTextColor(palette.textPrimary);
        }
        if (toolbarSubtitleTextView != null) {
            toolbarSubtitleTextView.setTextColor(palette.textSecondary);
        }
        if (themeBadgeTextView != null) {
            themeBadgeTextView.setBackground(AkitaTheme.createBadgeDrawable(context, palette, true));
            themeBadgeTextView.setTextColor(palette.white);
            themeBadgeTextView.setText(AkitaTheme.getThemeLabel(context));
        }
        if (connectionMethodStatusTextView != null) {
            connectionMethodStatusTextView.setBackground(AkitaTheme.createStatTileDrawable(context, palette));
            connectionMethodStatusTextView.setTextColor(palette.textPrimary);
        }
        if (bleStatusTextView != null) {
            bleStatusTextView.setBackground(AkitaTheme.createStatTileDrawable(context, palette));
            updateStatusColor(bleStatusTextView, bleDetailedStatus);
        }
        if (serialStatusTextView != null) {
            serialStatusTextView.setBackground(AkitaTheme.createStatTileDrawable(context, palette));
            updateStatusColor(serialStatusTextView, serialDetailedStatus);
        }
        if (batteryStatusTextView != null) {
            batteryStatusTextView.setBackground(AkitaTheme.createStatTileDrawable(context, palette));
        }
        if (toolbarHealthTextView != null) {
            toolbarHealthTextView.setBackground(AkitaTheme.createStatTileDrawable(context, palette));
            updateOperationalHealth();
        }
        if (missionProfileStatusTextView != null) {
            missionProfileStatusTextView.setBackground(AkitaTheme.createStatTileDrawable(context, palette));
            missionProfileStatusTextView.setTextColor(palette.textPrimary);
            updateMissionProfileStatus();
        }
        if (securityPostureStatusTextView != null) {
            securityPostureStatusTextView.setBackground(AkitaTheme.createStatTileDrawable(context, palette));
            updateSecurityPostureStatus();
        }
        if (sosButton != null) {
            sosButton.setBackground(AkitaTheme.createDangerButtonDrawable(context, palette));
            sosButton.setTextColor(palette.white);
        }
        setBatteryStatus(batteryPercent >= 0 ? batteryPercent + "%" : "--%");
    }
}
