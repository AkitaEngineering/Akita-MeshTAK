package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import androidx.preference.PreferenceManager;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.plugin.ui.PluginMapOverlay;
import com.atakmap.api.Point2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public class MissionMapOverlay extends PluginMapOverlay {

    private static final long STALE_THRESHOLD_MILLIS = 5L * 60L * 1000L;

    private final Context context;
    private final Paint framePaint;
    private final Paint fillPaint;
    private final Paint textPaint;
    private final Paint mutedTextPaint;
    private final Paint alertPaint;
    private final Paint sectorPaint;
    private final Paint geofencePaint;
    private final Paint routeHealthPaint;

    private String bleStatus = "Idle";
    private String serialStatus = "Idle";

    public MissionMapOverlay(Context context, MapView mapView) {
        super(mapView);
        this.context = context;

        float density = context.getResources().getDisplayMetrics().density;

        framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(2f * density);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(13f * density);
        textPaint.setFakeBoldText(true);

        mutedTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mutedTextPaint.setTextSize(12f * density);

        alertPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        alertPaint.setStyle(Paint.Style.STROKE);
        alertPaint.setStrokeWidth(2.5f * density);

        sectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectorPaint.setStyle(Paint.Style.STROKE);
        sectorPaint.setStrokeWidth(2f * density);

        geofencePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        geofencePaint.setStyle(Paint.Style.STROKE);
        geofencePaint.setStrokeWidth(2f * density);

        routeHealthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        routeHealthPaint.setStyle(Paint.Style.FILL);
    }

    public void setBleStatus(String status) {
        bleStatus = status;
        if (getMapView() != null) {
            getMapView().invalidate();
        }
    }

    public void setSerialStatus(String status) {
        serialStatus = status;
        if (getMapView() != null) {
            getMapView().invalidate();
        }
    }

    @Override
    public void draw(Canvas canvas, MapView mapView) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        AkitaTheme.Palette palette = AkitaTheme.resolvePalette(context);
        applyPalette(palette);

        AkitaIncidentBoard.IncidentState incidentState = AkitaIncidentBoard.getState(preferences);
        AkitaMissionMarkerRegistry registry = AkitaMissionMarkerRegistry.getInstance();
        List<AkitaMissionMarkerRegistry.TrackedMarker> staleMarkers = registry.getStaleMarkers(STALE_THRESHOLD_MILLIS);
        AkitaMissionMarkerRegistry.TrackedMarker anchorMarker = registry.getMostRecentMarker();

        PointF anchor = resolveAnchorPoint(mapView, anchorMarker, canvas.getWidth(), canvas.getHeight());
        float sectorRadius = Math.min(canvas.getWidth(), canvas.getHeight()) * 0.16f;
        float geofenceHalfSize = sectorRadius * 1.35f;

        drawRouteHealthBanner(canvas, palette, incidentState, registry.getTrackedMarkerCount(), staleMarkers.size());
        drawGeofence(canvas, anchor, geofenceHalfSize, incidentState.overlayLabel);
        drawSearchSectors(canvas, anchor, sectorRadius);
        drawAnchorMarker(canvas, anchor, anchorMarker, incidentState);
        drawStaleMarkerAlerts(canvas, mapView, staleMarkers, anchor);
    }

    private void drawRouteHealthBanner(Canvas canvas,
                                       AkitaTheme.Palette palette,
                                       AkitaIncidentBoard.IncidentState incidentState,
                                       int trackedCount,
                                       int staleCount) {
        float density = context.getResources().getDisplayMetrics().density;
        float left = canvas.getWidth() - (300f * density);
        float top = 18f * density;
        float right = canvas.getWidth() - (18f * density);
        float bottom = top + (88f * density);
        RectF card = new RectF(left, top, right, bottom);

        canvas.drawRoundRect(card, 18f * density, 18f * density, fillPaint);
        canvas.drawRoundRect(card, 18f * density, 18f * density, framePaint);

        String activeRoute = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("connection_method", "ble");
        String routeStatus = "ble".equalsIgnoreCase(activeRoute) ? bleStatus : serialStatus;
        String routeHealth = "Route: " + activeRoute.toUpperCase(Locale.US) + " • " + routeStatus;
        String staleSummary = staleCount <= 0 ? "No stale markers" : staleCount == 1 ? "1 stale marker" : staleCount + " stale markers";
        String nodeSummary = trackedCount <= 0 ? "Awaiting CoT tracks" : trackedCount + " tracked nodes";

        float textX = left + (14f * density);
        float textY = top + (24f * density);
        canvas.drawText(incidentState.title + " • " + incidentState.rolePack, textX, textY, textPaint);
        canvas.drawText(routeHealth, textX, textY + (20f * density), mutedTextPaint);
        canvas.drawText(nodeSummary + " • " + staleSummary, textX, textY + (38f * density), mutedTextPaint);
        canvas.drawText(incidentState.overlaySummary, textX, textY + (56f * density), mutedTextPaint);

        RectF bar = new RectF(textX, bottom - (16f * density), right - (14f * density), bottom - (8f * density));
        Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(AkitaTheme.statusColor(routeStatus, palette));
        canvas.drawRoundRect(bar, 999f, 999f, routeHealthPaint);
        float healthWidth = Math.max(0.12f, computeRouteHealth(routeStatus)) * bar.width();
        RectF fill = new RectF(bar.left, bar.top, bar.left + healthWidth, bar.bottom);
        canvas.drawRoundRect(fill, 999f, 999f, barPaint);
    }

    private void drawGeofence(Canvas canvas, PointF anchor, float halfSize, String overlayLabel) {
        RectF geofence = new RectF(anchor.x - halfSize, anchor.y - halfSize, anchor.x + halfSize, anchor.y + halfSize);
        canvas.drawRoundRect(geofence, halfSize * 0.08f, halfSize * 0.08f, geofencePaint);
        canvas.drawText(overlayLabel, geofence.left, geofence.top - dp(8), textPaint);
    }

    private void drawSearchSectors(Canvas canvas, PointF anchor, float radius) {
        RectF sectorBounds = new RectF(anchor.x - radius, anchor.y - radius, anchor.x + radius, anchor.y + radius);
        String[] labels = {"Alpha", "Bravo", "Charlie"};
        for (int index = 0; index < labels.length; index++) {
            float startAngle = -110f + (index * 70f);
            canvas.drawArc(sectorBounds, startAngle, 56f, false, sectorPaint);

            Path sectorLine = new Path();
            sectorLine.moveTo(anchor.x, anchor.y);
            double radians = Math.toRadians(startAngle + 28f);
            sectorLine.lineTo((float) (anchor.x + (Math.cos(radians) * radius)),
                    (float) (anchor.y + (Math.sin(radians) * radius)));
            canvas.drawPath(sectorLine, sectorPaint);

            float labelRadius = radius + dp(16);
            float labelX = (float) (anchor.x + (Math.cos(radians) * labelRadius));
            float labelY = (float) (anchor.y + (Math.sin(radians) * labelRadius));
            canvas.drawText(labels[index], labelX, labelY, mutedTextPaint);
        }
    }

    private void drawAnchorMarker(Canvas canvas,
                                  PointF anchor,
                                  AkitaMissionMarkerRegistry.TrackedMarker anchorMarker,
                                  AkitaIncidentBoard.IncidentState incidentState) {
        Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(textPaint.getColor());
        pointPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(anchor.x, anchor.y, dp(6), pointPaint);

        String anchorLabel = anchorMarker == null ? incidentState.nextAction : (anchorMarker.title == null || anchorMarker.title.isEmpty() ? anchorMarker.uid : anchorMarker.title);
        canvas.drawText(anchorLabel, anchor.x + dp(12), anchor.y - dp(8), textPaint);
    }

    private void drawStaleMarkerAlerts(Canvas canvas,
                                       MapView mapView,
                                       List<AkitaMissionMarkerRegistry.TrackedMarker> staleMarkers,
                                       PointF fallbackAnchor) {
        int alertCount = Math.min(3, staleMarkers.size());
        for (int index = 0; index < alertCount; index++) {
            AkitaMissionMarkerRegistry.TrackedMarker staleMarker = staleMarkers.get(index);
            PointF stalePoint = projectMarker(mapView, staleMarker);
            if (stalePoint == null) {
                stalePoint = new PointF(fallbackAnchor.x + dp(44 + (index * 30)), fallbackAnchor.y + dp(48 + (index * 22)));
            }

            canvas.drawCircle(stalePoint.x, stalePoint.y, dp(14), alertPaint);
            canvas.drawText("STALE", stalePoint.x - dp(16), stalePoint.y - dp(18), mutedTextPaint);
            String label = staleMarker.title == null || staleMarker.title.isEmpty() ? staleMarker.uid : staleMarker.title;
            canvas.drawText(label, stalePoint.x + dp(18), stalePoint.y + dp(4), textPaint);
        }
    }

    private PointF resolveAnchorPoint(MapView mapView,
                                      AkitaMissionMarkerRegistry.TrackedMarker anchorMarker,
                                      int canvasWidth,
                                      int canvasHeight) {
        PointF projected = projectMarker(mapView, anchorMarker);
        if (projected != null) {
            return projected;
        }
        return new PointF(canvasWidth * 0.45f, canvasHeight * 0.58f);
    }

    private PointF projectMarker(MapView mapView, AkitaMissionMarkerRegistry.TrackedMarker marker) {
        if (mapView == null || marker == null) {
            return null;
        }
        return projectPoint(mapView, marker.longitude, marker.latitude);
    }

    private PointF projectPoint(MapView mapView, double longitude, double latitude) {
        Object projected = invokeProjection(mapView, new Point2(longitude, latitude));
        if (projected == null) {
            projected = invokeProjection(mapView, longitude, latitude);
        }
        return pointFromObject(projected);
    }

    private Object invokeProjection(MapView mapView, Object... args) {
        String[] methodNames = {"forward", "toScreen", "toPixels", "projectToScreen"};
        for (String methodName : methodNames) {
            for (Method method : mapView.getClass().getMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterTypes().length != args.length) {
                    continue;
                }
                try {
                    return method.invoke(mapView, args);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private PointF pointFromObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof PointF) {
            return (PointF) value;
        }
        if (value instanceof Point2) {
            Point2 point2 = (Point2) value;
            return new PointF((float) point2.getX(), (float) point2.getY());
        }
        try {
            Method getX = value.getClass().getMethod("getX");
            Method getY = value.getClass().getMethod("getY");
            return new PointF(((Number) getX.invoke(value)).floatValue(), ((Number) getY.invoke(value)).floatValue());
        } catch (Exception ignored) {
        }
        try {
            Field xField = value.getClass().getField("x");
            Field yField = value.getClass().getField("y");
            return new PointF(((Number) xField.get(value)).floatValue(), ((Number) yField.get(value)).floatValue());
        } catch (Exception ignored) {
        }
        return null;
    }

    private void applyPalette(AkitaTheme.Palette palette) {
        framePaint.setColor(palette.monochrome ? palette.accent : AkitaTheme.withAlpha(palette.outline, 220));
        fillPaint.setColor(palette.monochrome ? AkitaTheme.withAlpha(palette.background, 235) : AkitaTheme.withAlpha(palette.surfaceElevated, 230));
        textPaint.setColor(palette.textPrimary);
        mutedTextPaint.setColor(palette.textSecondary);
        alertPaint.setColor(palette.monochrome ? palette.accent : palette.danger);
        sectorPaint.setColor(palette.monochrome ? palette.accent : palette.accentStrong);
        geofencePaint.setColor(palette.monochrome ? palette.accent : palette.warning);
        routeHealthPaint.setColor(palette.monochrome ? palette.background : AkitaTheme.withAlpha(palette.grid, 210));
    }

    private float computeRouteHealth(String status) {
        String normalized = status == null ? "" : status.toLowerCase(Locale.US);
        if (normalized.contains("connected") || normalized.contains("ready")) {
            return 0.92f;
        }
        if (normalized.contains("connecting") || normalized.contains("scanning") || normalized.contains("searching")) {
            return 0.58f;
        }
        if (normalized.contains("error") || normalized.contains("failed") || normalized.contains("disconnected")) {
            return 0.18f;
        }
        return 0.35f;
    }

    private float dp(int value) {
        return value * context.getResources().getDisplayMetrics().density;
    }
}