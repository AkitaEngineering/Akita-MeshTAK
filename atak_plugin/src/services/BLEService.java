// File: atak_plugin/src/services/BLEService.java
// Description: Handles BLE communication, retries, health checks, and ATAK marker updates.
package com.akitaengineering.meshtak.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.akitaengineering.meshtak.AkitaMissionControl;
import com.akitaengineering.meshtak.ui.AkitaProvisioningManager;
import com.akitaengineering.meshtak.ui.AkitaToolbar;
import com.akitaengineering.meshtak.ui.AkitaMissionMarkerRegistry;
import com.akitaengineering.meshtak.Config;
import com.akitaengineering.meshtak.AuditLogger;
import com.akitaengineering.meshtak.PayloadEnvelope;
import com.akitaengineering.meshtak.SecurityManager;
import com.akitaengineering.meshtak.TransportFrameCodec;

import java.util.UUID;
import java.lang.Math;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@SuppressLint("MissingPermission") // Every BLE entry point is guarded by hasRequiredBlePermissions().
public class BLEService extends Service {

    private static final String TAG = "BLEService";
    private final IBinder binder = new LocalBinder();
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private String bluetoothDeviceAddress;
    private String targetDeviceName = "AkitaNode01";
    private final Handler handler = new Handler();
    private MapView mapView;
    private AkitaToolbar akitaToolbar;
    private BleStatusListener bleStatusListener;
    private String bleConnectionStatus = "Idle";
    private SecurityManager securityManager;
    private AuditLogger auditLogger;
    private final Deque<byte[]> pendingWrites = new ArrayDeque<>();
    private final TransportFrameCodec.Reassembler inboundFrames = new TransportFrameCodec.Reassembler();
    private boolean writeInProgress;
    private boolean notificationsConfigured;
    private boolean mtuNegotiationComplete;
    private boolean destroyed;
    private int negotiatedMtu = 23;

    // Constants read from Config.java
    private static final UUID SERVICE_UUID = Config.BLE_SERVICE_UUID;
    private static final UUID COT_CHARACTERISTIC_UUID = Config.COT_CHARACTERISTIC_UUID;
    private static final UUID WRITE_CHARACTERISTIC_UUID = Config.WRITE_CHARACTERISTIC_UUID;
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final long SCAN_PERIOD = 10000;
    private static final long CONNECT_RETRY_DELAY = 5000;
    private int connectionRetryCount = 0;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long RE_SCAN_DELAY = 30000;
    private static final long CONNECTION_TIMEOUT = 15000;
    private static final long HEALTH_CHECK_INTERVAL = 30000;
    private static final int REQUESTED_MTU = 247;
    private static final int MIN_PROTOCOL_MTU = 64;

    private Runnable connectionTimeoutRunnable;
    private boolean scanReschedulePending = false;
    private final Runnable rescanRunnable = () -> {
        scanReschedulePending = false;
        startScan();
    };

    public interface BleStatusListener {
        void onBleStatusChanged(String status);
    }

    private final Runnable healthCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (destroyed) return;
            if (bleConnectionStatus.equals("Connected")) {
                queryDeviceStatus(Config.CMD_GET_BATT);
            }
            if (!destroyed) handler.postDelayed(this, HEALTH_CHECK_INTERVAL);
        }
    };

    public class LocalBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
    }

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

        initialize();
        startScan();
        handler.post(healthCheckRunnable);

        auditLogger.log(AuditLogger.EventType.CONNECTION, AuditLogger.Severity.INFO,
                       "BLEService", "Service created", true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        close();
        stopScan();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        disconnect();
        close();
        stopScan();
        super.onDestroy();
    }

    // --- Core GATT Logic and Handlers ---

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain BluetoothAdapter.");
            return false;
        }
        return true;
    }

    private void loadPreferences() {
        String prefName = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("ble_device_name", targetDeviceName);
        if (isValidTargetDeviceName(prefName)) {
            targetDeviceName = prefName.trim();
        }
    }

    private void initializeSecurity() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String provisioningSecret = AkitaProvisioningManager.getActiveProvisioningSecret(this);

        securityManager.reset();
        if (!securityManager.initializeFromProvisioning(targetDeviceName, provisioningSecret)) {
            Log.e(TAG, "Failed to initialize security manager");
            auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                    "BLEService", "Security initialization failed", false);
            return;
        }

        securityManager.setEncryptionEnabled(true);
        auditLogger.log(AuditLogger.EventType.CONFIGURATION_CHANGE, AuditLogger.Severity.INFO,
                "BLEService", "Security initialized", true);
    }

    public void reloadSecurityConfiguration() {
        loadPreferences();
        initializeSecurity();
    }

    private boolean scanning;
    private void startScan() {
        if (destroyed) return;
        if (!hasRequiredBlePermissions(true)) {
            reportMissingBlePermission();
            if (!destroyed) scheduleRescan();
            return;
        }
        if (!scanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        if (!scanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothLeScanner != null) {
            Log.d(TAG, "Starting BLE scan.");
            scanning = true;
            scanReschedulePending = false;
            bluetoothLeScanner.startScan(leScanCallback);
            handler.postDelayed(this::stopScan, SCAN_PERIOD);
        }
    }

    private void stopScan() {
        if (!hasRequiredBlePermissions(true)) {
            scanning = false;
            reportMissingBlePermission();
            scheduleRescan();
            return;
        }
        if (scanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothLeScanner != null) {
            Log.d(TAG, "Stopping BLE scan.");
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }

        if (!destroyed) scheduleRescan();
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (!hasRequiredBlePermissions(true)) {
                reportMissingBlePermission();
                return;
            }
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && device.getName().equals(targetDeviceName)) {
                Log.i(TAG, "Found target BLE device: " + device.getAddress());
                bluetoothDeviceAddress = device.getAddress();
                stopScan();
                connect(bluetoothDeviceAddress);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with error: " + errorCode);
            scanning = false;
            if (!destroyed) handler.postDelayed(BLEService.this::startScan, 5000);
        }
    };

    public boolean connect(final String address) {
        if (!hasRequiredBlePermissions(false)) {
            reportMissingBlePermission();
            return false;
        }
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        if (bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress) && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                connectionRetryCount = 0;
                bleConnectionStatus = "Connecting";
                if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Connecting to " + address + "...");
                startConnectionTimeout();
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new connection to " + address + " (attempt " + (connectionRetryCount + 1) + "/" + MAX_RETRY_ATTEMPTS + ")");
        bluetoothDeviceAddress = address;
        connectionRetryCount++;
        bleConnectionStatus = "Connecting";
        if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
        if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Connecting to " + address + "...");
        startConnectionTimeout();
        return true;
    }

    private void startConnectionTimeout() {
        stopConnectionTimeout();
        connectionTimeoutRunnable = () -> {
            if (destroyed) return;
            Log.w(TAG, "Connection timeout reached.");
            disconnect();
            close();
            bleConnectionStatus = "Error";
            if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
            if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Error: Connection timed out.");
            if (connectionRetryCount <= MAX_RETRY_ATTEMPTS) {
                long delay = CONNECT_RETRY_DELAY * (long) Math.pow(2, connectionRetryCount - 1);
                Log.i(TAG, "Attempting to reconnect in " + delay + " ms (attempt " + connectionRetryCount + "/" + MAX_RETRY_ATTEMPTS + ")");
                handler.postDelayed(() -> connect(bluetoothDeviceAddress), delay);
            } else {
                Log.w(TAG, "Max reconnection attempts reached. Will rescan periodically.");
                handler.postDelayed(BLEService.this::startScan, RE_SCAN_DELAY);
            }
        };
        handler.postDelayed(connectionTimeoutRunnable, CONNECTION_TIMEOUT);
    }

    private void stopConnectionTimeout() {
        if (connectionTimeoutRunnable != null) {
            handler.removeCallbacks(connectionTimeoutRunnable);
            connectionTimeoutRunnable = null;
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            stopConnectionTimeout();
            if (destroyed) {
                gatt.close();
                return;
            }
            if (!hasRequiredBlePermissions(false)) {
                reportMissingBlePermission();
                return;
            }
            // Check GATT status first to properly distinguish errors from normal disconnections
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onConnectionStateChange: GATT error for device " + gatt.getDevice().getAddress() + ", status: " + status + ", newState: " + newState);
                bleConnectionStatus = "Error";
                if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Error: Connection failed with status " + status);

                if (auditLogger != null) {
                    auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                                   "BLE", "GATT error status " + status + " for " + gatt.getDevice().getAddress(), false);
                }

                disconnect();
                close();
                if (connectionRetryCount <= MAX_RETRY_ATTEMPTS) {
                    long delay = CONNECT_RETRY_DELAY * (long) Math.pow(2, connectionRetryCount - 1);
                    Log.i(TAG, "Attempting to reconnect after error in " + delay + " ms (attempt " + connectionRetryCount + "/" + MAX_RETRY_ATTEMPTS + ")");
                    handler.postDelayed(() -> connect(bluetoothDeviceAddress), delay);
                } else {
                    Log.w(TAG, "Max reconnection attempts reached after error. Will rescan periodically.");
                    handler.postDelayed(BLEService.this::startScan, RE_SCAN_DELAY);
                }
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "onConnectionStateChange: Connected to GATT server for device " + gatt.getDevice().getAddress());
                connectionRetryCount = 0;
                bleConnectionStatus = "Configuring";
                if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Connected to " + gatt.getDevice().getAddress());

                if (auditLogger != null) {
                    auditLogger.log(AuditLogger.EventType.CONNECTION, AuditLogger.Severity.INFO,
                                   "BLE", "Connected to " + gatt.getDevice().getAddress(), true);
                }

                negotiatedMtu = 23;
                mtuNegotiationComplete = false;
                notificationsConfigured = false;
                inboundFrames.reset();
                synchronized (pendingWrites) {
                    clearPendingWritesLocked();
                }
                if (!gatt.requestMtu(REQUESTED_MTU)) {
                    failTransportConfiguration("Unable to start BLE MTU negotiation");
                } else {
                    startConnectionTimeout();
                }
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange: Disconnected from GATT server for device " + gatt.getDevice().getAddress());
                bleConnectionStatus = "Disconnected";
                if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Disconnected");

                if (auditLogger != null) {
                    auditLogger.log(AuditLogger.EventType.DISCONNECTION, AuditLogger.Severity.INFO,
                                   "BLE", "Disconnected from " + gatt.getDevice().getAddress(), true);
                }

                close();
                if (connectionRetryCount <= MAX_RETRY_ATTEMPTS) {
                    long delay = CONNECT_RETRY_DELAY * (long) Math.pow(2, connectionRetryCount - 1);
                    Log.i(TAG, "Attempting to reconnect in " + delay + " ms (attempt " + connectionRetryCount + "/" + MAX_RETRY_ATTEMPTS + ")");
                    handler.postDelayed(() -> connect(bluetoothDeviceAddress), delay);
                } else {
                    Log.w(TAG, "Max reconnection attempts reached. Will rescan periodically.");
                    handler.postDelayed(BLEService.this::startScan, RE_SCAN_DELAY);
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            mtuNegotiationComplete = true;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                negotiatedMtu = mtu;
            }
            if (status != BluetoothGatt.GATT_SUCCESS || negotiatedMtu < MIN_PROTOCOL_MTU) {
                failTransportConfiguration("BLE MTU negotiation did not meet the protocol minimum");
            } else if (!gatt.discoverServices()) {
                failTransportConfiguration("Unable to start BLE service discovery");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (!hasRequiredBlePermissions(false)) {
                reportMissingBlePermission();
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onServicesDiscovered: Services discovered for " + gatt.getDevice().getAddress());
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic cotCharacteristic = service.getCharacteristic(COT_CHARACTERISTIC_UUID);
                    BluetoothGattCharacteristic writeCharacteristic = service.getCharacteristic(WRITE_CHARACTERISTIC_UUID);
                    if (cotCharacteristic != null && writeCharacteristic != null) {
                        boolean notificationsEnabled = gatt.setCharacteristicNotification(cotCharacteristic, true);
                        if (notificationsEnabled) {
                            Log.i(TAG, "Enabled notifications for CoT characteristic.");
                            BluetoothGattDescriptor cccd = cotCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                            if (cccd != null) {
                                cccd.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                                if (!gatt.writeDescriptor(cccd)) {
                                    failTransportConfiguration("Unable to configure BLE indications");
                                }
                            } else {
                                failTransportConfiguration("BLE indication descriptor is missing");
                            }
                        } else {
                            failTransportConfiguration("Unable to enable BLE indications");
                        }
                    } else {
                        failTransportConfiguration("Required BLE characteristics are missing");
                    }
                } else {
                    failTransportConfiguration("Required BLE service is missing");
                }
            } else {
                failTransportConfiguration("BLE service discovery failed");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                processCotData(new String(characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] rawData = characteristic.getValue();
            if (rawData == null || rawData.length == 0) {
                return;
            }

            String payload = inboundFrames.accept(new String(rawData, StandardCharsets.UTF_8));
            if (payload == null) {
                return;
            }
            String decodedPayload = decodePayload(payload);
            if (decodedPayload == null) {
                if (auditLogger != null) {
                    auditLogger.log(AuditLogger.EventType.AUTHENTICATION_FAILURE, AuditLogger.Severity.WARNING,
                                   "BLE", "Failed to decode encrypted payload", false);
                }
                return;
            }

            // Audit log data reception
            if (auditLogger != null) {
                auditLogger.log(AuditLogger.EventType.DATA_RECEIVED, AuditLogger.Severity.INFO,
                               "BLE", "Data received, len: " + rawData.length, true);
            }

            handler.post(() -> processDecodedBlePayload(decodedPayload));
        }

        private void processDecodedBlePayload(String decodedPayload) {
            if (AkitaMissionControl.getInstance(getApplicationContext()).consumeIncomingStatus(decodedPayload, AkitaMissionControl.ROUTE_BLE)) {
                return;
            }

            if (consumeRuntimeStatus(decodedPayload)) {
                return;
            }

            processCotData(decodedPayload);
        }

        @Override public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                notificationsConfigured = true;
                updateTransportReadiness();
            } else {
                failTransportConfiguration("BLE indication configuration failed");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            synchronized (pendingWrites) {
                byte[] completed = pendingWrites.pollFirst();
                if (completed != null) Arrays.fill(completed, (byte) 0);
                writeInProgress = false;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    writeNextFrameLocked();
                } else {
                    clearPendingWritesLocked();
                    Log.e(TAG, "BLE frame write failed with status " + status);
                }
            }
        }
    };

    private void updateTransportReadiness() {
        if (notificationsConfigured && mtuNegotiationComplete
                && negotiatedMtu >= MIN_PROTOCOL_MTU && bluetoothGatt != null) {
            boolean becameReady = !"Connected".equals(bleConnectionStatus);
            bleConnectionStatus = "Connected";
            stopConnectionTimeout();
            if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
            if (becameReady) syncRuntimeState();
        }
    }

    private void failTransportConfiguration(String reason) {
        Log.e(TAG, reason);
        bleConnectionStatus = "Error";
        if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
        if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Error: " + reason);
        if (auditLogger != null) {
            auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                    "BLE", reason, false);
        }
        disconnect();
        close();
    }

    public void disconnect() {
        if (bluetoothGatt == null) return;
        if (!hasRequiredBlePermissions(false)) {
            reportMissingBlePermission();
            return;
        }
        bluetoothGatt.disconnect();
    }

    public void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        notificationsConfigured = false;
        mtuNegotiationComplete = false;
        synchronized (pendingWrites) {
            clearPendingWritesLocked();
        }
    }

    // --- Data Processing (Robustness Fix) ---
    private void processCotData(String data) {
        if (mapView == null) {
            Log.w(TAG, "MapView is not yet set. Cannot process CoT.");
            return;
        }

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
                    "BLE");
        } catch (Exception e) {
            Log.e(TAG, "Error processing CoT data: " + e.getMessage(), e);
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

        String missionName = sanitizeMissionName(PreferenceManager.getDefaultSharedPreferences(this)
                .getString("opentakserver_mission_name", ""));
        handler.postDelayed(() ->
                sendData((Config.CMD_COT_MISSION_PREFIX + missionName + "\n").getBytes(StandardCharsets.UTF_8)),
                100);
    }

    public void sendCriticalAlert() {
        sendData((Config.CMD_ALERT_SOS + "\n").getBytes());
    }

    public boolean sendPlaintextData(byte[] data) {
      return isProvisioningCommand(data) && sendData(data, true);
    }

    public boolean isReadyForTraffic() {
      return bleConnectionStatus.equals("Connected") && bluetoothGatt != null
              && notificationsConfigured && negotiatedMtu >= MIN_PROTOCOL_MTU;
    }

    public boolean sendData(byte[] data) {
      return sendData(data, false);
    }

    public boolean sendData(byte[] data, boolean forcePlaintext) {
            byte[] dataToSend = data;
            boolean wipeSendBuffer = false;
      if (!hasRequiredBlePermissions(false)) {
          reportMissingBlePermission();
          return false;
      }
      if (!bleConnectionStatus.equals("Connected") || bluetoothGatt == null) {
          if (auditLogger != null) {
              auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.WARNING,
                             "BLE", "Send failed - not connected", false);
          }
          return false;
      }

      // Input validation
      if (data == null || data.length == 0 || data.length > 512) {
          if (auditLogger != null) {
              auditLogger.log(AuditLogger.EventType.SECURITY_VIOLATION, AuditLogger.Severity.WARNING,
                             "BLE", "Invalid data length: " + (data != null ? data.length : 0), false);
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

          BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
          if (service == null) {
              if (auditLogger != null) {
                  auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                                 "BLE", "Service not found", false);
              }
              return false;
          }

          BluetoothGattCharacteristic writeCharacteristic = service.getCharacteristic(WRITE_CHARACTERISTIC_UUID);
          if (writeCharacteristic == null) {
              if (auditLogger != null) {
                  auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                                 "BLE", "Characteristic not found", false);
              }
              return false;
          }

          List<byte[]> frames;
          try {
              frames = TransportFrameCodec.frame(dataToSend, negotiatedMtu - 3);
          } catch (IllegalArgumentException | IllegalStateException exception) {
              Log.e(TAG, "Unable to frame BLE payload", exception);
              return false;
          }
          synchronized (pendingWrites) {
              for (byte[] frame : frames) pendingWrites.addLast(frame);
              writeNextFrameLocked();
              boolean success = writeInProgress || !pendingWrites.isEmpty();
              if (auditLogger != null) {
                  auditLogger.log(AuditLogger.EventType.DATA_SENT, AuditLogger.Severity.INFO,
                          "BLE", "Data queued, len: " + data.length, success);
              }
              return success;
          }
      } finally {
          if (wipeSendBuffer) {
              Arrays.fill(dataToSend, (byte) 0);
          }
      }
    }

    private void writeNextFrameLocked() {
        if (writeInProgress || pendingWrites.isEmpty() || bluetoothGatt == null) return;
        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        BluetoothGattCharacteristic characteristic = service == null
                ? null : service.getCharacteristic(WRITE_CHARACTERISTIC_UUID);
        if (characteristic == null) {
            clearPendingWritesLocked();
            return;
        }
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        characteristic.setValue(pendingWrites.peekFirst());
        writeInProgress = bluetoothGatt.writeCharacteristic(characteristic);
        if (!writeInProgress) clearPendingWritesLocked();
    }

    private void clearPendingWritesLocked() {
        while (!pendingWrites.isEmpty()) {
            Arrays.fill(pendingWrites.removeFirst(), (byte) 0);
        }
        writeInProgress = false;
    }

    private static boolean isProvisioningCommand(byte[] data) {
        if (data == null) {
            return false;
        }
        String value = new String(data, StandardCharsets.UTF_8).trim();
        return value.startsWith(Config.CMD_PROVISION_STAGE_PREFIX)
                && value.length() > Config.CMD_PROVISION_STAGE_PREFIX.length();
    }

    // --- External Setters and Getters ---
    public void setAkitaToolbar(AkitaToolbar toolbar) { this.akitaToolbar = toolbar; }
    public void setBleStatusListener(BleStatusListener listener) { this.bleStatusListener = listener; }
    public String getConnectionStatus() { return bleConnectionStatus; }
    public String getConnectedDeviceAddress() { return bluetoothDeviceAddress; }
    public void setMapView(MapView view) { this.mapView = view; }
    public void setTargetDeviceName(String name) {
        if (isValidTargetDeviceName(name)) {
            String normalized = name.trim();
            boolean changed = !normalized.equals(this.targetDeviceName);
            this.targetDeviceName = normalized;
            if (changed && securityManager != null) {
                initializeSecurity();
                disconnect();
                close();
                startScan();
            }
        } else {
            Log.w(TAG, "Rejected invalid BLE target device name");
        }
    }

    private static boolean isValidTargetDeviceName(String name) {
        if (name == null) return false;
        String value = name.trim();
        if (value.isEmpty() || value.length() > 64) return false;
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_') return false;
        }
        return true;
    }

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

    private String decodePayload(String payload) {
        return PayloadEnvelope.decode(securityManager, payload);
    }

    private boolean hasRequiredBlePermissions(boolean scanRequired) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            return !scanRequired
                    || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        return !scanRequired
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void reportMissingBlePermission() {
        bleConnectionStatus = "Permission Required";
        if (bleStatusListener != null) {
            bleStatusListener.onBleStatusChanged(bleConnectionStatus);
        }
        if (akitaToolbar != null) {
            akitaToolbar.setDetailedBleStatus("Grant Bluetooth permissions in Android settings");
        }
        if (auditLogger != null) {
            auditLogger.log(AuditLogger.EventType.SECURITY_VIOLATION, AuditLogger.Severity.WARNING,
                    "BLE", "Required runtime Bluetooth permission is missing", false);
        }
    }

    private void scheduleRescan() {
        if (!destroyed && !bleConnectionStatus.equals("Connected") && !scanReschedulePending) {
            scanReschedulePending = true;
            handler.postDelayed(rescanRunnable, RE_SCAN_DELAY);
        }
    }

    private static final byte[] HEX_DIGITS = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
}
