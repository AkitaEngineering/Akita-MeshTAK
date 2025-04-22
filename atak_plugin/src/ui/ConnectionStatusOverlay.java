package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.plugin.ui.PluginMapOverlay;

public class ConnectionStatusOverlay extends PluginMapOverlay {

    private String bleStatus = "BLE: Idle";
    private String serialStatus = "Serial: Idle";
    private final Paint textPaint;
    private final int textColor = Color.WHITE;
    private final float textSize = 16f;
    private final int textPadding = 10;

    public ConnectionStatusOverlay(Context context, MapView mapView) {
        super(mapView);
        textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize * context.getResources().getDisplayMetrics().density);
        textPaint.setAntiAlias(true);
    }

    public void setBleStatus(String status) {
        this.bleStatus = "BLE: " + status;
        if (getMapView() != null) {
            getMapView().invalidate();
        }
    }

    public void setSerialStatus(String status) {
        this.serialStatus = "Serial: " + status;
        if (getMapView() != null) {
            getMapView().invalidate();
        }
    }

    @Override
    public void draw(Canvas canvas, MapView mapView) {
        Point screenPoint = new Point(textPadding, textPadding + (int) textPaint.getTextSize());
        canvas.drawText(bleStatus, screenPoint.x, screenPoint.y, textPaint);

        Point serialScreenPoint = new Point(textPadding, screenPoint.y + (int) textPaint.getTextSize() + textPadding);
        canvas.drawText(serialStatus, serialScreenPoint.x, serialScreenPoint.y, textPaint);
    }
}
