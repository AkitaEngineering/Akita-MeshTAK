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
import com.atakmap.api.map.Marker;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.hoho.android.usbserial.driver.UsbSerialProber;

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

    private static final int HELTEC_VENDOR_ID = 1027;
    private static final int HELTEC_PRODUCT_ID = 24577;

    private static final String ACTION_USB_PERMISSION = "com.akitaengineering.meshtak.USB_PERMISSION";
    private static final long RECONNECT_DELAY = 5000;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private int reconnectAttemptCount = 0;
    private static final long OPEN_SERIAL_TIMEOUT = 10000;

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
                UsbDevice detachedDevice = (UsbDevice) intent.getParcelableExtra(UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
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

    private final Handler handler = new Handler();

    public void setAkitaToolbar(AkitaToolbar toolbar) {
        this.akitaToolbar = toolbar;
        if (toolbar != null) {
            toolbar.setSerialStatus(serialConnectionStatus);
        }
    }

    public void setSerialStatusListener(SerialStatusListener listener) {
        this.serialStatusListener = listener;
        if (listener != null) {
            listener.onSerialStatusChanged(serialConnectionStatus);
        }
    }

    public String getConnectionStatus() {
      return serialConnectionStatus;
    }

    public void setMapView(MapView view) {
        this.mapView = view;
    }

    public class LocalBinder extends Binder {
        public SerialService getService() {
            return SerialService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Serial Service created.");
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);
        loadPreferences();
        findAndOpenHeltecSerialPortWithRetry();
    }

    private void loadPreferences() {
        baudRate = Integer.parseInt(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .getString("serial_baud_rate", "115200"));
        Log.i(TAG, "Using baud rate: " + baudRate);
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
        if (availableDrivers.isEmpty()) {
            Log.w(TAG, "No serial drivers found.");
            updateStatus("Error: No drivers found");
            return;
        }

        for (UsbSerialDriver driver : availableDrivers) {
            UsbDevice device = driver.getDevice();
            Log.i(TAG, "Found USB Device - VID: " + device.getVendorId() + ", PID: " + device.getProductId() + ", Name: " + device.getDeviceName());
            if (device.getVendorId() == HELTEC_VENDOR_ID && device.getProductId() == HELTEC_PRODUCT_ID) {
                List<UsbSerialPort> ports = driver.getPorts();
                if (!ports.isEmpty()) {
                    serialPort = ports.get(0);
                    Log.i(TAG, "Found Heltec V3 serial port: " + serialPort);
                    UsbDeviceConnection connection = usbManager.openDevice(device);
                    if (connection == null) {
                        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                        usbManager.requestPermission(device, permissionIntent);
                        return;
                    }
                    openSerialPort(connection);
                    return;
                } else {
                    Log.w(TAG, "No serial ports found for Heltec V3 device.");
                    updateStatus("Error: No ports for Heltec");
                }
            }
        }
        Log.w(TAG, "Heltec V3 serial port not found.");
        updateStatus("Disconnected");
        handler.postDelayed(SerialService.this::findAndOpenHeltecSerialPortWithRetry, RECONNECT_DELAY);
    }

    private void openSerialPortWithTimeout() {
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
            reconnectAttemptCount = 0;
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

    @Override
    public void onDestroy() {
        Log.d(TAG, "Serial Service destroyed.");
        unregisterReceiver(usbReceiver);
        stopIoManager();
        closeSerialPort();
        executorService.shutdown();
        super.onDestroy();
    }

    @Override
    public void onNewData(byte[] data) {
        String received = new String(data);
        Log.i(TAG, "Received from serial: " + received.trim());
        processCotData(received.trim());
    }

    @Override
    public void onRunError(Exception e) {
        Log.e(TAG, "Serial I/O error: " + e.getMessage(), e);
        stopIoManager();
        closeSerialPort();
        updateStatus("Error: I/O - " + e.getMessage());
        handler.postDelayed(SerialService.this::findAndOpenHeltecSerialPortWithRetry, RECONNECT_DELAY);
    }

    private void processCotData(String cotData) {
       if (mapView == null) {
            Log.w(TAG, "MapView not set, cannot process CoT from serial.");
            return;
        }
        try {
            CotPoint cotPoint = CotPoint.fromXml(cotData);
            if (cotPoint != null) {
                Point2 geoPoint = new Point2(cotPoint.getLongitude(), cotPoint.getLatitude());
                String uid = cotPoint.getUid();
                String callsign = cotPoint.getDetail().get("contact").get("callsign");
                if (uid == null || callsign == null) return;

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
                Log.w(TAG, "Failed to parse CoT from serial: " + cotData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing CoT from serial: " + e.getMessage(), e);
        }
    }

    public void sendData(byte[] data) {
         if (serialPort != null && ioManager != null) {
            try {
                serialPort.write(data, 100);
                Log.i(TAG, "Sent data via serial: " + new String(data));
            } catch (IOException e) {
                Log.e(TAG, "Error sending data via serial: " + e.getMessage(), e);
                updateStatus("Error sending data: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Serial port not open, cannot send data.");
            updateStatus("Error: Serial port not open");
        }
    }

    public interface SerialStatusListener {
        void onSerialStatusChanged(String status);
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
}
