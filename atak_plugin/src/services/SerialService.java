// File: atak_plugin/src/services/SerialService.java
// Description: Handles USB Serial communication, device discovery, health checks, and ATAK marker updates.
package com.akitaengineering.meshtak.services;

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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.atakmap.android.maps.MapView;
import com.atakmap.api.CotPoint;
import com.atakmap.api.Point2;
import com.atakmap.api.map.MapItem;
import com.atakmap.api.map.Marker;
import com.akitaengineering.meshtak.AkitaMissionControl;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.akitaengineering.meshtak.ui.AkitaMissionMarkerRegistry;
import com.akitaengineering.meshtak.ui.AkitaProvisioningManager;
import com.akitaengineering.meshtak.ui.AkitaToolbar;
import com.akitaengineering.meshtak.Config;
import com.akitaengineering.meshtak.AuditLogger;
import com.akitaengineering.meshtak.SecurityManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.charset.StandardCharsets;

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
    private String serialConnectionStatus = "Idle";
    private SecurityManager securityManager;
    private AuditLogger auditLogger;

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
                            openSerialPortWithTimeout();
                        }
                    } else {
                        Log.e(TAG, "USB permission denied for device: " + device);
                        updateStatus("Error: USB permission denied");
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice detachedDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (detachedDevice != null && serialPort != null && detachedDevice.getDeviceId() == serialPort.getDriver().getDevice().getDeviceId()) {
                    Log.i(TAG, "USB device detached: " + detachedDevice.getDeviceName());
                    stopIoManager();
                    closeSerialPort();
                    updateStatus("Disconnected");
                    reconnectAttemptCount = 0;
                    
                    // Audit log disconnection
                    if (auditLogger != null) {
                        auditLogger.log(AuditLogger.EventType.DISCONNECTION, AuditLogger.Severity.INFO,
                                       "Serial", "USB device detached", true);
                    } 
                    handler.postDelayed(SerialService.this::findAndOpenHeltecSerialPortWithRetry, RECONNECT_DELAY);
                }
            }
        }
    };
    
    // --- Service Lifecycle and Setup ---
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
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);
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
        handler.removeCallbacks(healthCheckRunnable);
        unregisterReceiver(usbReceiver);
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
        String provisioningSecret = AkitaProvisioningManager.getActiveProvisioningSecret(preferences);
        boolean encryptionEnabled = AkitaProvisioningManager.isEncryptionEnabled(preferences);

        securityManager.reset();
        if (!securityManager.initializeFromProvisioning(deviceId, provisioningSecret)) {
            Log.e(TAG, "Failed to initialize security manager");
            auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                    "SerialService", "Security initialization failed", false);
            return;
        }

        securityManager.setEncryptionEnabled(encryptionEnabled);
        auditLogger.log(AuditLogger.EventType.CONFIGURATION_CHANGE, AuditLogger.Severity.INFO,
                "SerialService", encryptionEnabled ? "Security initialized" : "Security initialized with encryption disabled", true);
    }

    public void reloadSecurityConfiguration() {
        loadPreferences();
        initializeSecurity();
    }
    
    private void updateStatus(final String status) {
        serialConnectionStatus = status;
        if (serialStatusListener != null) {
            serialStatusListener.onSerialStatusChanged(status);
        }
        if (akitaToolbar != null) {
            akitaToolbar.setDetailedSerialStatus(status);
        }
    }

    private void findAndOpenHeltecSerialPortWithRetry() {
        if (reconnectAttemptCount >= MAX_RECONNECT_ATTEMPTS && MAX_RECONNECT_ATTEMPTS > 0) {
            Log.w(TAG, "Max serial reconnect attempts reached.");
            updateStatus("Error: Max reconnect attempts");
            return;
        }
        updateStatus("Connecting (Attempt " + (reconnectAttemptCount + 1) + ")");
        handler.postDelayed(this::findAndOpenHeltecSerialPortInternal, RECONNECT_DELAY);
        reconnectAttemptCount++;
    }

    private void findAndOpenHeltecSerialPortInternal() {
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
                        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
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
        handler.postDelayed(SerialService.this::findAndOpenHeltecSerialPortWithRetry, RECONNECT_DELAY);
    }

    private void openSerialPortWithTimeout() {
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
        handler.postDelayed(SerialService.this::findAndOpenHeltecSerialPortWithRetry, RECONNECT_DELAY);
    }

    private void openSerialPort(UsbDeviceConnection connection) {
        // Setup timeout to prevent hanging if open fails silently
        handler.postDelayed(() -> {
            if (ioManager == null && serialPort != null && serialPort.isOpen()) {
                Log.w(TAG, "Serial port open timed out.");
                closeSerialPort();
                updateStatus("Error: Open timed out");
                handler.postDelayed(SerialService.this::findAndOpenHeltecSerialPortWithRetry, RECONNECT_DELAY);
            }
        }, OPEN_SERIAL_TIMEOUT);

        try {
            serialPort.open(connection);
            serialPort.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            startIoManager();
                    Log.i(TAG, "Serial port opened for Heltec V3");
            updateStatus("Connected");
            reconnectAttemptCount = 0; // Success! Reset count
            
            // Audit log connection
            if (auditLogger != null) {
                auditLogger.log(AuditLogger.EventType.CONNECTION, AuditLogger.Severity.INFO,
                               "Serial", "Serial port connected", true);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error opening serial port: " + e.getMessage(), e);
            closeSerialPort();
            updateStatus("Error: " + e.getMessage());
             handler.postDelayed(SerialService.this::findAndOpenHeltecSerialPortWithRetry, RECONNECT_DELAY);
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
    }
    
    // --- Interface Implementations ---
    @Override
    public void onRunError(Exception e) {
        Log.e(TAG, "Serial I/O error: " + e.getMessage(), e);
        stopIoManager();
        closeSerialPort();
        updateStatus("Error: I/O - " + e.getMessage());
        handler.postDelayed(SerialService.this::findAndOpenHeltecSerialPortWithRetry, RECONNECT_DELAY);
    }

    @Override
    public void onNewData(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        
        String received = new String(data, StandardCharsets.UTF_8);
        String decodedPayload = decodePayload(received.trim());
        if (decodedPayload == null) {
            if (auditLogger != null) {
                auditLogger.log(AuditLogger.EventType.AUTHENTICATION_FAILURE, AuditLogger.Severity.WARNING,
                               "Serial", "Failed to decode encrypted payload", false);
            }
            return;
        }

        Log.i(TAG, "Received from serial: " + received.trim());
        
        // Audit log data reception
        if (auditLogger != null) {
            auditLogger.log(AuditLogger.EventType.DATA_RECEIVED, AuditLogger.Severity.INFO,
                           "Serial", "Data received, len: " + data.length, true);
        }

        if (AkitaMissionControl.getInstance(getApplicationContext()).consumeIncomingStatus(decodedPayload, AkitaMissionControl.ROUTE_SERIAL)) {
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
            Log.w(TAG, "Received fragmented or non-CoT data (ignoring): " + cleanData);
            return; 
        }

        // 3. Process CoT (ATAK Marker Logic)
        try {
            CotPoint cotPoint = CotPoint.fromXml(cleanData);
            if (cotPoint == null) return;

            final String uid = cotPoint.getUid();
            String callsign = null;
            if (cotPoint.getDetail() != null && cotPoint.getDetail().get("contact") != null) {
                callsign = cotPoint.getDetail().get("contact").get("callsign");
            }
            final Point2 geoPoint = new Point2(cotPoint.getLongitude(), cotPoint.getLatitude());

            if (uid == null) return;

            MapItem mapItem = mapView.getMapItem(uid);

            if (mapItem == null) {
                final Marker marker = new Marker(geoPoint);
                marker.setUid(uid);
                marker.setTitle(callsign != null ? callsign : uid);
                marker.setType(cotPoint.getType() != null ? cotPoint.getType() : Config.DEFAULT_COT_TYPE);
                
                mapView.getRootGroup().addItem(marker);
            } else if (mapItem instanceof Marker) {
                Marker marker = (Marker) mapItem;
                marker.setGeoPoint(geoPoint);
            }

            AkitaMissionMarkerRegistry.getInstance().recordMarker(
                    uid,
                    callsign != null ? callsign : uid,
                    cotPoint.getLatitude(),
                    cotPoint.getLongitude(),
                    "Serial");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing CoT data from serial: " + e.getMessage(), e);
        }
    }
    
    // --- Public Service Interface Methods ---
    
    public void queryDeviceStatus(String command) {
        sendData((command + "\n").getBytes());
    }

    public void sendCriticalAlert() {
        sendData((Config.CMD_ALERT_SOS + "\n").getBytes());
    }
    
    public boolean sendPlaintextData(byte[] data) {
        return sendData(data, true);
    }

    public boolean isReadyForTraffic() {
        return serialPort != null && serialPort.isOpen();
    }

    public boolean sendData(byte[] data) {
        return sendData(data, false);
    }

    public boolean sendData(byte[] data, boolean forcePlaintext) {
        if (serialPort == null || !serialPort.isOpen()) {
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
        
        // Optional: Encrypt data if security is enabled; default is plaintext for compatibility
        byte[] dataToSend = data;
        if (!forcePlaintext && securityManager != null && securityManager.isInitialized() && securityManager.isEncryptionEnabled()) {
            String encryptedEnvelope = encodeEncryptedPayload(data);
            if (encryptedEnvelope != null) {
                dataToSend = encryptedEnvelope.getBytes(StandardCharsets.UTF_8);
            } else {
                Log.w(TAG, "Encryption failed, aborting send");
                return false;
            }
        }
        
        try {
            byte[] dataWithNewline = new byte[dataToSend.length + 1];
            System.arraycopy(dataToSend, 0, dataWithNewline, 0, dataToSend.length);
            dataWithNewline[dataToSend.length] = '\n'; 

            serialPort.write(dataWithNewline, 500); 
            Log.i(TAG, "Data sent via serial: " + new String(data));
            
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

    private String encodeEncryptedPayload(byte[] plaintext) {
        if (securityManager == null || !securityManager.isInitialized()) {
            return null;
        }
        byte[] encrypted = securityManager.encrypt(plaintext);
        if (encrypted == null || encrypted.length == 0) {
            return null;
        }

        StringBuilder hex = new StringBuilder(encrypted.length * 2);
        for (byte b : encrypted) {
            hex.append(String.format("%02x", b & 0xFF));
        }
        return Config.ENCRYPTED_PAYLOAD_PREFIX + Config.ENCRYPTED_PAYLOAD_VERSION + ":" +
            Config.ENCRYPTED_KEY_ID + ":" + hex;
    }

    private String decodePayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        if (!payload.startsWith(Config.ENCRYPTED_PAYLOAD_PREFIX)) {
            return payload;
        }

        if (securityManager == null || !securityManager.isInitialized() || !securityManager.isEncryptionEnabled()) {
            return null;
        }

        String headerAndHex = payload.substring(Config.ENCRYPTED_PAYLOAD_PREFIX.length());
        int firstSep = headerAndHex.indexOf(':');
        int secondSep = headerAndHex.indexOf(':', firstSep + 1);
        if (firstSep <= 0 || secondSep <= firstSep + 1) {
            return null;
        }

        String version = headerAndHex.substring(0, firstSep);
        String keyId = headerAndHex.substring(firstSep + 1, secondSep);
        if (!Config.ENCRYPTED_PAYLOAD_VERSION.equals(version) || !Config.ENCRYPTED_KEY_ID.equals(keyId)) {
            return null;
        }

        String hex = headerAndHex.substring(secondSep + 1);
        if ((hex.length() % 2) != 0) {
            return null;
        }

        byte[] encrypted = new byte[hex.length() / 2];
        for (int i = 0; i < encrypted.length; i++) {
            int idx = i * 2;
            try {
                encrypted[i] = (byte) Integer.parseInt(hex.substring(idx, idx + 2), 16);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        byte[] decrypted = securityManager.decrypt(encrypted);
        if (decrypted == null) {
            return null;
        }
        return new String(decrypted, StandardCharsets.UTF_8).trim();
    }
}
