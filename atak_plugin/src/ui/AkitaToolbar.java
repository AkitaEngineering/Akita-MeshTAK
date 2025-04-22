package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.atakmap.android.plugin.ui.PluginToolbar;
import com.akitaengineering.meshtak.R;

public class AkitaToolbar extends PluginToolbar {

    private TextView connectionMethodStatusTextView;
    private TextView bleStatusTextView;
    private TextView serialStatusTextView;
    private Context context;

    public AkitaToolbar(Context context) {
        super(context, R.layout.akita_toolbar);
        this.context = context;
    }

    @Override
    public void onAttachedToView(View v) {
        connectionMethodStatusTextView = v.findViewById(R.id.connection_method_status);
        bleStatusTextView = v.findViewById(R.id.ble_status);
        serialStatusTextView = v.findViewById(R.id.serial_status);
        updateConnectionMethodDisplay();
        setBleStatus("Idle");
        setSerialStatus("Idle");
    }

     public void updateConnectionMethodDisplay() {
        if (connectionMethodStatusTextView != null && context != null) {
            String method = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("connection_method", "ble");
            String displayMethod = method.equalsIgnoreCase("ble") ? "Method: BLE" : "Method: Serial";
            connectionMethodStatusTextView.setText(displayMethod);
        }
    }

    public void setBleStatus(final String status) {
        if (bleStatusTextView != null) {
            bleStatusTextView.post(new Runnable() {
                @Override
                public void run() {
                    bleStatusTextView.setText("BLE: " + status);
                    updateStatusColor(bleStatusTextView, status);
                }
            });
        }
    }

    public void setDetailedBleStatus(final String detailedStatus) {
        if (bleStatusTextView != null) {
            bleStatusTextView.post(new Runnable() {
                @Override
                public void run() {
                    bleStatusTextView.setText("BLE: " + detailedStatus);
                    String generalStatus = detailedStatus.toLowerCase();
                    if (generalStatus.contains("connected")) {
                        updateStatusColor(bleStatusTextView, "Connected");
                    } else if (generalStatus.contains("disconnected") || generalStatus.contains("error")) {
                        updateStatusColor(bleStatusTextView, "Disconnected");
                    } else if (generalStatus.contains("connecting")) {
                        updateStatusColor(bleStatusTextView, "Connecting");
                    } else {
                        updateStatusColor(bleStatusTextView, "Idle");
                    }
                }
            });
        }
    }

    public void setSerialStatus(final String status) {
        if (serialStatusTextView != null) {
            serialStatusTextView.post(new Runnable() {
                @Override
                public void run() {
                    serialStatusTextView.setText("Serial: " + status);
                    updateStatusColor(serialStatusTextView, status);
                }
            });
        }
    }

    public void setDetailedSerialStatus(final String detailedStatus) {
        if (serialStatusTextView != null) {
            serialStatusTextView.post(new Runnable() {
                @Override
                public void run() {
                    serialStatusTextView.setText("Serial: " + detailedStatus);
                    String generalStatus = detailedStatus.toLowerCase();
                     if (generalStatus.contains("connected")) {
                        updateStatusColor(serialStatusTextView, "Connected");
                    } else if (generalStatus.contains("disconnected") || generalStatus.contains("error")) {
                        updateStatusColor(serialStatusTextView, "Disconnected");
                    } else if (generalStatus.contains("connecting")) {
                        updateStatusColor(serialStatusTextView, "Connecting");
                    } else {
                        updateStatusColor(serialStatusTextView, "Idle");
                    }
                }
            });
        }
    }

    private void updateStatusColor(TextView textView, String status) {
        if (status.equalsIgnoreCase("Connected")) {
            textView.setTextColor(Color.GREEN);
        } else if (status.equalsIgnoreCase("Disconnected") || status.equalsIgnoreCase("Error")) {
            textView.setTextColor(Color.RED);
        } else if (status.equalsIgnoreCase("Connecting")) {
            textView.setTextColor(Color.YELLOW);
        } else {
            textView.setTextColor(Color.WHITE);
        }
    }
}
