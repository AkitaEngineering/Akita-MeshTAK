package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    private final Paint subtitlePaint;
    private final Paint textPaint;
    private final Paint mutedPaint;
    private final Paint backgroundPaint;
    private final Paint strokePaint;
    private final Paint accentPaint;
    private final Paint trackPaint;
    private final Paint statusPaint;
    private final float textSize = 14f;
    private final float headerSize = 16f;
    private final float subtitleSize = 12f;
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

        subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtitlePaint.setTextSize(subtitleSize * density);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(textSize * density);

        mutedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mutedPaint.setTextSize(textSize * density);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2f * density);

        accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        statusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        statusPaint.setTextSize(textSize * density);
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
        AkitaTheme.Palette palette = AkitaTheme.resolvePalette(context);
        applyPalette(palette);

        String connectionMethod = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("connection_method", "ble");
        String deviceName = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("ble_device_name", "AkitaNode01");
        String baudRate = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("serial_baud_rate", "115200");
        String portPath = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("serial_port_path", "/dev/ttyUSB0");

        String routeLine = connectionMethod.equalsIgnoreCase("ble")
                ? "Route: BLE • Target " + deviceName
                : "Route: Serial • " + portPath + " @ " + baudRate;

        String freshnessLine = "Freshness: BLE " + formatAgeSeconds(lastBleUpdate)
                + " • Serial " + formatAgeSeconds(lastSerialUpdate);

        float totalWidth = Math.max(320f * context.getResources().getDisplayMetrics().density, headerPaint.measureText(routeLine) + (textPadding * 2));
        float totalHeight = 220f * context.getResources().getDisplayMetrics().density;
        RectF background = new RectF(textPadding, textPadding, textPadding + totalWidth, textPadding + totalHeight);
        canvas.drawRoundRect(background, cornerRadius, cornerRadius, backgroundPaint);
        canvas.drawRoundRect(background, cornerRadius, cornerRadius, strokePaint);

        float accentHeight = 10f * context.getResources().getDisplayMetrics().density;
        RectF accentStrip = new RectF(background.left, background.top, background.right, background.top + accentHeight);
        canvas.drawRoundRect(accentStrip, cornerRadius, cornerRadius, accentPaint);

        float x = textPadding * 2;
        float y = background.top + accentHeight + (headerPaint.getTextSize() * 1.25f);
        canvas.drawText("Akita MeshTAK", x, y, headerPaint);

        float subtitleX = background.right - textPadding;
        subtitlePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(AkitaTheme.getThemeLabel(context), subtitleX, y, subtitlePaint);

        y += subtitlePaint.getTextSize() * 1.45f;
        subtitlePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Route picture", x, y, subtitlePaint);

        y += textPaint.getTextSize() * 1.45f;
        canvas.drawText(routeLine, x, y, textPaint);

        y += textPaint.getTextSize() * 1.75f;
        drawStatusRow(canvas, background, y, "BLE", stripPrefix(bleStatus, "BLE: "), lastBleUpdate, palette);

        y += 48f * context.getResources().getDisplayMetrics().density;
        drawStatusRow(canvas, background, y, "SER", stripPrefix(serialStatus, "Serial: "), lastSerialUpdate, palette);

        y += 54f * context.getResources().getDisplayMetrics().density;
        canvas.drawText(freshnessLine, x, y, mutedPaint);
    }

    private void drawStatusRow(Canvas canvas,
                               RectF background,
                               float baselineY,
                               String label,
                               String status,
                               long timestamp,
                               AkitaTheme.Palette palette) {
        float left = textPadding * 2f;
        float right = background.right - (textPadding * 2f);
        float barTop = baselineY + (6f * context.getResources().getDisplayMetrics().density);
        float barBottom = barTop + (10f * context.getResources().getDisplayMetrics().density);
        float barLeft = left + (58f * context.getResources().getDisplayMetrics().density);

        canvas.drawText(label, left, baselineY, mutedPaint);
        statusPaint.setColor(AkitaTheme.statusColor(status, palette));
        canvas.drawText(status, barLeft, baselineY, statusPaint);

        RectF track = new RectF(barLeft, barTop, right, barBottom);
        canvas.drawRoundRect(track, barBottom - barTop, barBottom - barTop, trackPaint);

        float freshness = computeFreshness(timestamp);
        RectF fill = new RectF(barLeft, barTop, barLeft + ((right - barLeft) * freshness), barBottom);
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(AkitaTheme.statusColor(status, palette));
        canvas.drawRoundRect(fill, barBottom - barTop, barBottom - barTop, fillPaint);
    }

    private void applyPalette(AkitaTheme.Palette palette) {
        headerPaint.setColor(palette.textPrimary);
        subtitlePaint.setColor(palette.textSecondary);
        textPaint.setColor(palette.textPrimary);
        mutedPaint.setColor(palette.textMuted);
        backgroundPaint.setColor(AkitaTheme.withAlpha(palette.surface, 230));
        strokePaint.setColor(AkitaTheme.withAlpha(palette.outline, 185));
        accentPaint.setColor(AkitaTheme.withAlpha(palette.accentStrong, 225));
        trackPaint.setColor(AkitaTheme.withAlpha(palette.grid, 210));
        trackPaint.setStyle(Paint.Style.FILL);
        statusPaint.setTypeface(Typeface.DEFAULT_BOLD);
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

    private float computeFreshness(long timestamp) {
        if (timestamp <= 0) {
            return 0.08f;
        }
        long elapsedMillis = Math.max(0L, System.currentTimeMillis() - timestamp);
        float freshness = 1f - (elapsedMillis / 180000f);
        return Math.max(0.08f, Math.min(1f, freshness));
    }

    private String stripPrefix(String value, String prefix) {
        if (value == null) {
            return "Idle";
        }
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }
        return value;
    }
}
