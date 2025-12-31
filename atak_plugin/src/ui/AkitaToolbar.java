// File: atak_plugin/src/ui/AkitaToolbar.java
// Description: Handles the custom ATAK toolbar UI, displaying connection status and battery health.
package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.atakmap.android.plugin.ui.PluginToolbar;
import com.akitaengineering.meshtak.R;
import com.akitaengineering.meshtak.services.BLEService;
import com.akitaengineering.meshtak.services.SerialService;
import com.akitaengineering.meshtak.AuditLogger;

import androidx.preference.PreferenceManager;

public class AkitaToolbar extends PluginToolbar implements SharedPreferences.OnSharedPreferenceChangeListener {

    private TextView connectionMethodStatusTextView;
    private TextView bleStatusTextView;
    private TextView serialStatusTextView;
    private TextView batteryStatusTextView;
    private Button sosButton;              
    
    private Context context;
    private BLEService bleService;
    private SerialService serialService;

    public AkitaToolbar(Context context) {
        super(context, R.layout.akita_toolbar);
        this.context = context;
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this);
    }
    
    public void setServices(BLEService ble, SerialService serial) {
        this.bleService = ble;
        this.serialService = serial;
        setBatteryStatus("--%");
    }

    @Override
    public void onAttachedToView(View v) {
        connectionMethodStatusTextView = v.findViewById(R.id.connection_method_status);
        bleStatusTextView = v.findViewById(R.id.ble_status);
        serialStatusTextView = v.findViewById(R.id.serial_status);
        batteryStatusTextView = v.findViewById(R.id.battery_status);
        sosButton = v.findViewById(R.id.sos_button);

        updateConnectionMethodDisplay();
        setBleStatus("Idle");
        setSerialStatus("Idle");
        setBatteryStatus("--%");

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
        if (key.equals("connection_method")) {
            updateConnectionMethodDisplay();
        }
    }

    public void updateConnectionMethodDisplay() {
        if (connectionMethodStatusTextView != null && context != null) {
            String method = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("connection_method", "ble");
            String displayMethod = method.equalsIgnoreCase("ble") ? "Method: BLE" : "Method: Serial";
            connectionMethodStatusTextView.setText(displayMethod);
        }
    }
    
    private void triggerSosAlert() {
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
                batteryStatusTextView.setText("BATT: " + status);
                
                try {
                    int percent = Integer.parseInt(status.replace("%", "").trim());
                    if (percent <= 20) {
                        batteryStatusTextView.setTextColor(Color.RED);
                    } else if (percent <= 50) {
                        batteryStatusTextView.setTextColor(Color.YELLOW);
                    } else {
                        batteryStatusTextView.setTextColor(Color.GREEN);
                    }
                } catch (NumberFormatException e) {
                    batteryStatusTextView.setTextColor(Color.WHITE);
                }
            });
        }
    }

    public void setDetailedBleStatus(final String detailedStatus) {
        if (bleStatusTextView != null) {
            bleStatusTextView.post(() -> {
                bleStatusTextView.setText("BLE: " + detailedStatus);
                String generalStatus = detailedStatus.toLowerCase();
                if (generalStatus.contains("connected")) {
                    updateStatusColor(bleStatusTextView, "Connected");
                } else if (generalStatus.contains("disconnected") || generalStatus.contains("error") || generalStatus.contains("failed")) {
                    updateStatusColor(bleStatusTextView, "Disconnected");
                } else if (generalStatus.contains("connecting") || generalStatus.contains("scanning")) {
                    updateStatusColor(bleStatusTextView, "Connecting");
                } else {
                    updateStatusColor(bleStatusTextView, "Idle");
                }
            });
        }
    }

    public void setDetailedSerialStatus(final String detailedStatus) {
        if (serialStatusTextView != null) {
            serialStatusTextView.post(() -> {
                serialStatusTextView.setText("Serial: " + detailedStatus);
                String generalStatus = detailedStatus.toLowerCase();
                if (generalStatus.contains("connected")) {
                    updateStatusColor(serialStatusTextView, "Connected");
                } else if (generalStatus.contains("disconnected") || generalStatus.contains("error") || generalStatus.contains("failed")) {
                    updateStatusColor(serialStatusTextView, "Disconnected");
                } else if (generalStatus.contains("connecting") || generalStatus.contains("searching")) {
                    updateStatusColor(serialStatusTextView, "Connecting");
                } else {
                    updateStatusColor(serialStatusTextView, "Idle");
                }
            });
        }
    }

    private void updateStatusColor(TextView textView, String status) {
        if (status.equalsIgnoreCase("Connected")) {
            textView.setTextColor(Color.GREEN);
        } else if (status.equalsIgnoreCase("Disconnected") || status.equalsIgnoreCase("Error")) {
            textView.setTextColor(Color.RED);
        } else if (status.equalsIgnoreCase("Connecting") || status.equalsIgnoreCase("Idle")) {
            textView.setTextColor(Color.YELLOW);
        } else {
            textView.setTextColor(Color.WHITE);
        }
    }
}
