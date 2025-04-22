package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.atakmap.android.maps.MapView;
import com.akitaengineering.meshtak.R;
import com.akitaengineering.meshtak.services.BLEService;
import com.akitaengineering.meshtak.services.SerialService;

import java.util.ArrayList;
import java.util.List;

public class SendDataView extends LinearLayout implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Spinner dataFormatSpinner;
    private EditText dataToSendEditText;
    private Button sendButton;
    private Spinner commandHistorySpinner;
    private BLEService bleService;
    private SerialService serialService;
    private String connectionMethod;
    private List<String> commandHistory = new ArrayList<>();
    private ArrayAdapter<String> historyAdapter;
    private Context context;

    public SendDataView(Context context, MapView mapView, BLEService bleService, SerialService serialService) {
        super(context);
        this.context = context;
        this.bleService = bleService;
        this.serialService = serialService;
        this.connectionMethod = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("connection_method", "ble");

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.send_data_view, this, true);

        dataFormatSpinner = findViewById(R.id.data_format_spinner);
        dataToSendEditText = findViewById(R.id.data_to_send);
        sendButton = findViewById(R.id.send_button);
        commandHistorySpinner = findViewById(R.id.command_history_spinner);

        ArrayAdapter<CharSequence> formatAdapter = ArrayAdapter.createFromResource(context, R.array.data_formats, android.R.layout.simple_spinner_item);
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataFormatSpinner.setAdapter(formatAdapter);

        historyAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, commandHistory);
        commandHistorySpinner.setAdapter(historyAdapter);

        sendButton.setOnClickListener(v -> {
            String data = dataToSendEditText.getText().toString();
            if (!data.isEmpty()) {
                String selectedFormat = dataFormatSpinner.getSelectedItem().toString();
                byte[] formattedData = formatData(selectedFormat, data);
                sendDataToDevice(formattedData);
                addToCommandHistory(data);
                dataToSendEditText.getText().clear();
            } else {
                Toast.makeText(context, "Please enter data to send.", Toast.LENGTH_SHORT).show();
            }
        });

        commandHistorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCommand = (String) parent.getItemAtPosition(position);
                dataToSendEditText.setText(selectedCommand);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });

        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        PreferenceManager.getDefaultSharedPreferences(context)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("connection_method")) {
            connectionMethod = sharedPreferences.getString(key, "ble");
            //  You can add logic here to enable/disable specific UI elements
            //  based on the selected connection method if needed.
            Toast.makeText(context, "Connection method changed to: " + connectionMethod, Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] formatData(String format, String data) {
        String formattedString = "";
        switch (format) {
            case "Plain Text":
                formattedString = "TXT:" + data;
                break;
            case "JSON":
                formattedString = "JSON:" + data;
                break;
            case "Custom":
                formattedString = "CUSTOM:" + data;
                break;
            default:
                formattedString = data; // Default to plain text
                break;
        }
        return formattedString.getBytes();
    }

    private void sendDataToDevice(byte[] data) {
       if (connectionMethod.equals("ble") && bleService != null) {
            bleService.sendData(data);
            Toast.makeText(getContext(), "Sent via BLE: " + new String(data), Toast.LENGTH_SHORT).show();
        } else if (connectionMethod.equals("serial") && serialService != null) {
            serialService.sendData(data);
            Toast.makeText(getContext(), "Sent via Serial: " + new String(data), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Not connected or connection method not selected.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToCommandHistory(String command) {
        if (!commandHistory.contains(command)) {
            commandHistory.add(command);
            historyAdapter.notifyDataSetChanged();
            //  Optionally, you could persist this history
        }
    }
}
