package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.preference.PreferenceManager;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.plugin.ui.PluginMapOverlay;

public class ConnectionStatusOverlay extends PluginMapOverlay {

    private String bleStatus = "BLE: Idle";
    private String serialStatus = "Serial: Idle";
    private long lastBleUpdate = 0;
    private long lastSerialUpdate = 0;
    private final Paint headerPaint;
    private final Paint textPaint;
    private final Paint mutedPaint;
    private final Paint backgroundPaint;
    private final float textSize = 14f;
    private final float headerSize = 16f;
    private final int textPadding;
    private final int cornerRadius;
    private final Context context;

    public ConnectionStatusOverlay(Context context, MapView mapView) {
        super(mapView);
        this.context = context;
        float density = context.getResources().getDisplayMetrics().density;
        textPadding = (int) (8 * density);
        cornerRadius = (int) (10 * density);

        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.WHITE);
        headerPaint.setTextSize(headerSize * density);
        headerPaint.setTypeface(Typeface.DEFAULT_BOLD);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(textSize * density);

        mutedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mutedPaint.setColor(Color.LTGRAY);
        mutedPaint.setTextSize(textSize * density);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.argb(180, 10, 10, 10));
    }

    public void setBleStatus(String status) {
        this.bleStatus = "BLE: " + status;
        lastBleUpdate = System.currentTimeMillis();
        if (getMapView() != null) {
            getMapView().invalidate();
        }
    }

    public void setSerialStatus(String status) {
        this.serialStatus = "Serial: " + status;
        lastSerialUpdate = System.currentTimeMillis();
        if (getMapView() != null) {
            getMapView().invalidate();
        }
    }

    @Override
    public void draw(Canvas canvas, MapView mapView) {
        String connectionMethod = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("connection_method", "ble");
        String deviceName = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("ble_device_name", "AkitaNode01");
        String baudRate = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("serial_baud_rate", "115200");

        String methodLine = connectionMethod.equalsIgnoreCase("ble")
                ? "Method: BLE"
                : "Method: Serial";
        String detailLine = connectionMethod.equalsIgnoreCase("ble")
                ? "Device: " + deviceName
                : "Baud: " + baudRate;

        String lastUpdateLine = "Last update: BLE " + formatAgeSeconds(lastBleUpdate)
                + " | Serial " + formatAgeSeconds(lastSerialUpdate);

        String[] lines = new String[] {
                "Akita MeshTAK",
                methodLine,
                detailLine,
                bleStatus,
                serialStatus,
                lastUpdateLine
        };

        float maxWidth = 0;
        for (int i = 0; i < lines.length; i++) {
            Paint p = (i == 0) ? headerPaint : (i == lines.length - 1 ? mutedPaint : textPaint);
            maxWidth = Math.max(maxWidth, p.measureText(lines[i]));
        }

        float lineHeight = textPaint.getTextSize() + (textPaint.getTextSize() * 0.35f);
        float headerHeight = headerPaint.getTextSize() + (headerPaint.getTextSize() * 0.35f);
        float totalHeight = headerHeight + (lineHeight * (lines.length - 1)) + (textPadding * 2);
        float totalWidth = maxWidth + (textPadding * 2);

        RectF background = new RectF(textPadding, textPadding, textPadding + totalWidth, textPadding + totalHeight);
        canvas.drawRoundRect(background, cornerRadius, cornerRadius, backgroundPaint);

        float x = textPadding * 2;
        float y = textPadding + headerHeight;
        canvas.drawText(lines[0], x, y, headerPaint);

        textPaint.setColor(Color.WHITE);

        for (int i = 1; i < lines.length; i++) {
            y += lineHeight;
            Paint p = (i == lines.length - 1) ? mutedPaint : textPaint;
            if (i == 3) {
                p = getStatusPaint(bleStatus);
            } else if (i == 4) {
                p = getStatusPaint(serialStatus);
            }
            canvas.drawText(lines[i], x, y, p);
        }
    }

    private Paint getStatusPaint(String status) {
        String lower = status.toLowerCase();
        if (lower.contains("connected")) {
            textPaint.setColor(Color.GREEN);
        } else if (lower.contains("disconnected") || lower.contains("error") || lower.contains("failed")) {
            textPaint.setColor(Color.RED);
        } else if (lower.contains("connecting") || lower.contains("scanning")) {
            textPaint.setColor(Color.YELLOW);
        } else {
            textPaint.setColor(Color.WHITE);
        }
        return textPaint;
    }

    private String formatAgeSeconds(long timestamp) {
        if (timestamp <= 0) return "never";
        long seconds = (System.currentTimeMillis() - timestamp) / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        return hours + "h";
    }
}
