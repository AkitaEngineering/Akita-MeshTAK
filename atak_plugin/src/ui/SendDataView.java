package com.akitaengineering.meshtak.ui;

import android.content.res.ColorStateList;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
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
import com.akitaengineering.meshtak.AkitaMissionControl;
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
    private final MapView mapView;
    private Spinner dataFormatSpinner;
    private Spinner missionTemplateSpinner;
    private Spinner incidentRoleActionSpinner;
    private EditText dataToSendEditText;
    private Button sendButton;
    private Button loadTemplateButton;
    private Button loadRoleActionButton;
    private Spinner commandHistorySpinner;
    private Button themeModeButton;
    private TextView routeChip;
    private TextView endpointChip;
    private TextView missionProfileChip;
    private TextView operationalSummaryTextView;
    private TextView payloadSummaryTextView;
    private TextView assuranceSummaryTextView;
    private TextView mailboxSummaryTextView;
    private TextView incidentBoardSummaryTextView;
    private TextView incidentTitleValueTextView;
    private TextView incidentRolePackValueTextView;
    private TextView incidentTempoValueTextView;
    private TextView incidentNextActionValueTextView;
    private TextView incidentRoleActionHintTextView;
    private TextView encryptionStatusValueTextView;
    private TextView auditStatusValueTextView;
    private TextView interoperabilityStatusValueTextView;
    private TextView provisioningStatusValueTextView;
    private TextView mailboxPendingValueTextView;
    private TextView mailboxInFlightValueTextView;
    private TextView mailboxDeliveredValueTextView;
    private TextView mailboxFailoverValueTextView;
    private TextView mailboxReplayValueTextView;
    private TextView templateHintTextView;
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
    private View assuranceCard;
    private View mailboxCard;
    private View incidentBoardCard;
    private View trendCard;
    private View formatCard;
    private View composerCard;
    private View historyCard;
    private View routeStatCard;
    private View payloadStatCard;
    private View lastSendStatCard;
    private View deliveryStatCard;
    private View assuranceEncryptionCard;
    private View assuranceAuditCard;
    private View assuranceInteroperabilityCard;
    private View assuranceProvisioningCard;
    private View mailboxPendingCard;
    private View mailboxInFlightCard;
    private View mailboxDeliveredCard;
    private View mailboxFailoverCard;
    private View mailboxReplayCard;
    private View incidentTitleCard;
    private View incidentRolePackCard;
    private View incidentTempoCard;
    private View incidentNextActionCard;
    private View plainLegendDot;
    private View jsonLegendDot;
    private View customLegendDot;
    private BLEService bleService;
    private SerialService serialService;
    private String connectionMethod;
    private Button retryMailboxButton;
    private Button replayMissionButton;
    private final List<String> commandHistory = new ArrayList<>();
    private final List<Integer> recentPayloadSizes = new ArrayList<>();
    private final List<String> dataFormats = new ArrayList<>();
    private final List<AkitaMissionProfile.TemplatePreset> templatePresets = new ArrayList<>();
    private final List<AkitaIncidentBoard.RoleAction> roleActions = new ArrayList<>();
    private final List<AkitaMissionControl.ReplayEvent> replayTimeline = new ArrayList<>();
    private ThemedSpinnerAdapter formatAdapter;
    private ThemedSpinnerAdapter templateAdapter;
    private ThemedSpinnerAdapter roleActionAdapter;
    private ThemedSpinnerAdapter historyAdapter;
    private final Context context;
    private final SharedPreferences preferences;
    private final AkitaMissionControl missionControl;
    private final Handler replayHandler = new Handler(Looper.getMainLooper());
    private final Runnable replayStepRunnable = this::advanceReplayStep;
    private static final int MAX_HISTORY = 20;
    private static final int MAX_PAYLOAD_BYTES = 512;
    private static final int MAX_TREND_POINTS = 12;
    private static final long REPLAY_STEP_DELAY_MS = 1400L;
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
    private boolean replayActive;
    private int replayIndex;

    public SendDataView(Context context, MapView mapView, BLEService bleService, SerialService serialService) {
        super(context);
        this.context = context;
        this.mapView = mapView;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.missionControl = AkitaMissionControl.getInstance(context);
        this.bleService = bleService;
        this.serialService = serialService;
        this.connectionMethod = preferences.getString("connection_method", "ble");
        setOrientation(VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.send_data_view, this, true);

        bindViews();
        loadPersistedState();
        setupFormatSpinner();
        setupMissionTemplateSpinner();
        setupRoleActionSpinner();
        setupHistorySpinner();
        setupComposerInteractions();
        updateThemeModeButton();
        refreshMissionTemplates();
        refreshRoleActions();
        refreshHistorySpinner();
        applyTheme();
        refreshDashboard();

        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void setServices(BLEService bleService, SerialService serialService) {
        this.bleService = bleService;
        this.serialService = serialService;
        flushPendingMailbox(false);
        refreshDashboard();
    }

    private void bindViews() {
        dataFormatSpinner = findViewById(R.id.data_format_spinner);
        missionTemplateSpinner = findViewById(R.id.mission_template_spinner);
        incidentRoleActionSpinner = findViewById(R.id.incident_role_action_spinner);
        dataToSendEditText = findViewById(R.id.data_to_send);
        sendButton = findViewById(R.id.send_button);
        loadTemplateButton = findViewById(R.id.load_template_button);
        loadRoleActionButton = findViewById(R.id.load_role_action_button);
        commandHistorySpinner = findViewById(R.id.command_history_spinner);
        themeModeButton = findViewById(R.id.theme_mode_button);
        routeChip = findViewById(R.id.route_chip);
        endpointChip = findViewById(R.id.endpoint_chip);
        missionProfileChip = findViewById(R.id.mission_profile_chip);
        operationalSummaryTextView = findViewById(R.id.operational_summary);
        payloadSummaryTextView = findViewById(R.id.payload_summary);
        assuranceSummaryTextView = findViewById(R.id.assurance_summary);
        mailboxSummaryTextView = findViewById(R.id.mailbox_summary);
        incidentBoardSummaryTextView = findViewById(R.id.incident_board_summary);
        incidentTitleValueTextView = findViewById(R.id.incident_title_value);
        incidentRolePackValueTextView = findViewById(R.id.incident_role_pack_value);
        incidentTempoValueTextView = findViewById(R.id.incident_tempo_value);
        incidentNextActionValueTextView = findViewById(R.id.incident_next_action_value);
        incidentRoleActionHintTextView = findViewById(R.id.incident_role_action_hint);
        encryptionStatusValueTextView = findViewById(R.id.assurance_encryption_value);
        auditStatusValueTextView = findViewById(R.id.assurance_audit_value);
        interoperabilityStatusValueTextView = findViewById(R.id.assurance_interop_value);
        provisioningStatusValueTextView = findViewById(R.id.assurance_provisioning_value);
        mailboxPendingValueTextView = findViewById(R.id.mailbox_pending_value);
        mailboxInFlightValueTextView = findViewById(R.id.mailbox_in_flight_value);
        mailboxDeliveredValueTextView = findViewById(R.id.mailbox_delivered_value);
        mailboxFailoverValueTextView = findViewById(R.id.mailbox_failover_value);
        mailboxReplayValueTextView = findViewById(R.id.mailbox_replay_value);
        templateHintTextView = findViewById(R.id.template_hint);
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
        assuranceCard = findViewById(R.id.assurance_card);
        mailboxCard = findViewById(R.id.mailbox_card);
        incidentBoardCard = findViewById(R.id.incident_board_card);
        trendCard = findViewById(R.id.trend_card);
        formatCard = findViewById(R.id.format_card);
        composerCard = findViewById(R.id.composer_card);
        historyCard = findViewById(R.id.history_card);
        routeStatCard = findViewById(R.id.stat_route_card);
        payloadStatCard = findViewById(R.id.stat_payload_card);
        lastSendStatCard = findViewById(R.id.stat_last_send_card);
        deliveryStatCard = findViewById(R.id.stat_delivery_card);
        assuranceEncryptionCard = findViewById(R.id.assurance_encryption_card);
        assuranceAuditCard = findViewById(R.id.assurance_audit_card);
        assuranceInteroperabilityCard = findViewById(R.id.assurance_interop_card);
        assuranceProvisioningCard = findViewById(R.id.assurance_provisioning_card);
        mailboxPendingCard = findViewById(R.id.mailbox_pending_card);
        mailboxInFlightCard = findViewById(R.id.mailbox_in_flight_card);
        mailboxDeliveredCard = findViewById(R.id.mailbox_delivered_card);
        mailboxFailoverCard = findViewById(R.id.mailbox_failover_card);
        mailboxReplayCard = findViewById(R.id.mailbox_replay_card);
        incidentTitleCard = findViewById(R.id.incident_title_card);
        incidentRolePackCard = findViewById(R.id.incident_role_pack_card);
        incidentTempoCard = findViewById(R.id.incident_tempo_card);
        incidentNextActionCard = findViewById(R.id.incident_next_action_card);
        plainLegendDot = findViewById(R.id.plain_legend_dot);
        jsonLegendDot = findViewById(R.id.json_legend_dot);
        customLegendDot = findViewById(R.id.custom_legend_dot);
        retryMailboxButton = findViewById(R.id.retry_mailbox_button);
        replayMissionButton = findViewById(R.id.replay_mission_button);
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

    private void setupMissionTemplateSpinner() {
        templateAdapter = new ThemedSpinnerAdapter(context, new ArrayList<>());
        missionTemplateSpinner.setAdapter(templateAdapter);
    }

    private void setupRoleActionSpinner() {
        roleActionAdapter = new ThemedSpinnerAdapter(context, new ArrayList<>());
        incidentRoleActionSpinner.setAdapter(roleActionAdapter);
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

        loadTemplateButton.setOnClickListener(v -> loadSelectedTemplate());
        loadRoleActionButton.setOnClickListener(v -> loadSelectedRoleAction());
        sendButton.setOnClickListener(v -> sendCurrentPayload());
        retryMailboxButton.setOnClickListener(v -> flushPendingMailbox(true));
        replayMissionButton.setOnClickListener(v -> toggleReplay());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        replayHandler.removeCallbacks(replayStepRunnable);
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("connection_method".equals(key)) {
            connectionMethod = sharedPreferences.getString(key, "ble");
            flushPendingMailbox(false);
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
            || AkitaMissionControl.PREF_MAILBOX_RECORDS.equals(key)
            || AkitaMissionControl.PREF_REPLAY_EVENTS.equals(key)
            || AkitaMissionControl.PREF_AUTO_FAILOVER.equals(key)
                || AkitaMockSettings.PREF_MOCK_BLE_STATUS.equals(key)
                || AkitaMockSettings.PREF_MOCK_SERIAL_STATUS.equals(key)
                || AkitaMockSettings.PREF_MOCK_BATTERY_LEVEL.equals(key)
                || AkitaMissionProfile.PREF_MISSION_PROFILE.equals(key)
                || AkitaProvisioningManager.PREF_PROVISIONING_SECRET.equals(key)
            || AkitaProvisioningManager.PREF_PROVISIONING_BUNDLE.equals(key)
                || AkitaProvisioningManager.PREF_ENCRYPTION_ENABLED.equals(key)
                || "ble_device_name".equals(key)
                || "serial_baud_rate".equals(key)
                || "serial_port_path".equals(key)) {
            refreshMissionTemplates();
            refreshRoleActions();
            refreshDashboard();
            refreshHistorySpinner();
        }
    }

    private byte[] formatData(String format, String data) {
        String normalizedData = data == null ? "" : data;
        if ("Plain Text".equals(format)) {
            normalizedData = normalizedData.trim();
        }
        return normalizedData.getBytes(StandardCharsets.UTF_8);
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
        String payload = new String(formatData(selectedFormat, data), StandardCharsets.UTF_8);
        int preparedBytes = AkitaMissionControl.getPreparedMailboxCommandBytes(selectedFormat, payload);
        if (preparedBytes > MAX_PAYLOAD_BYTES) {
            recordFailedSend();
            Toast.makeText(context, "Payload too large (max " + MAX_PAYLOAD_BYTES + " bytes).", Toast.LENGTH_SHORT).show();
            updatePayloadInsights();
            return;
        }

        missionControl.queueMessage(selectedFormat, payload, connectionMethod);
        AkitaMissionControl.DispatchBatchResult dispatchResult = flushPendingMailbox(false);

        addToCommandHistory(data);
        dataToSendEditText.getText().clear();
        if (dispatchResult.dispatchedCount > 0) {
            recordSuccessfulSend(selectedFormat, preparedBytes, dispatchResult.lastRoute);
        } else if (dispatchResult.anyFailures && dispatchResult.queuedCount == 0) {
            recordFailedSend();
        }

        Toast.makeText(context, dispatchResult.summary, Toast.LENGTH_SHORT).show();
        refreshDashboard();
    }

    private void recordSuccessfulSend(String format, int payloadBytes, String routeUsed) {
        successfulSends++;
        lastSendAt = System.currentTimeMillis();
        lastSendBytes = payloadBytes;
        lastSendFormat = format;
        lastSendRoute = TextUtils.isEmpty(routeUsed) ? connectionMethod : routeUsed;

        if ("ble".equalsIgnoreCase(lastSendRoute)) {
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
        AkitaMissionControl.QueueSnapshot mailboxSnapshot = missionControl.getQueueSnapshot(AkitaMissionControl.isAutoFailoverEnabled(preferences));
        String routeLabel = "ble".equals(connectionMethod) ? "BLE" : "Serial";
        routeChip.setText((mockModeEnabled ? "Simulated route: " : "Active route: ") + routeLabel);
        endpointChip.setText(buildEndpointDescription());
        missionProfileChip.setText("Profile: " + AkitaMissionProfile.getProfileBadge(preferences));
        operationalSummaryTextView.setText(buildOperationalSummary());
        assuranceSummaryTextView.setText(AkitaOperationalReadiness.getAssuranceSummary(context, isActiveTransportAttached(), mapView != null));
        refreshIncidentBoard();
        refreshMailboxCard(mailboxSnapshot);
        activeRouteValueTextView.setText(routeLabel);
        long displayLastSendAt = getDisplayLastSendAt();
        lastSendValueTextView.setText(displayLastSendAt > 0 ? formatRelativeTime(displayLastSendAt) : "Never");
        deliveryRatioValueTextView.setText(buildDeliveryRatio(mailboxSnapshot));
        lastOperationTextView.setText(buildLastOperationText(mailboxSnapshot));
        historyCaptionTextView.setText(buildHistoryCaption());

        setAssuranceStatus(encryptionStatusValueTextView,
            AkitaOperationalReadiness.getEncryptionStatus(context, isActiveTransportAttached()));
        setAssuranceStatus(auditStatusValueTextView,
            AkitaOperationalReadiness.getAuditStatus(context));
        setAssuranceStatus(interoperabilityStatusValueTextView,
            AkitaOperationalReadiness.getInteroperabilityStatus(context, isActiveTransportAttached(), mapView != null));
        setAssuranceStatus(provisioningStatusValueTextView,
            AkitaOperationalReadiness.getProvisioningStatus(context));

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
            payloadSummaryTextView.setText("Compose a mailbox frame to verify how it fits inside the field radio envelope.");
            payloadMetricsTextView.setText("Draft size: 0 / 512 bytes");
            payloadMetricsTextView.setTextColor(palette.textSecondary);
            return;
        }

        if (payloadBytes > MAX_PAYLOAD_BYTES) {
            payloadSummaryTextView.setText(String.format(Locale.US, "Mailbox frame exceeds the safe payload ceiling by %d bytes.", payloadBytes - MAX_PAYLOAD_BYTES));
        } else if (payloadBytes >= (MAX_PAYLOAD_BYTES * 0.75f)) {
            payloadSummaryTextView.setText("Mailbox frame is nearing the radio payload ceiling. Consider trimming before release.");
        } else {
            payloadSummaryTextView.setText("Mailbox frame fits comfortably inside the radio payload envelope.");
        }

        payloadMetricsTextView.setText(String.format(Locale.US,
                "Prepared mailbox frame: %s • %d B • %d B remaining",
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
        String payload = new String(formatData(getSelectedFormat(), draft), StandardCharsets.UTF_8);
        return AkitaMissionControl.getPreparedMailboxCommandBytes(getSelectedFormat(), payload);
    }

    private String getSelectedFormat() {
        Object selectedItem = dataFormatSpinner.getSelectedItem();
        if (selectedItem == null) {
            return dataFormats.isEmpty() ? "Plain Text" : dataFormats.get(0);
        }
        return selectedItem.toString();
    }

    private void refreshMissionTemplates() {
        templatePresets.clear();
        templatePresets.addAll(AkitaMissionProfile.getTemplatePresets(preferences));

        List<String> templateLabels = new ArrayList<>();
        if (templatePresets.isEmpty()) {
            templateLabels.add(context.getString(R.string.template_spinner_empty));
            missionTemplateSpinner.setEnabled(false);
            loadTemplateButton.setEnabled(false);
        } else {
            for (AkitaMissionProfile.TemplatePreset preset : templatePresets) {
                templateLabels.add(preset.label);
            }
            missionTemplateSpinner.setEnabled(true);
            loadTemplateButton.setEnabled(true);
        }

        templateAdapter.replaceItems(templateLabels);
        missionTemplateSpinner.setSelection(0, false);
        missionTemplateSpinner.setAlpha(missionTemplateSpinner.isEnabled() ? 1f : 0.65f);
        loadTemplateButton.setAlpha(loadTemplateButton.isEnabled() ? 1f : 0.65f);
        templateHintTextView.setText(AkitaMissionProfile.getTemplateHint(preferences));
    }

    private void refreshRoleActions() {
        roleActions.clear();
        roleActions.addAll(AkitaIncidentBoard.getRoleActions(preferences));

        List<String> actionLabels = new ArrayList<>();
        if (roleActions.isEmpty()) {
            actionLabels.add(context.getString(R.string.role_action_spinner_empty));
            incidentRoleActionSpinner.setEnabled(false);
            loadRoleActionButton.setEnabled(false);
        } else {
            for (AkitaIncidentBoard.RoleAction roleAction : roleActions) {
                actionLabels.add(roleAction.label);
            }
            incidentRoleActionSpinner.setEnabled(true);
            loadRoleActionButton.setEnabled(true);
        }

        roleActionAdapter.replaceItems(actionLabels);
        incidentRoleActionSpinner.setSelection(0, false);
        incidentRoleActionSpinner.setAlpha(incidentRoleActionSpinner.isEnabled() ? 1f : 0.65f);
        loadRoleActionButton.setAlpha(loadRoleActionButton.isEnabled() ? 1f : 0.65f);
    }

    private void refreshIncidentBoard() {
        AkitaIncidentBoard.IncidentState incidentState = AkitaIncidentBoard.getState(preferences);
        incidentBoardSummaryTextView.setText(incidentState.summary);
        incidentTitleValueTextView.setText(incidentState.title);
        incidentRolePackValueTextView.setText(incidentState.rolePack);
        incidentTempoValueTextView.setText(incidentState.tempo);
        incidentNextActionValueTextView.setText(incidentState.nextAction);
        incidentRoleActionHintTextView.setText("Queue " + incidentState.rolePack + " actions into the secure composer.");
    }

    private void loadSelectedTemplate() {
        if (templatePresets.isEmpty()) {
            return;
        }

        int selectedIndex = missionTemplateSpinner.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= templatePresets.size()) {
            selectedIndex = 0;
        }

        AkitaMissionProfile.TemplatePreset preset = templatePresets.get(selectedIndex);
        int formatIndex = dataFormats.indexOf(preset.format);
        if (formatIndex >= 0) {
            dataFormatSpinner.setSelection(formatIndex, false);
        }
        dataToSendEditText.setText(preset.payload);
        dataToSendEditText.setSelection(preset.payload.length());
        updatePayloadInsights();
        Toast.makeText(context, "Loaded playbook: " + preset.label, Toast.LENGTH_SHORT).show();
    }

    private void loadSelectedRoleAction() {
        if (roleActions.isEmpty()) {
            return;
        }

        int selectedIndex = incidentRoleActionSpinner.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= roleActions.size()) {
            selectedIndex = 0;
        }

        AkitaIncidentBoard.RoleAction roleAction = roleActions.get(selectedIndex);
        int formatIndex = dataFormats.indexOf(roleAction.format);
        if (formatIndex >= 0) {
            dataFormatSpinner.setSelection(formatIndex, false);
        }
        dataToSendEditText.setText(roleAction.payload);
        dataToSendEditText.setSelection(roleAction.payload.length());
        updatePayloadInsights();
        Toast.makeText(context, "Queued role action: " + roleAction.label, Toast.LENGTH_SHORT).show();
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
        String profileLabel = AkitaMissionProfile.getProfileLabel(preferences);
        String missionFocus = AkitaMissionProfile.getOperationalFocus(preferences);
        if (AkitaMockSettings.isEnabled(preferences)) {
            if ("ble".equals(connectionMethod)) {
                String deviceName = preferences.getString("ble_device_name", "AkitaNode01");
                return profileLabel + " playbook is active in mock mode. BLE route is being simulated for " + deviceName + " so teams can validate encrypted ATAK interoperability and " + missionFocus + " without hardware.";
            }
            String baudRate = preferences.getString("serial_baud_rate", "115200");
            String portPath = preferences.getString("serial_port_path", "/dev/ttyUSB0");
            return profileLabel + " playbook is active in mock mode. Serial route is being simulated on " + portPath + " at " + baudRate + " baud so teams can rehearse " + missionFocus + " without hardware.";
        }
        if ("ble".equals(connectionMethod)) {
            String deviceName = preferences.getString("ble_device_name", "AkitaNode01");
            if (bleService != null) {
                return profileLabel + " posture is armed over BLE for " + deviceName + ". Secure ATAK and partner traffic will route over the mesh radio for " + missionFocus + ".";
            }
            return profileLabel + " posture is staged over BLE for " + deviceName + ". Waiting for the transport service to attach before release.";
        }

        String baudRate = preferences.getString("serial_baud_rate", "115200");
        String portPath = preferences.getString("serial_port_path", "/dev/ttyUSB0");
        if (serialService != null) {
            return profileLabel + " posture is armed on serial route " + portPath + " at " + baudRate + " baud for " + missionFocus + " and partner-system interoperability.";
        }
        return profileLabel + " posture is staged on serial route " + portPath + " at " + baudRate + " baud. Waiting for the transport service to attach.";
    }

    private String buildDeliveryRatio(AkitaMissionControl.QueueSnapshot mailboxSnapshot) {
        int attempts = mailboxSnapshot.deliveredCount + mailboxSnapshot.failedCount;
        if (attempts <= 0) {
            attempts = getDisplaySuccessfulSends() + getDisplayFailedSends();
        }
        if (attempts <= 0) {
            return "Awaiting";
        }
        int successes = mailboxSnapshot.deliveredCount > 0 || mailboxSnapshot.failedCount > 0
                ? mailboxSnapshot.deliveredCount
                : getDisplaySuccessfulSends();
        int ratio = Math.round((successes * 100f) / attempts);
        return ratio + "%";
    }

    private String buildLastOperationText(AkitaMissionControl.QueueSnapshot mailboxSnapshot) {
        if (replayActive && !replayTimeline.isEmpty()) {
            int currentStep = Math.min(replayIndex, replayTimeline.size());
            return String.format(Locale.US,
                    "Digital twin replay active • checkpoint %d of %d.",
                    currentStep,
                    replayTimeline.size());
        }
        if (mailboxSnapshot.pendingCount > 0 || mailboxSnapshot.inFlightCount > 0) {
            return mailboxSnapshot.summary;
        }
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
        applyPanel(assuranceCard, AkitaTheme.createPanelDrawable(context, palette, true));
        applyPanel(mailboxCard, AkitaTheme.createPanelDrawable(context, palette, true));
        applyPanel(incidentBoardCard, AkitaTheme.createPanelDrawable(context, palette, true));
        applyPanel(trendCard, AkitaTheme.createPanelDrawable(context, palette, true));
        applyPanel(formatCard, AkitaTheme.createPanelDrawable(context, palette, true));
        applyPanel(composerCard, AkitaTheme.createPanelDrawable(context, palette, true));
        applyPanel(historyCard, AkitaTheme.createPanelDrawable(context, palette, true));

        applyPanel(routeStatCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(payloadStatCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(lastSendStatCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(deliveryStatCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(assuranceEncryptionCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(assuranceAuditCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(assuranceInteroperabilityCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(assuranceProvisioningCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(mailboxPendingCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(mailboxInFlightCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(mailboxDeliveredCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(mailboxFailoverCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(mailboxReplayCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(incidentTitleCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(incidentRolePackCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(incidentTempoCard, AkitaTheme.createStatTileDrawable(context, palette));
        applyPanel(incidentNextActionCard, AkitaTheme.createStatTileDrawable(context, palette));

        routeChip.setBackground(AkitaTheme.createBadgeDrawable(context, palette, true));
        routeChip.setTextColor(palette.white);
        endpointChip.setBackground(AkitaTheme.createBadgeDrawable(context, palette, false));
        endpointChip.setTextColor(palette.textPrimary);
        missionProfileChip.setBackground(AkitaTheme.createBadgeDrawable(context, palette, false));
        missionProfileChip.setTextColor(palette.textPrimary);

        sendButton.setBackground(AkitaTheme.createAccentButtonDrawable(context, palette));
        sendButton.setTextColor(palette.white);
        loadTemplateButton.setBackground(AkitaTheme.createStatTileDrawable(context, palette));
        loadTemplateButton.setTextColor(palette.textPrimary);
        loadRoleActionButton.setBackground(AkitaTheme.createStatTileDrawable(context, palette));
        loadRoleActionButton.setTextColor(palette.textPrimary);
        retryMailboxButton.setBackground(AkitaTheme.createStatTileDrawable(context, palette));
        retryMailboxButton.setTextColor(palette.textPrimary);
        replayMissionButton.setBackground(AkitaTheme.createAccentButtonDrawable(context, palette));
        replayMissionButton.setTextColor(palette.white);

        dataToSendEditText.setBackground(AkitaTheme.createInputDrawable(context, palette));
        dataToSendEditText.setTextColor(palette.textPrimary);
        dataToSendEditText.setHintTextColor(palette.textMuted);

        applySpinnerStyle(dataFormatSpinner, palette);
        applySpinnerStyle(missionTemplateSpinner, palette);
        applySpinnerStyle(incidentRoleActionSpinner, palette);
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
                R.id.assurance_card_title,
            R.id.mailbox_card_title,
                R.id.incident_board_title,
                R.id.stat_active_route_value,
                R.id.stat_payload_value,
                R.id.stat_last_send_value,
                R.id.stat_delivery_value,
                R.id.assurance_encryption_value,
                R.id.assurance_audit_value,
                R.id.assurance_interop_value,
                R.id.assurance_provisioning_value,
            R.id.mailbox_pending_value,
            R.id.mailbox_in_flight_value,
            R.id.mailbox_delivered_value,
            R.id.mailbox_failover_value,
            R.id.mailbox_replay_value,
                R.id.incident_title_value,
                R.id.incident_role_pack_value,
                R.id.incident_tempo_value,
                R.id.incident_next_action_value,
                R.id.trend_card_title,
                R.id.format_card_title,
                R.id.composer_card_title,
                R.id.history_card_title);

        setTextColors(palette.textSecondary,
                R.id.dashboard_subtitle,
                R.id.operational_summary,
                R.id.payload_summary,
                R.id.assurance_summary,
            R.id.mailbox_summary,
                R.id.incident_board_summary,
                R.id.last_operation_text,
                R.id.trend_card_subtitle,
                R.id.format_card_subtitle,
                R.id.composer_card_subtitle,
                R.id.template_hint,
                R.id.incident_role_action_hint,
                R.id.history_caption,
                R.id.payload_metrics,
                R.id.plain_legend_text,
                R.id.json_legend_text,
                R.id.custom_legend_text,
                R.id.stat_route_label,
                R.id.stat_payload_label,
                R.id.stat_last_send_label,
                R.id.stat_delivery_label,
                R.id.assurance_encryption_label,
                R.id.assurance_audit_label,
                R.id.assurance_interop_label,
                R.id.assurance_provisioning_label,
                R.id.mailbox_pending_label,
                R.id.mailbox_in_flight_label,
                R.id.mailbox_delivered_label,
                R.id.mailbox_failover_label,
                R.id.mailbox_replay_label,
                R.id.incident_title_label,
                R.id.incident_role_pack_label,
                R.id.incident_tempo_label,
                R.id.incident_next_action_label);

        formatAdapter.setPalette(palette);
        templateAdapter.setPalette(palette);
        roleActionAdapter.setPalette(palette);
        historyAdapter.setPalette(palette);
        refreshMissionTemplates();
        refreshRoleActions();
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

    private AkitaMissionControl.DispatchBatchResult flushPendingMailbox(boolean showToast) {
        boolean mockMode = AkitaMockSettings.isEnabled(preferences);
        AkitaMissionControl.DispatchBatchResult result = missionControl.dispatchPendingMessages(
                buildRouteSender(mockMode),
                AkitaMissionControl.isAutoFailoverEnabled(preferences),
                mockMode);
        if (showToast) {
            Toast.makeText(context, result.summary, Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    private void refreshMailboxCard(AkitaMissionControl.QueueSnapshot snapshot) {
        mailboxSummaryTextView.setText(replayActive
                ? String.format(Locale.US, "Digital twin replay is active with %d checkpoint(s) loaded.", replayTimeline.size())
                : snapshot.summary);
        mailboxPendingValueTextView.setText(String.valueOf(snapshot.pendingCount));
        mailboxInFlightValueTextView.setText(String.valueOf(snapshot.inFlightCount));
        mailboxDeliveredValueTextView.setText(String.valueOf(snapshot.deliveredCount));
        mailboxFailoverValueTextView.setText(snapshot.failoverSummary);
        mailboxReplayValueTextView.setText(String.format(Locale.US,
                "%d checkpoints • %s",
                snapshot.replayCheckpointCount,
                snapshot.lastEventSummary));

        boolean hasRetryWork = snapshot.pendingCount > 0 || snapshot.failedCount > 0;
        retryMailboxButton.setEnabled(hasRetryWork);
        retryMailboxButton.setAlpha(hasRetryWork ? 1f : 0.65f);

        boolean hasReplayData = replayActive || !missionControl.getReplayableTimeline().isEmpty();
        replayMissionButton.setEnabled(hasReplayData);
        replayMissionButton.setAlpha(hasReplayData ? 1f : 0.65f);
        replayMissionButton.setText(replayActive ? "Stop Replay" : "Replay Last Mission");
    }

    private void toggleReplay() {
        if (replayActive) {
            stopReplay(false);
            refreshDashboard();
            return;
        }

        if (!AkitaMockSettings.isEnabled(preferences)) {
            Toast.makeText(context, "Enable Mock Transport Mode to rehearse the last mission timeline.", Toast.LENGTH_SHORT).show();
            return;
        }

        replayTimeline.clear();
        replayTimeline.addAll(missionControl.getReplayableTimeline());
        if (replayTimeline.isEmpty()) {
            Toast.makeText(context, "No replay checkpoints are available yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        replayActive = true;
        replayIndex = 0;
        replayHandler.removeCallbacks(replayStepRunnable);
        advanceReplayStep();
    }

    private void advanceReplayStep() {
        if (!replayActive) {
            return;
        }
        if (replayIndex >= replayTimeline.size()) {
            stopReplay(true);
            return;
        }

        AkitaMissionControl.ReplayEvent event = replayTimeline.get(replayIndex);
        replayIndex++;
        applyReplayEvent(event);
        refreshDashboard();
        replayHandler.postDelayed(replayStepRunnable, REPLAY_STEP_DELAY_MS);
    }

    private void stopReplay(boolean completed) {
        replayActive = false;
        replayHandler.removeCallbacks(replayStepRunnable);
        if (completed) {
            Toast.makeText(context, "Mission replay completed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyReplayEvent(AkitaMissionControl.ReplayEvent event) {
        int formatIndex = dataFormats.indexOf(event.format);
        if (formatIndex >= 0) {
            dataFormatSpinner.setSelection(formatIndex, false);
        }
        if (!TextUtils.isEmpty(event.payload)) {
            dataToSendEditText.setText(event.payload);
            dataToSendEditText.setSelection(event.payload.length());
        }
        updatePayloadInsights();
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

    private void setAssuranceStatus(TextView view, String status) {
        AkitaTheme.Palette palette = AkitaTheme.resolvePalette(context);
        view.setText(status);
        if (palette.monochrome) {
            view.setTextColor(palette.textPrimary);
            return;
        }

        String normalizedStatus = status == null ? "" : status.toLowerCase(Locale.US);
        int color = palette.textPrimary;
        if (normalizedStatus.contains("rotate")
                || normalizedStatus.contains("failed")
                || normalizedStatus.contains("disabled")
                || normalizedStatus.contains("degraded")) {
            color = palette.danger;
        } else if (normalizedStatus.contains("pending")
                || normalizedStatus.contains("standby")
                || normalizedStatus.contains("staged")
                || normalizedStatus.contains("awaiting")) {
            color = palette.warning;
        } else if (normalizedStatus.contains("active")
                || normalizedStatus.contains("live")
                || normalizedStatus.contains("loaded")
                || normalizedStatus.contains("rehearsed")
                || normalizedStatus.contains("simulated")
                || normalizedStatus.contains("ready")) {
            color = palette.success;
        }
        view.setTextColor(color);
    }

    private boolean isActiveTransportAttached() {
        if (AkitaMockSettings.isEnabled(preferences)) {
            return true;
        }
        return "ble".equals(connectionMethod) ? bleService != null : serialService != null;
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
