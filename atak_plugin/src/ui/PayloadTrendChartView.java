package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PayloadTrendChartView extends View {

    private final List<Integer> values = new ArrayList<>();
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private AkitaTheme.Palette palette;
    private int maxValue = 512;

    public PayloadTrendChartView(Context context) {
        this(context, null);
    }

    public PayloadTrendChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PayloadTrendChartView(Context context, AttributeSet attrs, int defStyleAttr) {
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

    public void setValues(List<Integer> points) {
        values.clear();
        if (points != null) {
            values.addAll(points);
        }
        invalidate();
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = Math.max(1, maxValue);
        invalidate();
    }

    private void configurePaints() {
        gridPaint.setColor(AkitaTheme.withAlpha(palette.grid, 180));
        gridPaint.setStrokeWidth(dp(1));

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(AkitaTheme.withAlpha(palette.navy, 120));

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(palette.silver);
        linePaint.setStrokeWidth(dp(3));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setColor(palette.accentStrong);

        labelPaint.setColor(palette.textSecondary);
        labelPaint.setTextSize(dp(11));

        emptyPaint.setColor(palette.textMuted);
        emptyPaint.setTextSize(dp(13));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = dp(16);
        float top = dp(16);
        float right = getWidth() - dp(16);
        float bottom = getHeight() - dp(24);

        if (right <= left || bottom <= top) {
            return;
        }

        float chartHeight = bottom - top;
        float chartWidth = right - left;

        for (int i = 0; i < 4; i++) {
            float y = top + ((chartHeight / 3f) * i);
            canvas.drawLine(left, y, right, y, gridPaint);
        }

        canvas.drawText(maxValue + " B", right - labelPaint.measureText(maxValue + " B"), top - dp(4), labelPaint);
        canvas.drawText("0 B", right - labelPaint.measureText("0 B"), bottom + dp(14), labelPaint);

        if (values.isEmpty()) {
            canvas.drawText("Awaiting successful transmissions", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }

        Path linePath = new Path();
        Path fillPath = new Path();
        float stepX = values.size() == 1 ? 0 : chartWidth / (values.size() - 1f);

        for (int i = 0; i < values.size(); i++) {
            float x = left + (stepX * i);
            float ratio = Math.min(1f, Math.max(0f, values.get(i) / (float) maxValue));
            float y = bottom - (chartHeight * ratio);
            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, bottom);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        float finalX = left + (stepX * (values.size() - 1));
        fillPath.lineTo(finalX, bottom);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        int lastValue = values.get(values.size() - 1);
        float lastRatio = Math.min(1f, Math.max(0f, lastValue / (float) maxValue));
        float lastY = bottom - (chartHeight * lastRatio);
        canvas.drawCircle(finalX, lastY, dp(5), pointPaint);

        String lastLabel = lastValue + " B";
        float labelX = Math.min(right - labelPaint.measureText(lastLabel), finalX + dp(8));
        float labelY = Math.max(top + dp(12), lastY - dp(8));
        canvas.drawText(lastLabel, labelX, labelY, labelPaint);
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}