package com.akitaengineering.meshtak.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.atakmap.android.maps.MapView;
import com.atakmap.api.CotPoint;
import com.atakmap.api.Point2;
import com.atakmap.api.map.Marker;
import com.akitaengineering.meshtak.ui.AkitaToolbar;

import java.util.List;
import java.util.UUID;

public class BLEService extends Service {

    private static final String TAG = "BLEService";
    private final IBinder binder = new LocalBinder();
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private String bluetoothDeviceAddress;
    private String targetDeviceName = "AkitaNode01";
    private Handler handler = new Handler();
    private MapView mapView;
    private AkitaToolbar akitaToolbar;
    private BleStatusListener bleStatusListener;
    private String bleConnectionStatus = "Idle";

    private static final UUID SERVICE_UUID = UUID.fromString("YOUR_SERVICE_UUID"); // Replace
    private static final UUID COT_CHARACTERISTIC_UUID = UUID.fromString("YOUR_CHARACTERISTIC_UUID"); // Replace
    private static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("YOUR_WRITE_CHARACTERISTIC_UUID"); // Add this

    private static final long SCAN_PERIOD = 10000;
    private static final long CONNECT_RETRY_DELAY = 5000;
    private int connectionRetryCount = 0;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long RE_SCAN_DELAY = 30000;
    private static final long CONNECTION_TIMEOUT = 15000;

    private Runnable connectionTimeoutRunnable;

    public void setAkitaToolbar(AkitaToolbar toolbar) {
        this.akitaToolbar = toolbar;
    }

    public void setBleStatusListener(BleStatusListener listener) {
        this.bleStatusListener = listener;
        if (listener != null) {
            listener.onBleStatusChanged(bleConnectionStatus);
        }
    }

    public String getConnectionStatus() {
        return bleConnectionStatus;
    }

    public String getConnectedDeviceAddress() {
        return bluetoothDeviceAddress;
    }

    public void setMapView(MapView view) {
        this.mapView = view;
    }

    public void setTargetDeviceName(String name) {
        this.targetDeviceName = name;
    }

    public class LocalBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
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
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BLE Service created.");
        initialize();
        loadPreferences();
        startScan();
    }

    private void loadPreferences() {
        if (mapView != null && mapView.getContext() != null) {
            targetDeviceName = androidx.preference.PreferenceManager.getDefaultSharedPreferences(mapView.getContext())
                    .getString("ble_device_name", "AkitaNode01");
            Log.i(TAG, "Target BLE device name loaded from settings: " + targetDeviceName);
        } else {
            Log.w(TAG, "MapView context not available, using default BLE device name.");
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "BLE Service destroyed.");
        disconnect();
        close();
        stopScan();
        super.onDestroy();
    }

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
            //  Attempt reconnect or rescan based on retry count
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
                bluetoothGatt.discoverServices();
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange: Disconnected from GATT server for device " + gatt.getDevice().getAddress() + ", status: " + status);
                bleConnectionStatus = "Disconnected";
                if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                 if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Disconnected");
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
                            readCharacteristic(cotCharacteristic);
                        } else {
                            Log.w(TAG, "Failed to enable notifications.");
                            bleConnectionStatus = "Error";
                            if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                            if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Error: Failed to enable CoT notifications");
                            disconnect();
                            close();
                             if (connectionRetryCount <= MAX_RETRY_ATTEMPTS) {
                                 long delay = CONNECT_RETRY_DELAY * (long) Math.pow(2, connectionRetryCount - 1);
                                 handler.postDelayed(() -> connect(bluetoothDeviceAddress), delay);
                             } else {
                                  handler.postDelayed(BLEService.this::startScan, RE_SCAN_DELAY);
                             }
                        }
                    } else {
                        Log.w(TAG, "CoT Characteristic not found.");
                        bleConnectionStatus = "Error";
                        if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                         if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Error: CoT Characteristic not found");
                        disconnect();
                        close();
                         if (connectionRetryCount <= MAX_RETRY_ATTEMPTS) {
                             long delay = CONNECT_RETRY_DELAY * (long) Math.pow(2, connectionRetryCount - 1);
                            handler.postDelayed(() -> connect(bluetoothDeviceAddress), delay);
                         } else {
                              handler.postDelayed(BLEService.this::startScan, RE_SCAN_DELAY);
                         }
                    }
                } else {
                    Log.w(TAG, "Akita MeshTAK Service not found.");
                    bleConnectionStatus = "Error";
                    if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                     if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Error: Akita MeshTAK Service not found");
                    disconnect();
                    close();
                     if (connectionRetryCount <= MAX_RETRY_ATTEMPTS) {
                         long delay = CONNECT_RETRY_DELAY * (long) Math.pow(2, connectionRetryCount - 1);
                         handler.postDelayed(() -> connect(bluetoothDeviceAddress), delay);
                     } else {
                          handler.postDelayed(BLEService.this::startScan, RE_SCAN_DELAY);
                     }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status + " for " + gatt.getDevice().getAddress());
                bleConnectionStatus = "Error";
                if (bleStatusListener != null) bleStatusListener.onBleStatusChanged(bleConnectionStatus);
                 if (akitaToolbar != null) akitaToolbar.setDetailedBleStatus("Error: Service discovery failed with status " + status);
                disconnect();
                close();
                 if (connectionRetryCount <= MAX_RETRY_ATTEMPTS) {
                     long delay = CONNECT_RETRY_DELAY * (long) Math.pow(2, connectionRetryCount - 1);
                     handler.postDelayed(() -> connect(bluetoothDeviceAddress), delay);
                 } else {
                      handler.postDelayed(BLEService.this::startScan, RE_SCAN_DELAY);
                 }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
                String cotData = new String(value);
                Log.i(TAG, "Read CoT data: " + cotData);
                processCotData(cotData);
            } else {
                Log.w(TAG, "Failed to read characteristic: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            String cotData = new String(value);
            Log.i(TAG, "Received updated CoT data: " + cotData);
            processCotData(cotData);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, android.bluetooth.BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Descriptor write success.");
            } else {
                Log.w(TAG, "Descriptor write failed: " + status);
            }
        }
    };

    public void sendData(byte[] data) {
      if (bluetoothGatt == null) {
          Log.w(TAG, "BluetoothGatt not initialized.");
          return;
      }
      BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
      if (service == null) {
          Log.w(TAG, "Akita MeshTAK Service not found.");
          return;
      }
      BluetoothGattCharacteristic writeCharacteristic = service.getCharacteristic(WRITE_CHARACTERISTIC_UUID);
      if (writeCharacteristic == null) {
          Log.w(TAG, "Write Characteristic not found.");
          return;
      }
      writeCharacteristic.setValue(data);
      boolean success = bluetoothGatt.writeCharacteristic(writeCharacteristic);
      Log.i(TAG, "Sent data via BLE: " + new String(data) + ", success: " + success);
    }

    public void disconnect() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.disconnect();
    }

    public void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    private void processCotData(String cotData) {
        if (mapView == null) {
            Log.w(TAG, "MapView is not yet set. Cannot process CoT.");
            return;
        }
        try {
            CotPoint cotPoint = CotPoint.fromXml(cotData);
            if (cotPoint != null) {
                Point2 geoPoint = new Point2(cotPoint.getLongitude(), cotPoint.getLatitude());
                String uid = cotPoint.getUid();
                String callsign = cotPoint.getDetail().get("contact").get("callsign");
                if (uid == null || callsign == null) {
                    Log.w(TAG, "CoT missing UID or callsign.");
                    return;
                }

                Marker existingMarker = null;
                for (Marker marker : mapView.getMarkers()) {
                    if (uid.equals(marker.getUid())) {
                        existingMarker = marker;
                        break;
                    }
                }

                if (existingMarker != null) {
                    existingMarker.setGeoPoint(geoPoint);
                    mapView.refresh();
                } else {
                    Marker marker = new Marker(geoPoint);
                    marker.setUid(uid);
                    marker.setTitle(callsign);
                    mapView.addMarker(marker);
                }
            } else {
                Log.w(TAG, "Failed to parse CoT XML: " + cotData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing CoT data: " + e.getMessage(), e);
        }
    }

    public interface BleStatusListener {
        void onBleStatusChanged(String status);
    }
}
