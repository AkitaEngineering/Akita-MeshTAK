// File: atak_plugin/src/services/BLEService.java
// Description: Handles BLE communication, retries, health checks, and ATAK marker updates.
package com.akitaengineering.meshtak.services;

import android.app.Service;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.atakmap.android.maps.MapView;
import com.atakmap.api.CotPoint;
import com.atakmap.api.Point2;
import com.atakmap.api.map.MapItem;
import com.atakmap.api.map.Marker;
import com.akitaengineering.meshtak.ui.AkitaToolbar;
import com.akitaengineering.meshtak.Config;
import com.akitaengineering.meshtak.AuditLogger;
import com.akitaengineering.meshtak.SecurityManager;

import java.util.UUID;
import java.lang.Math;

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

    private static final long SCAN_PERIOD = 10000;
    private static final long CONNECT_RETRY_DELAY = 5000;
    private int connectionRetryCount = 0;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long RE_SCAN_DELAY = 30000;
    private static final long CONNECTION_TIMEOUT = 15000;
    private static final long HEALTH_CHECK_INTERVAL = 30000;

    private Runnable connectionTimeoutRunnable;

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
        
        // Initialize security and audit logging
        securityManager = SecurityManager.getInstance();
        auditLogger = AuditLogger.getInstance();
        auditLogger.initialize(getApplicationContext());
        
        // Generate keys if not already initialized (in production, use secure provisioning)
        if (!securityManager.isInitialized()) {
            if (!securityManager.generateKeys()) {
                Log.e(TAG, "Failed to initialize security manager");
                auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                               "BLEService", "Security initialization failed", false);
            } else {
                auditLogger.log(AuditLogger.EventType.CONFIGURATION_CHANGE, AuditLogger.Severity.INFO,
                               "BLEService", "Security initialized", true);
            }
        }
        
        initialize();
        loadPreferences();
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
        // NOTE: In a real app, preferences would be loaded from SharedPreferences
    }

    private boolean scanning;
    private void startScan() {
        if (!scanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        if (!scanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothLeScanner != null) {
            Log.d(TAG, "Starting BLE scan.");
            scanning = true;
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
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "onConnectionStateChange: Connected to GATT server for device " + gatt.getDevice().getAddress());
                connectionRetryCount = 0;
                bleConnectionStatus = "Connected";
                if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Connected to " + gatt.getDevice().getAddress());
                
                // Audit log connection
                if (auditLogger != null) {
                    auditLogger.log(AuditLogger.EventType.CONNECTION, AuditLogger.Severity.INFO,
                                   "BLE", "Connected to " + gatt.getDevice().getAddress(), true);
                }
                
                bluetoothGatt.discoverServices();
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange: Disconnected from GATT server for device " + gatt.getDevice().getAddress() + ", status: " + status);
                bleConnectionStatus = "Disconnected";
                if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Disconnected");
                
                // Audit log disconnection
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
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onConnectionStateChange: GATT connection error for device " + gatt.getDevice().getAddress() + ", status: " + status + ", newState: " + newState);
                bleConnectionStatus = "Error";
                if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Error: Connection failed with status " + status);
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
            
            // Optional: Decrypt if security is enabled
            byte[] dataToProcess = rawData;
            if (securityManager != null && securityManager.isInitialized()) {
                byte[] decrypted = securityManager.decrypt(rawData);
                if (decrypted != null) {
                    dataToProcess = decrypted;
                } else {
                    Log.w(TAG, "Decryption failed, processing as plaintext");
                    if (auditLogger != null) {
                        auditLogger.log(AuditLogger.EventType.AUTHENTICATION_FAILURE, AuditLogger.Severity.WARNING,
                                       "BLE", "Decryption failed", false);
                    }
                }
            }
            
            // Audit log data reception
            if (auditLogger != null) {
                auditLogger.log(AuditLogger.EventType.DATA_RECEIVED, AuditLogger.Severity.INFO,
                               "BLE", "Data received, len: " + rawData.length, true);
            }
            
            processCotData(new String(dataToProcess));
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
            final String callsign = cotPoint.getDetail().get("contact").get("callsign");
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
    
    public void sendData(byte[] data) {
      if (!bleConnectionStatus.equals("Connected") || bluetoothGatt == null) {
          if (auditLogger != null) {
              auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.WARNING,
                             "BLE", "Send failed - not connected", false);
          }
          return;
      }
      
      // Input validation
      if (data == null || data.length == 0 || data.length > 512) {
          if (auditLogger != null) {
              auditLogger.log(AuditLogger.EventType.SECURITY_VIOLATION, AuditLogger.Severity.WARNING,
                             "BLE", "Invalid data length: " + (data != null ? data.length : 0), false);
          }
          return;
      }
      
      // Optional: Encrypt data if security is enabled
      byte[] dataToSend = data;
      if (securityManager != null && securityManager.isInitialized()) {
          byte[] encrypted = securityManager.encrypt(data);
          if (encrypted != null) {
              dataToSend = encrypted;
          } else {
              Log.w(TAG, "Encryption failed, sending plaintext");
          }
      }
      
      BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
      if (service == null) {
          if (auditLogger != null) {
              auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                             "BLE", "Service not found", false);
          }
          return;
      }
      
      BluetoothGattCharacteristic writeCharacteristic = service.getCharacteristic(WRITE_CHARACTERISTIC_UUID);
      if (writeCharacteristic == null) {
          if (auditLogger != null) {
              auditLogger.log(AuditLogger.EventType.ERROR, AuditLogger.Severity.ERROR,
                             "BLE", "Characteristic not found", false);
          }
          return;
      }
      
      writeCharacteristic.setValue(dataToSend);
      boolean success = bluetoothGatt.writeCharacteristic(writeCharacteristic);
      
      // Audit log data send
      if (auditLogger != null) {
          auditLogger.log(AuditLogger.EventType.DATA_SENT, AuditLogger.Severity.INFO,
                         "BLE", "Data sent, len: " + data.length, success);
      }
    }
    
    // --- External Setters and Getters ---
    public void setAkitaToolbar(AkitaToolbar toolbar) { this.akitaToolbar = toolbar; }
    public void setBleStatusListener(BleStatusListener listener) { this.bleStatusListener = listener; }
    public String getConnectionStatus() { return bleConnectionStatus; }
    public String getConnectedDeviceAddress() { return bluetoothDeviceAddress; }
    public void setMapView(MapView view) { this.mapView = view; }
    public void setTargetDeviceName(String name) { this.targetDeviceName = name; }
}
