// File: atak_plugin/src/services/BLEService.java
// Description: Handles BLE communication, retries, health checks, and ATAK marker updates.
package com.akitaengineering.meshtak.services;

import android.app.Service;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.atakmap.android.maps.MapView;
import com.atakmap.api.CotPoint;
import com.atakmap.api.Point2;
import com.atakmap.api.map.MapItem;
import com.atakmap.api.map.Marker;
import com.akitaengineering.meshtak.AkitaMissionControl;
import com.akitaengineering.meshtak.ui.AkitaProvisioningManager;
import com.akitaengineering.meshtak.ui.AkitaToolbar;
import com.akitaengineering.meshtak.ui.AkitaMissionMarkerRegistry;
import com.akitaengineering.meshtak.Config;
import com.akitaengineering.meshtak.AuditLogger;
import com.akitaengineering.meshtak.SecurityManager;

import java.util.UUID;
import java.lang.Math;
import java.nio.charset.StandardCharsets;

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
            if (bleConnectionStatus.equals("Connected")) {
                queryDeviceStatus(Config.CMD_GET_BATT);
            }
            handler.postDelayed(this, HEALTH_CHECK_INTERVAL);
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
        handler.removeCallbacks(healthCheckRunnable);
        handler.removeCallbacks(rescanRunnable);
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
        if (prefName != null && !prefName.trim().isEmpty()) {
            targetDeviceName = prefName.trim();
        }
    }

    private void initializeSecurity() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean encryptionEnabled = AkitaProvisioningManager.isEncryptionEnabled(preferences);
        String provisioningSecret = AkitaProvisioningManager.getActiveProvisioningSecret(this);

        securityManager.reset();
        if (!securityManager.initializeFromProvisioning(targetDeviceName, provisioningSecret)) {
            Log.e(TAG, "Failed to initialize security manager");
            auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                    "BLEService", "Security initialization failed", false);
            return;
        }

        securityManager.setEncryptionEnabled(encryptionEnabled);
        auditLogger.log(AuditLogger.EventType.CONFIGURATION_CHANGE, AuditLogger.Severity.INFO,
                "BLEService", encryptionEnabled ? "Security initialized" : "Security initialized with encryption disabled", true);
    }

    public void reloadSecurityConfiguration() {
        loadPreferences();
        initializeSecurity();
    }

    private boolean scanning;
    private void startScan() {
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
        if (scanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothLeScanner != null) {
            Log.d(TAG, "Stopping BLE scan.");
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }

        if (!bleConnectionStatus.equals("Connected") && !scanReschedulePending) {
            scanReschedulePending = true;
            handler.postDelayed(rescanRunnable, RE_SCAN_DELAY);
        }
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
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
            handler.postDelayed(BLEService.this::startScan, 5000);
        }
    };

    public boolean connect(final String address) {
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
                bleConnectionStatus = "Connected";
                if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Connected to " + gatt.getDevice().getAddress());
                
                if (auditLogger != null) {
                    auditLogger.log(AuditLogger.EventType.CONNECTION, AuditLogger.Severity.INFO,
                                   "BLE", "Connected to " + gatt.getDevice().getAddress(), true);
                }
                
                bluetoothGatt.discoverServices();
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
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onServicesDiscovered: Services discovered for " + gatt.getDevice().getAddress());
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic cotCharacteristic = service.getCharacteristic(COT_CHARACTERISTIC_UUID);
                    if (cotCharacteristic != null) {
                        boolean notificationsEnabled = gatt.setCharacteristicNotification(cotCharacteristic, true);
                        if (notificationsEnabled) {
                            Log.i(TAG, "Enabled notifications for CoT characteristic.");
                            BluetoothGattDescriptor cccd = cotCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                            if (cccd != null) {
                                cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(cccd);
                            } else {
                                Log.w(TAG, "CCCD descriptor not found for CoT characteristic");
                            }
                        } else {
                            // Notification enable failure handling
                        }
                    } 
                } 
            } else {
                // Service discovery failure handling
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

            String payload = new String(rawData, StandardCharsets.UTF_8).trim();
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

            if (AkitaMissionControl.getInstance(getApplicationContext()).consumeIncomingStatus(decodedPayload, AkitaMissionControl.ROUTE_BLE)) {
                return;
            }
            
            processCotData(decodedPayload);
        }

        @Override public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {}
    };

    public void disconnect() {
        if (bluetoothGatt == null) return;
        bluetoothGatt.disconnect();
    }

    public void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
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
                    "BLE");
        } catch (Exception e) {
            Log.e(TAG, "Error processing CoT data: " + e.getMessage(), e);
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
      return bleConnectionStatus.equals("Connected") && bluetoothGatt != null;
    }

    public boolean sendData(byte[] data) {
      return sendData(data, false);
    }

    public boolean sendData(byte[] data, boolean forcePlaintext) {
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
      
      writeCharacteristic.setValue(dataToSend);
      boolean success = bluetoothGatt.writeCharacteristic(writeCharacteristic);
      
      // Audit log data send
      if (auditLogger != null) {
          auditLogger.log(AuditLogger.EventType.DATA_SENT, AuditLogger.Severity.INFO,
                         "BLE", "Data sent, len: " + data.length, success);
      }
            return success;
    }
    
    // --- External Setters and Getters ---
    public void setAkitaToolbar(AkitaToolbar toolbar) { this.akitaToolbar = toolbar; }
    public void setBleStatusListener(BleStatusListener listener) { this.bleStatusListener = listener; }
    public String getConnectionStatus() { return bleConnectionStatus; }
    public String getConnectedDeviceAddress() { return bluetoothDeviceAddress; }
    public void setMapView(MapView view) { this.mapView = view; }
    public void setTargetDeviceName(String name) { this.targetDeviceName = name; }

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
