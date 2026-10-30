package com.hana.aodclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.TypedValue;
import android.view.View;

/**
 * Simple forecast chart (v5): one bar per day for precipitation chance
 * (0-100%) with the daily high/low temperature lines drawn over them,
 * for exactly the forecast days the text list is showing.
 * Pure Canvas drawing, no libraries.
 */
public class WeatherChartView extends View {
    private String[] names = new String[0];
    private double[] max = new double[0];
    private double[] min = new double[0];
    private int[] pop = new int[0]; // percent, -1 = unavailable
    private int labelColor = Color.parseColor("#B3E5FC");

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WeatherChartView(Context ctx) {
        super(ctx);
        float density = ctx.getResources().getDisplayMetrics().density;
        barPaint.setColor(Color.parseColor("#3D7AB8"));
        barPaint.setStyle(Paint.Style.FILL);
        maxPaint.setColor(Color.parseColor("#FF9100"));
        maxPaint.setStyle(Paint.Style.STROKE);
        maxPaint.setStrokeWidth(2.2f * density);
        maxPaint.setStrokeJoin(Paint.Join.ROUND);
        maxPaint.setStrokeCap(Paint.Cap.ROUND);
        minPaint.setColor(Color.parseColor("#40C4FF"));
        minPaint.setStyle(Paint.Style.STROKE);
        minPaint.setStrokeWidth(2.2f * density);
        minPaint.setStrokeJoin(Paint.Join.ROUND);
        minPaint.setStrokeCap(Paint.Cap.ROUND);
        dotPaint.setStyle(Paint.Style.FILL);
        axisPaint.setColor(Color.argb(90, 255, 255, 255));
        axisPaint.setStrokeWidth(1f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    /** Supply the days to chart (already trimmed to the chosen forecast period). */
    public void setData(String[] names, double[] max, double[] min, int[] pop,
                        int days, int labelColor) {
        int n = Math.max(0, Math.min(days, names != null ? names.length : 0));
        this.names = new String[n];
        this.max = new double[n];
        this.min = new double[n];
        this.pop = new int[n];
        for (int i = 0; i < n; i++) {
            this.names[i] = names[i];
            this.max[i] = max[i];
            this.min[i] = min[i];
            this.pop[i] = (pop != null && i < pop.length) ? pop[i] : -1;
        }
        this.labelColor = labelColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = names.length;
        if (n == 0 || getWidth() == 0 || getHeight() == 0) return;

        float w = getWidth();
        float h = getHeight();
        float density = getResources().getDisplayMetrics().density;
        float legendH = 16 * density;
        float labelH = 15 * density;      // day names under the plot
        float topPad = 14 * density;      // room for high-temp labels
        float plotTop = legendH + topPad;
        float plotBottom = h - labelH - 2 * density;
        float plotH = Math.max(10 * density, plotBottom - plotTop);
        float left = 6 * density;
        float right = w - 6 * density;
        float slot = (right - left) / n;

        // Legend: orange = high, blue = low, bar = precip %
        textPaint.setColor(labelColor);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
        textPaint.setTextAlign(Paint.Align.LEFT);
        String legend = "— High   — Low   ■ Precip %";
        canvas.drawText(legend, left, legendH - 4 * density, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Temperature scale
        double tMin = Double.MAX_VALUE, tMax = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            tMin = Math.min(tMin, min[i]);
            tMax = Math.max(tMax, max[i]);
        }
        if (tMax - tMin < 4) { tMax += 2; tMin -= 2; }
        double pad = (tMax - tMin) * 0.12;
        tMax += pad;
        tMin -= pad;

        // Precipitation bars (behind the temp lines)
        float barW = Math.min(slot * 0.52f, 30 * density);
        for (int i = 0; i < n; i++) {
            if (pop[i] < 0) continue;
            float cx = left + slot * (i + 0.5f);
            float barH = plotH * Math.max(0, Math.min(100, pop[i])) / 100f;
            if (barH > 0.5f) {
                canvas.drawRect(cx - barW / 2f, plotBottom - barH,
                        cx + barW / 2f, plotBottom, barPaint);
            }
            if (pop[i] > 0) {
                textPaint.setColor(Color.parseColor("#90CAF9"));
                textPaint.setTextSize(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP, 9,
                        getResources().getDisplayMetrics()));
                canvas.drawText(pop[i] + "%", cx,
                        plotBottom - barH - 2 * density, textPaint);
            }
        }

        // Baseline
        canvas.drawLine(left, plotBottom, right, plotBottom, axisPaint);

        // Temp lines + points + value labels
        drawTempLine(canvas, max, true, tMin, tMax, plotTop, plotH, left, slot, density);
        drawTempLine(canvas, min, false, tMin, tMax, plotTop, plotH, left, slot, density);

        // Day names
        textPaint.setColor(labelColor);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
        for (int i = 0; i < n; i++) {
            float cx = left + slot * (i + 0.5f);
            String name = (i == 0) ? "Today" : names[i];
            canvas.drawText(name, cx, h - 3 * density, textPaint);
        }
    }

    private void drawTempLine(Canvas canvas, double[] vals, boolean isMax,
                              double tMin, double tMax, float plotTop, float plotH,
                              float left, float slot, float density) {
        int n = vals.length;
        Paint line = isMax ? maxPaint : minPaint;
        Path path = new Path();
        for (int i = 0; i < n; i++) {
            float x = left + slot * (i + 0.5f);
            float y = plotTop + (float) ((tMax - vals[i]) / (tMax - tMin)) * plotH;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        canvas.drawPath(path, line);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
        for (int i = 0; i < n; i++) {
            float x = left + slot * (i + 0.5f);
            float y = plotTop + (float) ((tMax - vals[i]) / (tMax - tMin)) * plotH;
            dotPaint.setColor(isMax ? Color.parseColor("#FF9100")
                    : Color.parseColor("#40C4FF"));
            canvas.drawCircle(x, y, 2.6f * density, dotPaint);
            textPaint.setColor(isMax ? Color.parseColor("#FFB74D")
                    : Color.parseColor("#81D4FA"));
            float labelY = isMax ? y - 5 * density : y + 12 * density;
            canvas.drawText(Math.round(vals[i]) + "°", x, labelY, textPaint);
        }
    }
}
