package com.hana.aodclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;

public class AnalogClockView extends View {
    private int clockColor = Color.WHITE;
    private boolean showSeconds = false;

    private final Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AnalogClockView(Context context) {
        super(context);
        init();
    }

    public AnalogClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        facePaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);
        hourPaint.setStyle(Paint.Style.STROKE);
        hourPaint.setStrokeCap(Paint.Cap.ROUND);
        minPaint.setStyle(Paint.Style.STROKE);
        minPaint.setStrokeCap(Paint.Cap.ROUND);
        secPaint.setStyle(Paint.Style.STROKE);
        secPaint.setStrokeCap(Paint.Cap.ROUND);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        centerPaint.setStyle(Paint.Style.FILL);
    }

    public void setClockColor(int color) {
        clockColor = color;
        invalidate();
    }

    public void setShowSeconds(boolean show) {
        showSeconds = show;
        invalidate();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) / 2f - dp(6);

        // Face ring
        facePaint.setColor(withAlpha(clockColor, 110));
        facePaint.setStrokeWidth(dp(3));
        canvas.drawCircle(cx, cy, r, facePaint);

        // Ticks
        for (int i = 0; i < 60; i++) {
            boolean major = i % 5 == 0;
            double rad = Math.toRadians(i * 6);
            float inner = major ? r * 0.88f : r * 0.93f;
            float x1 = cx + (float) (inner * Math.sin(rad));
            float y1 = cy - (float) (inner * Math.cos(rad));
            float x2 = cx + (float) (r * 0.97f * Math.sin(rad));
            float y2 = cy - (float) (r * 0.97f * Math.cos(rad));
            tickPaint.setColor(withAlpha(clockColor, major ? 200 : 90));
            tickPaint.setStrokeWidth(dp(major ? 3 : 1.5f));
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        // Numbers 12, 3, 6, 9
        numberPaint.setColor(clockColor);
        numberPaint.setTextSize(r * 0.24f);
        Paint.FontMetrics fm = numberPaint.getFontMetrics();
        float textOffset = (fm.ascent + fm.descent) / 2f;
        String[] nums = {"12", "3", "6", "9"};
        for (int i = 0; i < 4; i++) {
            double rad = Math.toRadians(i * 90);
            float x = cx + (float) (r * 0.68f * Math.sin(rad));
            float y = cy - (float) (r * 0.68f * Math.cos(rad));
            canvas.drawText(nums[i], x, y - textOffset, numberPaint);
        }

        // Time
        Calendar now = Calendar.getInstance();
        float ms = now.get(Calendar.MILLISECOND) / 1000f;
        float sec = now.get(Calendar.SECOND) + ms;
        float min = now.get(Calendar.MINUTE) + sec / 60f;
        float hour = (now.get(Calendar.HOUR_OF_DAY) % 12) + min / 60f;

        // Hour hand
        drawHand(canvas, cx, cy, hour * 30, r * 0.48f, dp(7), clockColor, hourPaint);
        // Minute hand
        drawHand(canvas, cx, cy, min * 6, r * 0.72f, dp(5), clockColor, minPaint);
        // Second hand
        if (showSeconds) {
            double rad = Math.toRadians(sec * 6);
            float sx = cx + (float) (r * 0.84f * Math.sin(rad));
            float sy = cy - (float) (r * 0.84f * Math.cos(rad));
            float tx = cx - (float) (r * 0.18f * Math.sin(rad));
            float ty = cy + (float) (r * 0.18f * Math.cos(rad));
            secPaint.setColor(withAlpha(clockColor, 220));
            secPaint.setStrokeWidth(dp(2));
            canvas.drawLine(tx, ty, sx, sy, secPaint);
        }

        // Center dot
        centerPaint.setColor(clockColor);
        canvas.drawCircle(cx, cy, dp(5), centerPaint);
    }

    private void drawHand(Canvas canvas, float cx, float cy, float degrees,
                          float length, float width, int color, Paint paint) {
        double rad = Math.toRadians(degrees);
        float x = cx + (float) (length * Math.sin(rad));
        float y = cy - (float) (length * Math.cos(rad));
        paint.setColor(color);
        paint.setStrokeWidth(width);
        canvas.drawLine(cx, cy, x, y, paint);
    }
}
