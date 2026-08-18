// File: atak_plugin/src/services/SerialService.java
// Description: Handles USB Serial communication, device discovery, health checks, and ATAK marker updates.
package com.akitaengineering.meshtak.services;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.akitaengineering.meshtak.AkitaMissionControl;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.akitaengineering.meshtak.ui.AkitaMissionMarkerRegistry;
import com.akitaengineering.meshtak.ui.AkitaProvisioningManager;
import com.akitaengineering.meshtak.ui.AkitaToolbar;
import com.akitaengineering.meshtak.Config;
import com.akitaengineering.meshtak.DeviceSecurityState;
import com.akitaengineering.meshtak.AuditLogger;
import com.akitaengineering.meshtak.PayloadEnvelope;
import com.akitaengineering.meshtak.SecurityManager;
import com.akitaengineering.meshtak.SerialLineAccumulator;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SerialService extends Service implements SerialInputOutputManager.Listener {

    private static final String TAG = "SerialService";
    private final IBinder binder = new LocalBinder();
    private MapView mapView;
    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private SerialInputOutputManager ioManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int baudRate;
    private AkitaToolbar akitaToolbar;
    private SerialStatusListener serialStatusListener;
    private volatile String serialConnectionStatus = "Idle";
    private boolean serialPortOpen;
    private SecurityManager securityManager;
    private AuditLogger auditLogger;
    private final SerialLineAccumulator lineAccumulator = new SerialLineAccumulator(4096);
    private final Object serialWriteLock = new Object();
    private volatile boolean destroyed;

    // Constants read from Config.java
    private static final int HELTEC_VENDOR_ID = Config.HELTEC_VENDOR_ID;
    private static final int HELTEC_PRODUCT_ID = Config.HELTEC_PRODUCT_ID;

    private static final String ACTION_USB_PERMISSION = "com.akitaengineering.meshtak.USB_PERMISSION";
    private static final long RECONNECT_DELAY = 5000;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private int reconnectAttemptCount = 0;
    private static final long OPEN_SERIAL_TIMEOUT = 10000;
    private static final long HEALTH_CHECK_INTERVAL = 30000;

    private final Handler handler = new Handler();

    public interface SerialStatusListener {
        void onSerialStatusChanged(String status);
    }

    private final Runnable healthCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (destroyed) return;
            if (serialConnectionStatus.equals("Connected")) {
                queryDeviceStatus(Config.CMD_GET_BATT);
            }
            handler.postDelayed(this, HEALTH_CHECK_INTERVAL);
        }
    };

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.i(TAG, "USB permission granted for device: " + device.getDeviceName());
                            executorService.execute(SerialService.this::openSerialPortWithTimeout);
                        }
                    } else {
                        Log.e(TAG, "USB permission denied for device: " + device);
                        updateStatus("Error: USB permission denied");
                    }
                }
            }
        }
    };

    private final BroadcastReceiver usbDetachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                return;
            }
            UsbDevice detachedDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (detachedDevice != null && serialPort != null
                    && detachedDevice.getDeviceId() == serialPort.getDriver().getDevice().getDeviceId()) {
                Log.i(TAG, "USB device detached: " + detachedDevice.getDeviceName());
                stopIoManager();
                closeSerialPort();
                updateStatus("Disconnected");
                reconnectAttemptCount = 0;

                if (auditLogger != null) {
                    auditLogger.log(AuditLogger.EventType.DISCONNECTION, AuditLogger.Severity.INFO,
                            "Serial", "USB device detached", true);
                }
                scheduleReconnect();
            }
        }
    };
    // --- Service Lifecycle and Setup ---
    @SuppressLint("UnspecifiedRegisterReceiverFlag") // API <33 has no receiver-export flag overload.
    @Override
    public void onCreate() {
        super.onCreate();

        loadPreferences();

        // Initialize security and audit logging
        securityManager = SecurityManager.getInstance();
        auditLogger = AuditLogger.getInstance();
        auditLogger.initialize(getApplicationContext());
        initializeSecurity();

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter permissionFilter = new IntentFilter(ACTION_USB_PERMISSION);
        IntentFilter detachFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, permissionFilter, Context.RECEIVER_NOT_EXPORTED);
            registerReceiver(usbDetachReceiver, detachFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(usbReceiver, permissionFilter);
            registerReceiver(usbDetachReceiver, detachFilter);
        }
        findAndOpenHeltecSerialPortWithRetry();
        handler.post(healthCheckRunnable);

        auditLogger.log(AuditLogger.EventType.CONNECTION, AuditLogger.Severity.INFO,
                       "SerialService", "Service created", true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(usbReceiver);
        unregisterReceiver(usbDetachReceiver);
        stopIoManager();
        closeSerialPort();
        executorService.shutdown();
        super.onDestroy();
    }

    // --- Helper Methods ---

    private void loadPreferences() {
        // Loads baud rate from preferences, needed before connection attempts
        String configuredBaud = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .getString("serial_baud_rate", "115200");
        try {
            baudRate = Integer.parseInt(configuredBaud);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid serial_baud_rate preference: " + configuredBaud + ", falling back to 115200");
            baudRate = 115200;
        }
        Log.i(TAG, "Using baud rate: " + baudRate);
    }

    private void initializeSecurity() {
        SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        String deviceId = preferences.getString("ble_device_name", "AkitaNode01");
        String provisioningSecret = AkitaProvisioningManager.getActiveProvisioningSecret(this);
        String previousSecret = AkitaProvisioningManager.getPreviousProvisioningSecret(this);
        String keyId = AkitaProvisioningManager.getActiveKeyId(this);

        securityManager.reset();
        if (!securityManager.initializeFromProvisioning(
                deviceId,
                provisioningSecret,
                keyId,
                previousSecret,
                Config.nextKeyId(keyId))) {
            Log.e(TAG, "Failed to initialize security manager");
            auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                    "SerialService", "Security initialization failed", false);
            return;
        }

        securityManager.setEncryptionEnabled(true);
        auditLogger.log(AuditLogger.EventType.CONFIGURATION_CHANGE, AuditLogger.Severity.INFO,
                "SerialService", "Security initialized", true);
    }

    public void reloadSecurityConfiguration() {
        loadPreferences();
        initializeSecurity();
    }

    private void updateStatus(final String status) {
        serialConnectionStatus = status;
        handler.post(() -> {
            if (serialStatusListener != null) serialStatusListener.onSerialStatusChanged(status);
            if (akitaToolbar != null) akitaToolbar.setDetailedSerialStatus(status);
        });
    }

    private void findAndOpenHeltecSerialPortWithRetry() {
        if (destroyed) return;
        if (reconnectAttemptCount >= MAX_RECONNECT_ATTEMPTS && MAX_RECONNECT_ATTEMPTS > 0) {
            Log.w(TAG, "Max serial reconnect attempts reached.");
            updateStatus("Error: Max reconnect attempts");
            return;
        }
        updateStatus("Connecting (Attempt " + (reconnectAttemptCount + 1) + ")");
        handler.postDelayed(() -> {
            if (!destroyed) executorService.execute(this::findAndOpenHeltecSerialPortInternal);
        }, RECONNECT_DELAY);
        reconnectAttemptCount++;
    }

    private void findAndOpenHeltecSerialPortInternal() {
        if (destroyed) return;
        UsbSerialProber prober = UsbSerialProber.getDefaultProber();
        List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);

        for (UsbSerialDriver driver : availableDrivers) {
            UsbDevice device = driver.getDevice();
            if (device.getVendorId() == HELTEC_VENDOR_ID && device.getProductId() == HELTEC_PRODUCT_ID) {
                List<UsbSerialPort> ports = driver.getPorts();
                if (!ports.isEmpty()) {
                    serialPort = ports.get(0);
                    UsbDeviceConnection connection = usbManager.openDevice(device);
                    if (connection == null) {
                        Intent permissionRequest = new Intent(ACTION_USB_PERMISSION).setPackage(getPackageName());
                        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                                this, 0, permissionRequest, PendingIntent.FLAG_IMMUTABLE);
                        usbManager.requestPermission(device, permissionIntent);
                        return;
                    }
                    openSerialPort(connection);
                    return;
                }
            }
        }
        Log.w(TAG, "Heltec V3 serial port not found. Retrying...");
        updateStatus("Disconnected");
        scheduleReconnect();
    }

    private void openSerialPortWithTimeout() {
        if (destroyed) return;
        // This is called after permission is granted, so we search again quickly
        UsbSerialProber prober = UsbSerialProber.getDefaultProber();
        List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);
        for (UsbSerialDriver driver : availableDrivers) {
             UsbDevice device = driver.getDevice();
            if (device.getVendorId() == HELTEC_VENDOR_ID && device.getProductId() == HELTEC_PRODUCT_ID) {
                List<UsbSerialPort> ports = driver.getPorts();
                if (!ports.isEmpty()) {
                    serialPort = ports.get(0);
                    UsbDeviceConnection connection = usbManager.openDevice(device);
                    if (connection != null) {
                         openSerialPort(connection);
                         return;
                    }
                }
            }
        }
        updateStatus("Error: Heltec not found");
        scheduleReconnect();
    }

    private void openSerialPort(UsbDeviceConnection connection) {
        if (destroyed) {
            connection.close();
            return;
        }
        // Setup timeout to prevent hanging if open fails silently
        handler.postDelayed(() -> {
            if (ioManager == null && serialPort != null && serialPortOpen) {
                Log.w(TAG, "Serial port open timed out.");
                closeSerialPort();
                updateStatus("Error: Open timed out");
                scheduleReconnect();
            }
        }, OPEN_SERIAL_TIMEOUT);

        try {
            serialPort.open(connection);
            serialPort.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            serialPortOpen = true;
            startIoManager();
                    Log.i(TAG, "Serial port opened for Heltec V3");
            updateStatus("Connected");
            reconnectAttemptCount = 0; // Success! Reset count

            // Audit log connection
            if (auditLogger != null) {
                auditLogger.log(AuditLogger.EventType.CONNECTION, AuditLogger.Severity.INFO,
                               "Serial", "Serial port connected", true);
            }
            syncRuntimeState();
        } catch (IOException e) {
            Log.e(TAG, "Error opening serial port: " + e.getMessage(), e);
            closeSerialPort();
            updateStatus("Error: " + e.getMessage());
             scheduleReconnect();
        }
    }

    private void startIoManager() {
        if (serialPort != null) {
            stopIoManager();
            ioManager = new SerialInputOutputManager(serialPort, this);
            executorService.submit(ioManager);
            Log.i(TAG, "Started IO Manager");
        }
    }

    private void stopIoManager() {
        if (ioManager != null) {
            Log.i(TAG, "Stopping IO Manager");
            ioManager.stop();
            ioManager = null;
        }
    }

    private void closeSerialPort() {
        if (serialPort != null) {
            try {
                serialPort.close();
                Log.i(TAG, "Serial port closed.");
            } catch (IOException e) {
                Log.e(TAG, "Error closing serial port: " + e.getMessage(), e);
            }
            serialPort = null;
        }
        serialPortOpen = false;
        lineAccumulator.reset();
    }

    // --- Interface Implementations ---
    @Override
    public void onRunError(Exception e) {
        Log.e(TAG, "Serial I/O error: " + e.getMessage(), e);
        stopIoManager();
        closeSerialPort();
        updateStatus("Error: I/O - " + e.getMessage());
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (!destroyed) {
            handler.postDelayed(SerialService.this::findAndOpenHeltecSerialPortWithRetry, RECONNECT_DELAY);
        }
    }

    @Override
    public void onNewData(byte[] data) {
        if (destroyed || data == null || data.length == 0) {
            return;
        }

        for (String received : lineAccumulator.accept(data)) {
            processSerialLine(received);
        }
    }

    private void processSerialLine(String received) {
        if (!received.startsWith(Config.ENCRYPTED_PAYLOAD_PREFIX)) {
            Log.d(TAG, "Ignoring non-protocol serial line");
            return;
        }
        String decodedPayload = decodePayload(received);
        if (decodedPayload == null) {
            if (auditLogger != null) {
                auditLogger.log(AuditLogger.EventType.AUTHENTICATION_FAILURE, AuditLogger.Severity.WARNING,
                               "Serial", "Failed to decode encrypted payload", false);
            }
            return;
        }

        Log.i(TAG, "Received protocol line, len=" + received.length());

        // Audit log data reception
        if (auditLogger != null) {
            auditLogger.log(AuditLogger.EventType.DATA_RECEIVED, AuditLogger.Severity.INFO,
                           "Serial", "Data received, len: " + received.length(), true);
        }

        handler.post(() -> processDecodedSerialPayload(decodedPayload));
    }

    private void processDecodedSerialPayload(String decodedPayload) {
        if (AkitaMissionControl.getInstance(getApplicationContext()).consumeIncomingStatus(decodedPayload, AkitaMissionControl.ROUTE_SERIAL)) {
            return;
        }

        if (consumeRuntimeStatus(decodedPayload)) {
            return;
        }

        processCotData(decodedPayload);
    }

    // --- Data Processing (Robustness Fix) ---

    private void processCotData(String data) {
        if (mapView == null) return;

        // 1. Check for status prefixes (Health Monitoring)
        if (data.startsWith(Config.STATUS_BATT_PREFIX)) {
            String status = data.substring(Config.STATUS_BATT_PREFIX.length()).trim();
            if (akitaToolbar != null) akitaToolbar.setBatteryStatus(status);
            return;
        }

        // 2. Validate data framing (Robustness Check)
        String cleanData = data.trim();
        if (!cleanData.startsWith("<event") || !cleanData.endsWith("</event>")) {
            Log.w(TAG, "Received fragmented or non-CoT data (ignoring), len=" + cleanData.length());
            return;
        }

        // 3. Process CoT (ATAK Marker Logic)
        try {
            CotEvent cotEvent = CotEvent.parse(cleanData);
            if (cotEvent == null || !cotEvent.isValid()) return;

            final String uid = cotEvent.getUID();
            CotDetail contactDetail = cotEvent.findDetail("contact");
            String callsign = contactDetail != null ? contactDetail.getAttribute("callsign") : null;
            final GeoPoint geoPoint = cotEvent.getGeoPoint();

            if (uid == null) return;

            MapItem mapItem = mapView.getMapItem(uid);

            if (mapItem == null) {
                final Marker marker = new Marker(geoPoint, uid);
                marker.setTitle(callsign != null ? callsign : uid);
                marker.setType(cotEvent.getType() != null ? cotEvent.getType() : Config.DEFAULT_COT_TYPE);

                mapView.getRootGroup().addItem(marker);
            } else if (mapItem instanceof Marker) {
                Marker marker = (Marker) mapItem;
                marker.setPoint(geoPoint);
            }

            AkitaMissionMarkerRegistry.getInstance().recordMarker(
                    uid,
                    callsign != null ? callsign : uid,
                    geoPoint.getLatitude(),
                    geoPoint.getLongitude(),
                    "Serial");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing CoT data from serial: " + e.getMessage(), e);
        }
    }

    // --- Public Service Interface Methods ---

    public void queryDeviceStatus(String command) {
        sendData((command + "\n").getBytes());
    }

    public void syncRuntimeState() {
        if (!isReadyForTraffic()) {
            return;
        }
        long epochSeconds = System.currentTimeMillis() / 1000L;
        sendData((Config.CMD_TIME_SYNC_PREFIX + epochSeconds + "\n").getBytes(StandardCharsets.UTF_8));

        String missionName = sanitizeMissionName(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .getString("opentakserver_mission_name", ""));
        handler.postDelayed(() ->
                sendData((Config.CMD_COT_MISSION_PREFIX + missionName + "\n").getBytes(StandardCharsets.UTF_8)),
                100);
        handler.postDelayed(() ->
                sendData((Config.CMD_GET_SEC_STATE + "\n").getBytes(StandardCharsets.UTF_8)),
                200);
    }

    public void sendCriticalAlert() {
        sendData((Config.CMD_ALERT_SOS + "\n").getBytes());
    }

    public boolean sendPlaintextData(byte[] data) {
        return isProvisioningCommand(data) && sendData(data, true);
    }

    public boolean isReadyForTraffic() {
        return serialPort != null && serialPortOpen;
    }

    public boolean sendData(byte[] data) {
        return sendData(data, false);
    }

    public boolean sendData(byte[] data, boolean forcePlaintext) {
        byte[] dataToSend = data;
        byte[] dataWithNewline = null;
        boolean wipeSendBuffer = false;
        if (serialPort == null || !serialPortOpen) {
            Log.w(TAG, "Serial port not open, cannot send data.");
            updateStatus("Error: Serial port not open");
            if (auditLogger != null) {
                auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.WARNING,
                               "Serial", "Send failed - port not open", false);
            }
            return false;
        }

        // Input validation
        if (data == null || data.length == 0 || data.length > 512) {
            if (auditLogger != null) {
                auditLogger.log(AuditLogger.EventType.SECURITY_VIOLATION, AuditLogger.Severity.WARNING,
                               "Serial", "Invalid data length: " + (data != null ? data.length : 0), false);
            }
            return false;
        }

        try {
            if (forcePlaintext && !isProvisioningCommand(data)) {
                return false;
            }
            if (!forcePlaintext) {
                if (securityManager == null || !securityManager.isInitialized() || !securityManager.isEncryptionEnabled()) {
                    Log.e(TAG, "Security unavailable; refusing plaintext fallback");
                    return false;
                }
                dataToSend = encodeEncryptedPayloadBytes(data);
                wipeSendBuffer = dataToSend != null;
                if (dataToSend == null) {
                    Log.w(TAG, "Encryption failed, aborting send");
                    return false;
                }
            }

            dataWithNewline = new byte[dataToSend.length + 1];
            System.arraycopy(dataToSend, 0, dataWithNewline, 0, dataToSend.length);
            dataWithNewline[dataToSend.length] = '\n';

            synchronized (serialWriteLock) {
                serialPort.write(dataWithNewline, 500);
            }
            Log.i(TAG, "Data sent via serial, len=" + data.length);

            // Audit log data send
            if (auditLogger != null) {
                auditLogger.log(AuditLogger.EventType.DATA_SENT, AuditLogger.Severity.INFO,
                               "Serial", "Data sent, len: " + data.length, true);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error sending data via serial: " + e.getMessage(), e);
            updateStatus("Error sending data: " + e.getMessage());
            if (auditLogger != null) {
                auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                               "Serial", "Send error: " + e.getMessage(), false);
            }
            return false;
        } finally {
            if (dataWithNewline != null) {
                Arrays.fill(dataWithNewline, (byte) 0);
            }
            if (wipeSendBuffer) {
                Arrays.fill(dataToSend, (byte) 0);
            }
        }
    }

    // --- Setter/Getter Interface ---
    public class LocalBinder extends Binder {
        public SerialService getService() { return SerialService.this; }
    }
    public void setAkitaToolbar(AkitaToolbar toolbar) { this.akitaToolbar = toolbar; }
    public void setSerialStatusListener(SerialStatusListener listener) { this.serialStatusListener = listener; }
    public String getConnectionStatus() { return serialConnectionStatus; }
    public void setMapView(MapView view) { this.mapView = view; }

    private boolean consumeRuntimeStatus(String line) {
        if (line == null) {
            return false;
        }
        if (line.startsWith(Config.STATUS_TIME_SYNC_PREFIX)) {
            Log.i(TAG, "Firmware time sync status: " + line.substring(Config.STATUS_TIME_SYNC_PREFIX.length()));
            return true;
        }
        if (line.startsWith(Config.STATUS_COT_MISSION_PREFIX)) {
            Log.i(TAG, "Firmware CoT mission status: " + line.substring(Config.STATUS_COT_MISSION_PREFIX.length()));
            return true;
        }
        if (DeviceSecurityState.updateFromStatusLine(line)) {
            Log.i(TAG, "Firmware security state: " + DeviceSecurityState.getKeySummary()
                    + " • " + DeviceSecurityState.getHardwareSummary());
            return true;
        }
        return false;
    }

    private static String sanitizeMissionName(String missionName) {
        if (missionName == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder();
        for (int index = 0; index < missionName.length() && sanitized.length() < 64; index++) {
            char c = missionName.charAt(index);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ' ' || c == '.') {
                sanitized.append(c);
            }
        }
        return sanitized.toString().trim();
    }

    private byte[] encodeEncryptedPayloadBytes(byte[] plaintext) {
        return PayloadEnvelope.encode(securityManager, plaintext);
    }

    private static boolean isProvisioningCommand(byte[] data) {
        if (data == null) {
            return false;
        }
        String value = new String(data, StandardCharsets.UTF_8).trim();
        return value.startsWith(Config.CMD_PROVISION_STAGE_PREFIX)
                && value.length() > Config.CMD_PROVISION_STAGE_PREFIX.length();
    }

    private String decodePayload(String payload) {
        return PayloadEnvelope.decode(securityManager, payload);
    }

    private static final byte[] HEX_DIGITS = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
}
