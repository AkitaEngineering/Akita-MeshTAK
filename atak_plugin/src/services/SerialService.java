// File: atak_plugin/src/services/SerialService.java
// Description: Handles USB Serial communication, device discovery, health checks, and ATAK marker updates.
package com.akitaengineering.meshtak.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.akitaengineering.meshtak.ui.AkitaToolbar;
import com.akitaengineering.meshtak.Config;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                    handler.postDelayed(SerialService.this::findAndOpenHeltecSerialPortWithRetry, RECONNECT_DELAY);
                }
            }
        }
    };
    
    // --- Service Lifecycle and Setup ---
    @Override
    public void onCreate() {
        super.onCreate();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);
        loadPreferences();
        findAndOpenHeltecSerialPortWithRetry();
        handler.post(healthCheckRunnable); 
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
        baudRate = Integer.parseInt(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .getString("serial_baud_rate", "115200"));
        Log.i(TAG, "Using baud rate: " + baudRate);
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
        String received = new String(data);
        Log.i(TAG, "Received from serial: " + received.trim());
        processCotData(received.trim());
    }

    // --- Data Processing (Full Logic) ---

    private void processCotData(String data) {
        if (mapView == null) return;
        
        // 1. Check for specific status prefixes first
        if (data.startsWith(Config.STATUS_BATT_PREFIX)) {
            String status = data.substring(Config.STATUS_BATT_PREFIX.length()).trim();
            if (akitaToolbar != null) akitaToolbar.setBatteryStatus(status);
            Log.d(TAG, "Battery status updated (Serial): " + status);
            return;
        }

        // 2. If not a status, assume it's CoT (Full ATAK Marker Logic)
        try {
            CotPoint cotPoint = CotPoint.fromXml(data);
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
                Log.d(TAG, "ATAK: Created new marker: " + marker.getTitle());
            } else if (mapItem instanceof Marker) {
                Marker marker = (Marker) mapItem;
                marker.setGeoPoint(geoPoint);
                Log.d(TAG, "ATAK: Updated existing marker: " + marker.getTitle());
            }
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
        Log.i(TAG, "Critical SOS Alert Sent via Serial.");
    }
    
    public void sendData(byte[] data) {
        if (serialPort != null && serialPort.isOpen()) {
            try {
                byte[] dataWithNewline = new byte[data.length + 1];
                System.arraycopy(data, 0, dataWithNewline, 0, data.length);
                dataWithNewline[data.length] = '\n'; 

                serialPort.write(dataWithNewline, 500); 
                Log.i(TAG, "Data sent via serial: " + new String(data));
            } catch (IOException e) {
                Log.e(TAG, "Error sending data via serial: " + e.getMessage(), e);
                updateStatus("Error sending data: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Serial port not open, cannot send data.");
            updateStatus("Error: Serial port not open");
        }
    }
    
    // --- Setter/Getter Interface ---
    public class AppBinder extends Binder {
        public SerialService getService() { return SerialService.this; }
    }
    public void setAkitaToolbar(AkitaToolbar toolbar) { this.akitaToolbar = toolbar; }
    public void setSerialStatusListener(SerialStatusListener listener) { this.serialStatusListener = listener; }
    public String getConnectionStatus() { return serialConnectionStatus; }
    public void setMapView(MapView view) { this.mapView = view; }
}
