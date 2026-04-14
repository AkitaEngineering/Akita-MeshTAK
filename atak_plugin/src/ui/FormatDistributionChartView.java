package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class FormatDistributionChartView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint primaryTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondaryTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ringBounds = new RectF();
    private AkitaTheme.Palette palette;
    private int plainTextCount;
    private int jsonCount;
    private int customCount;

    public FormatDistributionChartView(Context context) {
        this(context, null);
    }

    public FormatDistributionChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FormatDistributionChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        palette = AkitaTheme.darkPalette();
        configurePaints();
    }

    public void setPalette(AkitaTheme.Palette palette) {
        if (palette == null) {
            return;
        }
        this.palette = palette;
        configurePaints();
        invalidate();
    }

    public void setData(int plainTextCount, int jsonCount, int customCount) {
        this.plainTextCount = Math.max(0, plainTextCount);
        this.jsonCount = Math.max(0, jsonCount);
        this.customCount = Math.max(0, customCount);
        invalidate();
    }

    private void configurePaints() {
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.BUTT);
        trackPaint.setColor(palette.monochrome ? palette.grid : AkitaTheme.withAlpha(palette.grid, 210));

        segmentPaint.setStyle(Paint.Style.STROKE);
        segmentPaint.setStrokeCap(Paint.Cap.BUTT);

        primaryTextPaint.setColor(palette.textPrimary);
        primaryTextPaint.setTextAlign(Paint.Align.CENTER);
        primaryTextPaint.setTextSize(dp(24));
        primaryTextPaint.setFakeBoldText(true);

        secondaryTextPaint.setColor(palette.monochrome ? palette.accent : palette.textSecondary);
        secondaryTextPaint.setTextAlign(Paint.Align.CENTER);
        secondaryTextPaint.setTextSize(dp(12));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = Math.min(width, height) * 0.28f;
        float strokeWidth = Math.max(dp(16), radius * 0.3f);
        float inset = strokeWidth / 2f + dp(8);

        ringBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        ringBounds.inset(inset / 2f, inset / 2f);

        int total = plainTextCount + jsonCount + customCount;
        if (palette.monochrome) {
            drawMonochromeRings(canvas, centerX, centerY, radius, total);
            return;
        }

        trackPaint.setStrokeWidth(strokeWidth);
        segmentPaint.setStrokeWidth(strokeWidth);

        canvas.drawArc(ringBounds, -90f, 360f, false, trackPaint);

        if (total > 0) {
            float startAngle = -90f;
            startAngle = drawSegment(canvas, startAngle, plainTextCount, total, palette.silver);
            startAngle = drawSegment(canvas, startAngle, jsonCount, total, palette.navy);
            drawSegment(canvas, startAngle, customCount, total, palette.accent);

            canvas.drawText(String.valueOf(total), centerX, centerY + dp(4), primaryTextPaint);
            canvas.drawText("frames tracked", centerX, centerY + dp(24), secondaryTextPaint);
        } else {
            segmentPaint.setColor(AkitaTheme.withAlpha(palette.outline, 140));
            canvas.drawArc(ringBounds, -90f, 360f, false, segmentPaint);
            canvas.drawText("0", centerX, centerY + dp(4), primaryTextPaint);
            canvas.drawText("Awaiting traffic", centerX, centerY + dp(24), secondaryTextPaint);
        }
    }

    private void drawMonochromeRings(Canvas canvas, float centerX, float centerY, float radius, int total) {
        float baseStroke = Math.max(dp(10), radius * 0.16f);
        float gap = dp(8);

        if (total > 0) {
            drawConcentricArc(canvas, centerX, centerY, radius, baseStroke, plainTextCount, total);
            drawConcentricArc(canvas, centerX, centerY, radius - (baseStroke + gap), baseStroke, jsonCount, total);
            drawConcentricArc(canvas, centerX, centerY, radius - ((baseStroke + gap) * 2f), baseStroke, customCount, total);

            canvas.drawText(String.valueOf(total), centerX, centerY + dp(4), primaryTextPaint);
            canvas.drawText("frames tracked", centerX, centerY + dp(24), secondaryTextPaint);
        } else {
            segmentPaint.setStyle(Paint.Style.STROKE);
            segmentPaint.setStrokeWidth(baseStroke);
            segmentPaint.setColor(palette.accent);
            RectF bounds = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            canvas.drawArc(bounds, -90f, 360f, false, segmentPaint);
            canvas.drawText("0", centerX, centerY + dp(4), primaryTextPaint);
            canvas.drawText("Awaiting traffic", centerX, centerY + dp(24), secondaryTextPaint);
        }
    }

    private void drawConcentricArc(Canvas canvas,
                                   float centerX,
                                   float centerY,
                                   float radius,
                                   float strokeWidth,
                                   int count,
                                   int total) {
        if (radius <= 0f || count <= 0 || total <= 0) {
            return;
        }
        RectF bounds = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        segmentPaint.setStyle(Paint.Style.STROKE);
        segmentPaint.setStrokeWidth(strokeWidth);
        segmentPaint.setColor(palette.accent);
        float sweepAngle = (360f * count) / total;
        canvas.drawArc(bounds, -90f, sweepAngle, false, segmentPaint);
    }

    private float drawSegment(Canvas canvas, float startAngle, int count, int total, int color) {
        if (count <= 0 || total <= 0) {
            return startAngle;
        }
        float sweepAngle = (360f * count) / total;
        segmentPaint.setColor(color);
        canvas.drawArc(ringBounds, startAngle, sweepAngle, false, segmentPaint);
        return startAngle + sweepAngle;
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}