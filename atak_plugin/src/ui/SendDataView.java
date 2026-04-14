package com.akitaengineering.meshtak.ui;

import android.content.res.ColorStateList;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.atakmap.android.maps.MapView;
import com.akitaengineering.meshtak.R;
import com.akitaengineering.meshtak.services.BLEService;
import com.akitaengineering.meshtak.services.SerialService;

import org.json.JSONArray;
import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SendDataView extends LinearLayout implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String PREF_COMMAND_HISTORY = "dashboard_command_history";
    private static final String PREF_RECENT_PAYLOADS = "dashboard_recent_payloads";
    private static final String PREF_SUCCESSFUL_SENDS = "dashboard_successful_sends";
    private static final String PREF_FAILED_SENDS = "dashboard_failed_sends";
    private static final String PREF_BLE_ROUTE_COUNT = "dashboard_route_ble";
    private static final String PREF_SERIAL_ROUTE_COUNT = "dashboard_route_serial";
    private static final String PREF_FORMAT_PLAIN = "dashboard_format_plain";
    private static final String PREF_FORMAT_JSON = "dashboard_format_json";
    private static final String PREF_FORMAT_CUSTOM = "dashboard_format_custom";
    private static final String PREF_LAST_SEND_AT = "dashboard_last_send_at";
    private static final String PREF_LAST_SEND_BYTES = "dashboard_last_send_bytes";
    private static final String PREF_LAST_SEND_FORMAT = "dashboard_last_send_format";
    private static final String PREF_LAST_SEND_ROUTE = "dashboard_last_send_route";
    private Spinner dataFormatSpinner;
    private EditText dataToSendEditText;
    private Button sendButton;
    private Spinner commandHistorySpinner;
    private Button themeModeButton;
    private TextView routeChip;
    private TextView endpointChip;
    private TextView operationalSummaryTextView;
    private TextView payloadSummaryTextView;
    private TextView activeRouteValueTextView;
    private TextView payloadBudgetValueTextView;
    private TextView lastSendValueTextView;
    private TextView deliveryRatioValueTextView;
    private TextView historyCaptionTextView;
    private TextView payloadMetricsTextView;
    private TextView lastOperationTextView;
    private TextView plainLegendTextView;
    private TextView jsonLegendTextView;
    private TextView customLegendTextView;
    private ProgressBar payloadBudgetProgressBar;
    private PayloadTrendChartView payloadTrendChartView;
    private FormatDistributionChartView formatDistributionChartView;
    private View dashboardContent;
    private View summaryCard;
    private View trendCard;
    private View formatCard;
    private View composerCard;
    private View historyCard;
    private View routeStatCard;
    private View payloadStatCard;
    private View lastSendStatCard;
    private View deliveryStatCard;
    private View plainLegendDot;
    private View jsonLegendDot;
    private View customLegendDot;
    private BLEService bleService;
    private SerialService serialService;
    private String connectionMethod;
    private final List<String> commandHistory = new ArrayList<>();
    private final List<Integer> recentPayloadSizes = new ArrayList<>();
    private final List<String> dataFormats = new ArrayList<>();
    private ThemedSpinnerAdapter formatAdapter;
    private ThemedSpinnerAdapter historyAdapter;
    private final Context context;
    private final SharedPreferences preferences;
    private static final int MAX_HISTORY = 20;
    private static final int MAX_PAYLOAD_BYTES = 512;
    private static final int MAX_TREND_POINTS = 12;
    private int successfulSends;
    private int failedSends;
    private int bleRouteCount;
    private int serialRouteCount;
    private int plainTextCount;
    private int jsonCount;
    private int customCount;
    private long lastSendAt;
    private int lastSendBytes;
    private String lastSendFormat = "";
    private String lastSendRoute = "";

    public SendDataView(Context context, MapView mapView, BLEService bleService, SerialService serialService) {
        super(context);
        this.context = context;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.bleService = bleService;
        this.serialService = serialService;
        this.connectionMethod = preferences.getString("connection_method", "ble");
        setOrientation(VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.send_data_view, this, true);

        bindViews();
        loadPersistedState();
        setupFormatSpinner();
        setupHistorySpinner();
        setupComposerInteractions();
        updateThemeModeButton();
        refreshHistorySpinner();
        applyTheme();
        refreshDashboard();

        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void setServices(BLEService bleService, SerialService serialService) {
        this.bleService = bleService;
        this.serialService = serialService;
        refreshDashboard();
    }

    private void bindViews() {
        dataFormatSpinner = findViewById(R.id.data_format_spinner);
        dataToSendEditText = findViewById(R.id.data_to_send);
        sendButton = findViewById(R.id.send_button);
        commandHistorySpinner = findViewById(R.id.command_history_spinner);
        themeModeButton = findViewById(R.id.theme_mode_button);
        routeChip = findViewById(R.id.route_chip);
        endpointChip = findViewById(R.id.endpoint_chip);
        operationalSummaryTextView = findViewById(R.id.operational_summary);
        payloadSummaryTextView = findViewById(R.id.payload_summary);
        activeRouteValueTextView = findViewById(R.id.stat_active_route_value);
        payloadBudgetValueTextView = findViewById(R.id.stat_payload_value);
        lastSendValueTextView = findViewById(R.id.stat_last_send_value);
        deliveryRatioValueTextView = findViewById(R.id.stat_delivery_value);
        historyCaptionTextView = findViewById(R.id.history_caption);
        payloadMetricsTextView = findViewById(R.id.payload_metrics);
        lastOperationTextView = findViewById(R.id.last_operation_text);
        plainLegendTextView = findViewById(R.id.plain_legend_text);
        jsonLegendTextView = findViewById(R.id.json_legend_text);
        customLegendTextView = findViewById(R.id.custom_legend_text);
        payloadBudgetProgressBar = findViewById(R.id.payload_budget_progress);
        payloadTrendChartView = findViewById(R.id.payload_trend_chart);
        formatDistributionChartView = findViewById(R.id.format_distribution_chart);
        dashboardContent = findViewById(R.id.dashboard_content);
        summaryCard = findViewById(R.id.summary_card);
        trendCard = findViewById(R.id.trend_card);
        formatCard = findViewById(R.id.format_card);
        composerCard = findViewById(R.id.composer_card);
        historyCard = findViewById(R.id.history_card);
        routeStatCard = findViewById(R.id.stat_route_card);
        payloadStatCard = findViewById(R.id.stat_payload_card);
        lastSendStatCard = findViewById(R.id.stat_last_send_card);
        deliveryStatCard = findViewById(R.id.stat_delivery_card);
        plainLegendDot = findViewById(R.id.plain_legend_dot);
        jsonLegendDot = findViewById(R.id.json_legend_dot);
        customLegendDot = findViewById(R.id.custom_legend_dot);
    }

    private void setupFormatSpinner() {
        dataFormats.clear();
        dataFormats.addAll(Arrays.asList(context.getResources().getStringArray(R.array.data_formats)));
        formatAdapter = new ThemedSpinnerAdapter(context, dataFormats);
        dataFormatSpinner.setAdapter(formatAdapter);
        dataFormatSpinner.setSelection(0, false);
        dataFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePayloadInsights();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updatePayloadInsights();
            }
        });
    }

    private void setupHistorySpinner() {
        historyAdapter = new ThemedSpinnerAdapter(context, new ArrayList<>());
        commandHistorySpinner.setAdapter(historyAdapter);
        commandHistorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!commandHistorySpinner.isEnabled() || position <= 0) {
                    return;
                }
                String selectedCommand = historyAdapter.getItem(position);
                if (!TextUtils.isEmpty(selectedCommand)) {
                    dataToSendEditText.setText(selectedCommand);
                    dataToSendEditText.setSelection(selectedCommand.length());
                }
                commandHistorySpinner.setSelection(0, false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupComposerInteractions() {
        dataToSendEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePayloadInsights();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        themeModeButton.setOnClickListener(v ->
                AkitaTheme.setThemeMode(context, AkitaTheme.nextThemeMode(context)));

        sendButton.setOnClickListener(v -> sendCurrentPayload());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("connection_method".equals(key)) {
            connectionMethod = sharedPreferences.getString(key, "ble");
            refreshDashboard();
            return;
        }
        if (AkitaTheme.PREF_UI_THEME.equals(key)) {
            updateThemeModeButton();
            applyTheme();
            refreshDashboard();
            return;
        }
        if (AkitaMockSettings.PREF_MOCK_MODE.equals(key)
                || AkitaMockSettings.PREF_MOCK_BLE_STATUS.equals(key)
                || AkitaMockSettings.PREF_MOCK_SERIAL_STATUS.equals(key)
                || AkitaMockSettings.PREF_MOCK_BATTERY_LEVEL.equals(key)
                || "ble_device_name".equals(key)
                || "serial_baud_rate".equals(key)
                || "serial_port_path".equals(key)) {
            refreshDashboard();
            refreshHistorySpinner();
        }
    }

    private byte[] formatData(String format, String data) {
        String formattedString;
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
                formattedString = data;
                break;
        }
        return formattedString.getBytes(StandardCharsets.UTF_8);
    }

    private boolean sendDataToDevice(byte[] data) {
        if (AkitaMockSettings.isEnabled(preferences)) {
            Toast.makeText(getContext(),
                    "Simulated send via " + connectionMethod.toUpperCase(Locale.US) + ": " + new String(data),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if ("ble".equals(connectionMethod) && bleService != null) {
            bleService.sendData(data);
            Toast.makeText(getContext(), "Sent via BLE: " + new String(data), Toast.LENGTH_SHORT).show();
            return true;
        }
        if ("serial".equals(connectionMethod) && serialService != null) {
            serialService.sendData(data);
            Toast.makeText(getContext(), "Sent via Serial: " + new String(data), Toast.LENGTH_SHORT).show();
            return true;
        }
        Toast.makeText(getContext(), "Not connected or connection method not selected.", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void addToCommandHistory(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        commandHistory.remove(command);
        commandHistory.add(0, command);
        while (commandHistory.size() > MAX_HISTORY) {
            commandHistory.remove(commandHistory.size() - 1);
        }
        persistState();
        refreshHistorySpinner();
    }

    private void sendCurrentPayload() {
        String data = dataToSendEditText.getText().toString();
        if (TextUtils.isEmpty(data.trim())) {
            Toast.makeText(context, "Please enter data to send.", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedFormat = getSelectedFormat();
        byte[] formattedData = formatData(selectedFormat, data);
        if (formattedData.length > MAX_PAYLOAD_BYTES) {
            recordFailedSend();
            Toast.makeText(context, "Payload too large (max " + MAX_PAYLOAD_BYTES + " bytes).", Toast.LENGTH_SHORT).show();
            updatePayloadInsights();
            return;
        }

        if (sendDataToDevice(formattedData)) {
            recordSuccessfulSend(selectedFormat, formattedData.length);
            addToCommandHistory(data);
            dataToSendEditText.getText().clear();
        } else {
            recordFailedSend();
        }
        refreshDashboard();
    }

    private void recordSuccessfulSend(String format, int payloadBytes) {
        successfulSends++;
        lastSendAt = System.currentTimeMillis();
        lastSendBytes = payloadBytes;
        lastSendFormat = format;
        lastSendRoute = connectionMethod;

        if ("ble".equals(connectionMethod)) {
            bleRouteCount++;
        } else {
            serialRouteCount++;
        }

        if ("JSON".equals(format)) {
            jsonCount++;
        } else if ("Custom".equals(format)) {
            customCount++;
        } else {
            plainTextCount++;
        }

        recentPayloadSizes.add(payloadBytes);
        while (recentPayloadSizes.size() > MAX_TREND_POINTS) {
            recentPayloadSizes.remove(0);
        }
        persistState();
    }

    private void recordFailedSend() {
        failedSends++;
        persistState();
        refreshDashboard();
    }

    private void refreshDashboard() {
        boolean mockModeEnabled = AkitaMockSettings.isEnabled(preferences);
        String routeLabel = "ble".equals(connectionMethod) ? "BLE" : "Serial";
        routeChip.setText((mockModeEnabled ? "Simulated route: " : "Active route: ") + routeLabel);
        endpointChip.setText(buildEndpointDescription());
        operationalSummaryTextView.setText(buildOperationalSummary());
        activeRouteValueTextView.setText(routeLabel);
        long displayLastSendAt = getDisplayLastSendAt();
        lastSendValueTextView.setText(displayLastSendAt > 0 ? formatRelativeTime(displayLastSendAt) : "Never");
        deliveryRatioValueTextView.setText(buildDeliveryRatio());
        lastOperationTextView.setText(buildLastOperationText());
        historyCaptionTextView.setText(buildHistoryCaption());

        plainLegendTextView.setText(String.format(Locale.US, "Plain Text %d", getDisplayPlainTextCount()));
        jsonLegendTextView.setText(String.format(Locale.US, "JSON %d", getDisplayJsonCount()));
        customLegendTextView.setText(String.format(Locale.US, "Custom %d", getDisplayCustomCount()));

        payloadTrendChartView.setValues(getDisplayPayloadTrend());
        payloadTrendChartView.setMaxValue(MAX_PAYLOAD_BYTES);
        formatDistributionChartView.setData(getDisplayPlainTextCount(), getDisplayJsonCount(), getDisplayCustomCount());

        updatePayloadInsights();
    }

    private void updatePayloadInsights() {
        AkitaTheme.Palette palette = AkitaTheme.resolvePalette(context);
        int payloadBytes = getPreparedPayloadBytes();
        int remainingBytes = Math.max(0, MAX_PAYLOAD_BYTES - payloadBytes);
        payloadBudgetProgressBar.setMax(MAX_PAYLOAD_BYTES);
        payloadBudgetProgressBar.setProgress(Math.min(payloadBytes, MAX_PAYLOAD_BYTES));

        int progressColor = payloadBytes > MAX_PAYLOAD_BYTES
                ? palette.danger
                : payloadBytes >= (MAX_PAYLOAD_BYTES * 0.75f)
                ? palette.warning
                : palette.accentStrong;

        payloadBudgetProgressBar.setProgressTintList(ColorStateList.valueOf(progressColor));
        payloadBudgetProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(
            palette.monochrome ? palette.background : AkitaTheme.withAlpha(palette.outline, 85)));

        payloadBudgetValueTextView.setText(String.format(Locale.US, "%d / %d B", payloadBytes, MAX_PAYLOAD_BYTES));
        if (payloadBytes == 0) {
            payloadSummaryTextView.setText("Compose a secure message to verify how it fits inside the field radio envelope.");
            payloadMetricsTextView.setText("Draft size: 0 / 512 bytes");
            payloadMetricsTextView.setTextColor(palette.textSecondary);
            return;
        }

        if (payloadBytes > MAX_PAYLOAD_BYTES) {
            payloadSummaryTextView.setText(String.format(Locale.US, "Secure frame exceeds the safe payload ceiling by %d bytes.", payloadBytes - MAX_PAYLOAD_BYTES));
        } else if (payloadBytes >= (MAX_PAYLOAD_BYTES * 0.75f)) {
            payloadSummaryTextView.setText("Secure frame is nearing the radio payload ceiling. Consider trimming before release.");
        } else {
            payloadSummaryTextView.setText("Secure frame fits comfortably inside the radio payload envelope.");
        }

        payloadMetricsTextView.setText(String.format(Locale.US,
                "Prepared secure frame: %s • %d B • %d B remaining",
                getSelectedFormat(),
                payloadBytes,
                remainingBytes));
        payloadMetricsTextView.setTextColor(progressColor);
    }

    private int getPreparedPayloadBytes() {
        String draft = dataToSendEditText.getText() == null ? "" : dataToSendEditText.getText().toString();
        if (TextUtils.isEmpty(draft.trim())) {
            return 0;
        }
        return formatData(getSelectedFormat(), draft).length;
    }

    private String getSelectedFormat() {
        Object selectedItem = dataFormatSpinner.getSelectedItem();
        if (selectedItem == null) {
            return dataFormats.isEmpty() ? "Plain Text" : dataFormats.get(0);
        }
        return selectedItem.toString();
    }

    private String buildEndpointDescription() {
        if (AkitaMockSettings.isEnabled(preferences)) {
            if ("ble".equals(connectionMethod)) {
                String deviceName = preferences.getString("ble_device_name", "AkitaNode01");
                return "Simulated target " + deviceName;
            }
            String baudRate = preferences.getString("serial_baud_rate", "115200");
            String portPath = preferences.getString("serial_port_path", "/dev/ttyUSB0");
            return "Simulated " + portPath + " @ " + baudRate;
        }
        if ("ble".equals(connectionMethod)) {
            String deviceName = preferences.getString("ble_device_name", "AkitaNode01");
            return "Target " + deviceName;
        }
        String baudRate = preferences.getString("serial_baud_rate", "115200");
        String portPath = preferences.getString("serial_port_path", "/dev/ttyUSB0");
        return portPath + " @ " + baudRate;
    }

    private String buildOperationalSummary() {
        if (AkitaMockSettings.isEnabled(preferences)) {
            if ("ble".equals(connectionMethod)) {
                String deviceName = preferences.getString("ble_device_name", "AkitaNode01");
                return "Mock secure transport active. BLE route is being simulated for " + deviceName + " so operators can validate encrypted ATAK interoperability without hardware.";
            }
            String baudRate = preferences.getString("serial_baud_rate", "115200");
            String portPath = preferences.getString("serial_port_path", "/dev/ttyUSB0");
            return "Mock secure transport active. Serial route is being simulated on " + portPath + " at " + baudRate + " baud for interoperability validation without hardware.";
        }
        if ("ble".equals(connectionMethod)) {
            String deviceName = preferences.getString("ble_device_name", "AkitaNode01");
            if (bleService != null) {
                return "Encrypted BLE mesh profile armed for " + deviceName + ". Secure ATAK and partner traffic will route over the mesh radio when transmitted.";
            }
            return "Encrypted BLE mesh profile staged for " + deviceName + ". Waiting for the transport service to attach before release.";
        }

        String baudRate = preferences.getString("serial_baud_rate", "115200");
        String portPath = preferences.getString("serial_port_path", "/dev/ttyUSB0");
        if (serialService != null) {
            return "Encrypted serial profile armed on " + portPath + " at " + baudRate + " baud for direct device exchange and partner-system interoperability.";
        }
        return "Encrypted serial profile staged on " + portPath + " at " + baudRate + " baud. Waiting for the transport service to attach.";
    }

    private String buildDeliveryRatio() {
        int attempts = getDisplaySuccessfulSends() + getDisplayFailedSends();
        if (attempts <= 0) {
            return "Awaiting";
        }
        int ratio = Math.round((getDisplaySuccessfulSends() * 100f) / attempts);
        return ratio + "%";
    }

    private String buildLastOperationText() {
        long displayLastSendAt = getDisplayLastSendAt();
        if (displayLastSendAt <= 0) {
            if (failedSends > 0) {
                return "Recent secure transmissions were blocked. Verify that the active route is connected before retrying.";
            }
            if (useDemoDashboardState()) {
                return "Mock encrypted telemetry is loaded to preview the dashboard. Any simulated transmit will replace the sample trend with local activity.";
            }
            return "No transmissions recorded yet.";
        }
        return String.format(Locale.US,
                "Last secure transmission: %s • %d B via %s • %s.",
                TextUtils.isEmpty(getDisplayLastSendFormat()) ? "Plain Text" : getDisplayLastSendFormat(),
                getDisplayLastSendBytes(),
                TextUtils.isEmpty(getDisplayLastSendRoute()) ? "BLE" : getDisplayLastSendRoute().toUpperCase(Locale.US),
                formatRelativeTime(displayLastSendAt));
    }

    private void refreshHistorySpinner() {
        List<String> displayHistory = getDisplayCommandHistory();
        List<String> historyItems = new ArrayList<>();
        if (displayHistory.isEmpty()) {
            historyItems.add(context.getString(R.string.history_spinner_empty));
            commandHistorySpinner.setEnabled(false);
        } else {
            historyItems.add(context.getString(R.string.history_spinner_prompt));
            historyItems.addAll(displayHistory);
            commandHistorySpinner.setEnabled(true);
        }
        historyAdapter.replaceItems(historyItems);
        commandHistorySpinner.setSelection(0, false);
        commandHistorySpinner.setAlpha(commandHistorySpinner.isEnabled() ? 1f : 0.65f);
    }

    private void updateThemeModeButton() {
        themeModeButton.setText(AkitaTheme.getThemeLabel(context));
    }

    private void applyTheme() {
        AkitaTheme.Palette palette = AkitaTheme.resolvePalette(context);

        setBackgroundColor(palette.background);
        dashboardContent.setBackgroundColor(palette.background);

        applyPanel(summaryCard, AkitaTheme.createAccentPanelDrawable(context, palette));
        applyPanel(trendCard, AkitaTheme.createPanelDrawable(context, palette, true));
        applyPanel(formatCard, AkitaTheme.createPanelDrawable(context, palette, true));
        applyPanel(composerCard, AkitaTheme.createPanelDrawable(context, palette, true));
        applyPanel(historyCard, AkitaTheme.createPanelDrawable(context, palette, true));

        applyPanel(routeStatCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(payloadStatCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(lastSendStatCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(deliveryStatCard, AkitaTheme.createStatTileDrawable(context, palette));

        routeChip.setBackground(AkitaTheme.createBadgeDrawable(context, palette, true));
        routeChip.setTextColor(palette.white);
        endpointChip.setBackground(AkitaTheme.createBadgeDrawable(context, palette, false));
        endpointChip.setTextColor(palette.textPrimary);

        sendButton.setBackground(AkitaTheme.createAccentButtonDrawable(context, palette));
        sendButton.setTextColor(palette.white);

        dataToSendEditText.setBackground(AkitaTheme.createInputDrawable(context, palette));
        dataToSendEditText.setTextColor(palette.textPrimary);
        dataToSendEditText.setHintTextColor(palette.textMuted);

        applySpinnerStyle(dataFormatSpinner, palette);
        applySpinnerStyle(commandHistorySpinner, palette);

        themeModeButton.setBackground(AkitaTheme.createBadgeDrawable(context, palette, true));
        themeModeButton.setTextColor(palette.white);
        updateThemeModeButton();

        payloadTrendChartView.setPalette(palette);
        formatDistributionChartView.setPalette(palette);

        plainLegendDot.setBackground(AkitaTheme.createDotDrawable(context, palette.silver));
        jsonLegendDot.setBackground(AkitaTheme.createDotDrawable(context, palette.navy));
        customLegendDot.setBackground(AkitaTheme.createDotDrawable(context, palette.accent));

        setTextColors(palette.textPrimary,
                R.id.dashboard_title,
                R.id.summary_card_title,
                R.id.stat_active_route_value,
                R.id.stat_payload_value,
                R.id.stat_last_send_value,
                R.id.stat_delivery_value,
                R.id.trend_card_title,
                R.id.format_card_title,
                R.id.composer_card_title,
                R.id.history_card_title);

        setTextColors(palette.textSecondary,
                R.id.dashboard_subtitle,
                R.id.operational_summary,
                R.id.payload_summary,
                R.id.last_operation_text,
                R.id.trend_card_subtitle,
                R.id.format_card_subtitle,
                R.id.composer_card_subtitle,
                R.id.history_caption,
                R.id.payload_metrics,
                R.id.plain_legend_text,
                R.id.json_legend_text,
                R.id.custom_legend_text,
                R.id.stat_route_label,
                R.id.stat_payload_label,
                R.id.stat_last_send_label,
                R.id.stat_delivery_label);

        formatAdapter.setPalette(palette);
        historyAdapter.setPalette(palette);
        updatePayloadInsights();
    }

    private boolean useDemoDashboardState() {
        return AkitaMockSettings.shouldUseDemoData(preferences, successfulSends, failedSends, commandHistory, recentPayloadSizes);
    }

    private List<String> getDisplayCommandHistory() {
        if (useDemoDashboardState()) {
            return AkitaMockSettings.getDemoCommands();
        }
        return commandHistory;
    }

    private List<Integer> getDisplayPayloadTrend() {
        if (useDemoDashboardState()) {
            return AkitaMockSettings.getDemoPayloads();
        }
        return recentPayloadSizes;
    }

    private int getDisplayPlainTextCount() {
        return useDemoDashboardState() ? AkitaMockSettings.getDemoPlainCount() : plainTextCount;
    }

    private int getDisplayJsonCount() {
        return useDemoDashboardState() ? AkitaMockSettings.getDemoJsonCount() : jsonCount;
    }

    private int getDisplayCustomCount() {
        return useDemoDashboardState() ? AkitaMockSettings.getDemoCustomCount() : customCount;
    }

    private int getDisplaySuccessfulSends() {
        return useDemoDashboardState() ? AkitaMockSettings.getDemoSuccessfulSends() : successfulSends;
    }

    private int getDisplayFailedSends() {
        return useDemoDashboardState() ? AkitaMockSettings.getDemoFailedSends() : failedSends;
    }

    private long getDisplayLastSendAt() {
        return useDemoDashboardState() ? AkitaMockSettings.getDemoLastSendAt() : lastSendAt;
    }

    private int getDisplayLastSendBytes() {
        return useDemoDashboardState() ? AkitaMockSettings.getDemoLastSendBytes() : lastSendBytes;
    }

    private String getDisplayLastSendFormat() {
        return useDemoDashboardState() ? AkitaMockSettings.getDemoLastSendFormat() : lastSendFormat;
    }

    private String getDisplayLastSendRoute() {
        return useDemoDashboardState() ? AkitaMockSettings.getDemoLastSendRoute() : lastSendRoute;
    }

    private String buildHistoryCaption() {
        if (useDemoDashboardState()) {
            return "Demo secure command library loaded for mock operations. Send any frame to replace it with local history.";
        }
        if (commandHistory.isEmpty()) {
            return "Recent secure commands and interoperable frames will appear here after transmission.";
        }
        return String.format(Locale.US, "%d reusable commands ready for retransmission.", commandHistory.size());
    }

    private void applyPanel(View view, android.graphics.drawable.Drawable drawable) {
        if (view != null) {
            view.setBackground(drawable);
        }
    }

    private void applySpinnerStyle(Spinner spinner, AkitaTheme.Palette palette) {
        spinner.setBackground(AkitaTheme.createInputDrawable(context, palette));
        spinner.setPopupBackgroundDrawable(AkitaTheme.createInputDrawable(context, palette));
    }

    private void setTextColors(int color, int... viewIds) {
        for (int viewId : viewIds) {
            View view = findViewById(viewId);
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(color);
            }
        }
    }

    private void loadPersistedState() {
        successfulSends = preferences.getInt(PREF_SUCCESSFUL_SENDS, 0);
        failedSends = preferences.getInt(PREF_FAILED_SENDS, 0);
        bleRouteCount = preferences.getInt(PREF_BLE_ROUTE_COUNT, 0);
        serialRouteCount = preferences.getInt(PREF_SERIAL_ROUTE_COUNT, 0);
        plainTextCount = preferences.getInt(PREF_FORMAT_PLAIN, 0);
        jsonCount = preferences.getInt(PREF_FORMAT_JSON, 0);
        customCount = preferences.getInt(PREF_FORMAT_CUSTOM, 0);
        lastSendAt = preferences.getLong(PREF_LAST_SEND_AT, 0L);
        lastSendBytes = preferences.getInt(PREF_LAST_SEND_BYTES, 0);
        lastSendFormat = preferences.getString(PREF_LAST_SEND_FORMAT, "");
        lastSendRoute = preferences.getString(PREF_LAST_SEND_ROUTE, "");

        commandHistory.clear();
        commandHistory.addAll(readStringArray(preferences.getString(PREF_COMMAND_HISTORY, "[]")));

        recentPayloadSizes.clear();
        recentPayloadSizes.addAll(readIntegerArray(preferences.getString(PREF_RECENT_PAYLOADS, "[]")));
    }

    private void persistState() {
        preferences.edit()
                .putInt(PREF_SUCCESSFUL_SENDS, successfulSends)
                .putInt(PREF_FAILED_SENDS, failedSends)
                .putInt(PREF_BLE_ROUTE_COUNT, bleRouteCount)
                .putInt(PREF_SERIAL_ROUTE_COUNT, serialRouteCount)
                .putInt(PREF_FORMAT_PLAIN, plainTextCount)
                .putInt(PREF_FORMAT_JSON, jsonCount)
                .putInt(PREF_FORMAT_CUSTOM, customCount)
                .putLong(PREF_LAST_SEND_AT, lastSendAt)
                .putInt(PREF_LAST_SEND_BYTES, lastSendBytes)
                .putString(PREF_LAST_SEND_FORMAT, lastSendFormat)
                .putString(PREF_LAST_SEND_ROUTE, lastSendRoute)
                .putString(PREF_COMMAND_HISTORY, writeStringArray(commandHistory))
                .putString(PREF_RECENT_PAYLOADS, writeIntegerArray(recentPayloadSizes))
                .apply();
    }

    private List<String> readStringArray(String serializedValue) {
        List<String> results = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(serializedValue);
            for (int i = 0; i < array.length(); i++) {
                results.add(array.getString(i));
            }
        } catch (JSONException ignored) {
        }
        return results;
    }

    private List<Integer> readIntegerArray(String serializedValue) {
        List<Integer> results = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(serializedValue);
            for (int i = 0; i < array.length(); i++) {
                results.add(array.getInt(i));
            }
        } catch (JSONException ignored) {
        }
        return results;
    }

    private String writeStringArray(List<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) {
            array.put(value);
        }
        return array.toString();
    }

    private String writeIntegerArray(List<Integer> values) {
        JSONArray array = new JSONArray();
        for (Integer value : values) {
            array.put(value);
        }
        return array.toString();
    }

    private String formatRelativeTime(long timestamp) {
        long ageSeconds = Math.max(0L, (System.currentTimeMillis() - timestamp) / 1000L);
        if (ageSeconds < 60L) {
            return ageSeconds + "s ago";
        }
        long ageMinutes = ageSeconds / 60L;
        if (ageMinutes < 60L) {
            return ageMinutes + "m ago";
        }
        long ageHours = ageMinutes / 60L;
        if (ageHours < 24L) {
            return ageHours + "h ago";
        }
        return (ageHours / 24L) + "d ago";
    }

    private static class ThemedSpinnerAdapter extends ArrayAdapter<String> {

        private AkitaTheme.Palette palette = AkitaTheme.darkPalette();

        ThemedSpinnerAdapter(Context context, List<String> items) {
            super(context, android.R.layout.simple_spinner_item, new ArrayList<>(items));
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        void setPalette(AkitaTheme.Palette palette) {
            this.palette = palette;
            notifyDataSetChanged();
        }

        void replaceItems(List<String> items) {
            clear();
            addAll(items);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            styleView(view, false);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            styleView(view, true);
            return view;
        }

        private void styleView(View view, boolean dropdown) {
            if (!(view instanceof TextView)) {
                return;
            }
            TextView textView = (TextView) view;
            textView.setTextColor(palette.textPrimary);
            textView.setTextSize(dropdown ? 15f : 14f);
            int verticalPadding = Math.round(textView.getResources().getDisplayMetrics().density * 10f);
            int horizontalPadding = Math.round(textView.getResources().getDisplayMetrics().density * 12f);
            textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            if (dropdown) {
                textView.setBackgroundColor(palette.surfaceElevated);
            } else {
                textView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        }
    }
    }
}
