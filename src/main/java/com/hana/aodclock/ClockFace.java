package com.hana.aodclock;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

/**
 * Shared clock-face renderer used by MainActivity, ClockDreamService
 * and (in scaled form) the settings preview. The root view must contain
 * the ids from activity_main.xml.
 */
public class ClockFace {
    private final Context ctx;
    private final View root;
    private final LinearLayout clockContainer;
    private final LinearLayout clockBlock;
    private final LinearLayout digitalRow;
    private final AnalogClockView analogView;
    private final TextView timeText;
    private final TextView amPmText;
    private final TextView dateText;
    private final TextView batteryText;
    private final LinearLayout weatherContainer;

    private Window window; // optional: host window for scheduled brightness
    private int lastAppliedBrightnessPct = -1;
    private boolean offNow = false;
    private int lastSchedMinute = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private long lastShiftMs = 0;
    private int lastSecond = -1;
    private long lastBatteryCheckMs = 0;
    private long lastWeatherRenderAt = -1;
    private boolean running = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
            handler.postDelayed(this, 200);
        }
    };

    public ClockFace(Context ctx, View root) {
        this.ctx = ctx;
        this.root = root;
        clockContainer = root.findViewById(R.id.clockContainer);
        clockBlock = root.findViewById(R.id.clockBlock);
        digitalRow = root.findViewById(R.id.digitalRow);
        analogView = root.findViewById(R.id.analogView);
        timeText = root.findViewById(R.id.timeText);
        amPmText = root.findViewById(R.id.amPmText);
        dateText = root.findViewById(R.id.dateText);
        batteryText = root.findViewById(R.id.batteryText);
        weatherContainer = root.findViewById(R.id.weatherContainer);
    }

    public static boolean isCharging(Context ctx) {
        Intent intent = ctx.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return false;
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    public void applyPrefs() {
        root.setBackgroundColor(Prefs.bgColor(ctx));

        int color = Prefs.color(ctx);
        boolean analog = Prefs.STYLE_ANALOG.equals(Prefs.style(ctx));

        digitalRow.setVisibility(analog ? View.GONE : View.VISIBLE);
        analogView.setVisibility(analog ? View.VISIBLE : View.GONE);
        analogView.setClockColor(color);
        analogView.setShowSeconds(Prefs.showSeconds(ctx));

        if (analog) {
            int size = (int) (Math.min(
                    ctx.getResources().getDisplayMetrics().widthPixels,
                    ctx.getResources().getDisplayMetrics().heightPixels) * 0.62f);
            analogView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        }

        Typeface tf = typeface();
        timeText.setTypeface(tf);
        timeText.setTextColor(color);
        timeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Prefs.fontSize(ctx));
        amPmText.setTypeface(tf);
        amPmText.setTextColor(color);
        dateText.setTypeface(tf);
        dateText.setTextColor(color);
        batteryText.setTypeface(tf);
        batteryText.setTextColor(color);

        dateText.setVisibility(Prefs.showDate(ctx) ? View.VISIBLE : View.GONE);
        batteryText.setVisibility(Prefs.showBattery(ctx) ? View.VISIBLE : View.GONE);

        if (!Prefs.burnIn(ctx)) {
            clockContainer.setTranslationX(0);
            clockContainer.setTranslationY(0);
        }
        lastShiftMs = System.currentTimeMillis();
        lastSecond = -1;
        lastWeatherRenderAt = -1;
        lastSchedMinute = -1;
        lastAppliedBrightnessPct = -1;
        applyLayoutPosition();
        renderWeather();
    }

    /** Give the face the host window so schedules can drive its brightness. */
    public void setWindow(Window w) {
        window = w;
        lastAppliedBrightnessPct = -1;
    }

    /** Arrange clock block vs weather per the position preference. */
    private void applyLayoutPosition() {
        clockContainer.removeView(clockBlock);
        clockContainer.removeView(weatherContainer);
        String pos = Prefs.clockPos(ctx);
        float density = ctx.getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (Prefs.POS_LEFT.equals(pos) || Prefs.POS_RIGHT.equals(pos)) {
            clockContainer.setOrientation(LinearLayout.HORIZONTAL);
            wp.leftMargin = Math.round(28 * density);
            wp.topMargin = 0;
            weatherContainer.setLayoutParams(wp);
            if (Prefs.POS_LEFT.equals(pos)) {
                clockContainer.addView(clockBlock);
                clockContainer.addView(weatherContainer);
            } else {
                clockContainer.addView(weatherContainer);
                clockContainer.addView(clockBlock);
            }
        } else {
            clockContainer.setOrientation(LinearLayout.VERTICAL);
            wp.leftMargin = 0;
            wp.topMargin = Math.round(16 * density);
            weatherContainer.setLayoutParams(wp);
            if (Prefs.POS_BOTTOM.equals(pos)) {
                clockContainer.addView(weatherContainer);
                clockContainer.addView(clockBlock);
            } else {
                clockContainer.addView(clockBlock);
                clockContainer.addView(weatherContainer);
            }
        }
    }

    /**
     * Once a minute: honour the daily off-schedule (blank the face) and
     * apply the effective (possibly night-scheduled) brightness.
     */
    private void applySchedule() {
        int minute = Prefs.nowMinutes();
        if (minute == lastSchedMinute) return;
        lastSchedMinute = minute;

        boolean off = Prefs.isOffNow(ctx);
        if (off != offNow) {
            offNow = off;
            clockContainer.setVisibility(off ? View.INVISIBLE : View.VISIBLE);
            root.setBackgroundColor(off
                    ? android.graphics.Color.BLACK : Prefs.bgColor(ctx));
        }
        if (window != null) {
            int pct = off ? 1 : Prefs.effectiveBrightness(ctx);
            if (pct != lastAppliedBrightnessPct) {
                lastAppliedBrightnessPct = pct;
                android.view.WindowManager.LayoutParams lp = window.getAttributes();
                lp.screenBrightness = pct / 100f;
                window.setAttributes(lp);
            }
        }
    }

    private Typeface typeface() {
        return Typeface.create(
                Prefs.FONT_FAMILIES[Prefs.font(ctx)],
                Prefs.bold(ctx) ? Typeface.BOLD : Typeface.NORMAL);
    }

    public void start() {
        if (running) return;
        running = true;
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
    }

    public void update() {
        applySchedule();
        Calendar now = Calendar.getInstance();
        int second = now.get(Calendar.SECOND);
        boolean analog = Prefs.STYLE_ANALOG.equals(Prefs.style(ctx));

        if (analog) {
            analogView.invalidate();
        } else if (second == lastSecond) {
            maybeShift();
            checkWeatherCache();
            return;
        }
        lastSecond = second;

        Locale locale = Locale.getDefault();
        if (!analog) {
            boolean f24 = Prefs.format24(ctx);
            boolean secs = Prefs.showSeconds(ctx);
            String pattern;
            if (f24) {
                pattern = secs ? "HH:mm:ss" : "HH:mm";
            } else {
                pattern = secs ? "h:mm:ss" : "h:mm";
            }
            timeText.setText(new SimpleDateFormat(pattern, locale).format(now.getTime()));
            if (f24) {
                amPmText.setVisibility(View.GONE);
            } else {
                amPmText.setVisibility(View.VISIBLE);
                amPmText.setText(new SimpleDateFormat("a", locale).format(now.getTime()));
            }
        }

        if (Prefs.showDate(ctx)) {
            dateText.setText(new SimpleDateFormat("EEEE, MMMM d", locale).format(now.getTime()));
        }

        long nowMs = System.currentTimeMillis();
        if (Prefs.showBattery(ctx) && nowMs - lastBatteryCheckMs > 5000) {
            lastBatteryCheckMs = nowMs;
            updateBattery();
        }

        checkWeatherCache();
        maybeShift();
    }

    private void checkWeatherCache() {
        if (Prefs.wxEnabled(ctx)
                && WeatherStore.cacheTime(ctx) != lastWeatherRenderAt) {
            renderWeather();
        }
    }

    private void updateBattery() {
        Intent intent = ctx.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (level < 0 || scale <= 0) return;
        int pct = Math.round(level * 100f / scale);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        batteryText.setText(charging ? pct + "% ⚡ charging" : pct + "%");
    }

    public void renderWeather() {
        weatherContainer.removeAllViews();
        if (!Prefs.wxEnabled(ctx)) {
            weatherContainer.setVisibility(View.GONE);
            return;
        }
        WeatherStore.Model m = WeatherStore.load(ctx);
        lastWeatherRenderAt = WeatherStore.cacheTime(ctx);
        if (m == null) {
            weatherContainer.setVisibility(View.GONE);
            return;
        }
        weatherContainer.setVisibility(View.VISIBLE);

        int wxColor = Prefs.wxColor(ctx);
        float wxSize = Prefs.wxSize(ctx);
        Typeface tf = typeface();
        String deg = m.unit.equals("f") ? "°F" : "°C";
        boolean useIcons = !Prefs.ICON_NONE.equals(Prefs.wxIconStyle(ctx));

        if (m.hasCurrent && (Prefs.wxShowCurrent(ctx) || Prefs.wxShowCondition(ctx))) {
            StringBuilder sb = new StringBuilder();
            if (Prefs.wxShowCondition(ctx) && useIcons) {
                sb.append(WeatherStore.kindIcon(ctx, m.currentKind)).append(" ");
            }
            if (Prefs.wxShowCurrent(ctx)) {
                sb.append(Math.round(m.currentTemp)).append(deg);
            }
            if (Prefs.wxShowCondition(ctx)) {
                if (Prefs.wxShowCurrent(ctx)) sb.append(" · ");
                sb.append(WeatherStore.kindLabel(m.currentKind));
            }
            sb.append("  ·  ").append(shortPlace(Prefs.wxPlaceName(ctx)));
            weatherContainer.addView(wxText(sb.toString(), wxColor, wxSize, tf, 0));
        }

        int days = Math.min(Prefs.wxDays(ctx), m.names.length);
        boolean showCond = Prefs.wxShowCondition(ctx);
        boolean showMax = Prefs.wxShowMax(ctx);
        boolean showMin = Prefs.wxShowMin(ctx);
        boolean showPrecip = Prefs.wxShowPrecip(ctx);
        for (int i = 0; i < days; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(i == 0 ? "Today" : m.names[i]);
            if (showCond && useIcons) {
                sb.append("  ").append(WeatherStore.kindIcon(ctx, m.kinds[i]));
            }
            if (showMax && showMin) {
                sb.append("  ").append(Math.round(m.max[i])).append("° / ")
                        .append(Math.round(m.min[i])).append("°");
            } else if (showMax) {
                sb.append("  ").append(Math.round(m.max[i])).append("°");
            } else if (showMin) {
                sb.append("  ").append(Math.round(m.min[i])).append("°");
            }
            if (showPrecip && m.pop[i] >= 0) {
                sb.append("  💧").append(m.pop[i]).append("%");
            }
            weatherContainer.addView(wxText(sb.toString(), wxColor, wxSize, tf, 4));
        }
    }

    private TextView wxText(String text, int color, float sizeSp, Typeface tf, int topDp) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        tv.setTypeface(tf);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = Math.round(topDp * ctx.getResources().getDisplayMetrics().density);
        tv.setLayoutParams(params);
        return tv;
    }

    private String shortPlace(String place) {
        if (place == null) return "";
        int comma = place.indexOf(',');
        return comma > 0 ? place.substring(0, comma) : place;
    }

    private void maybeShift() {
        if (!Prefs.burnIn(ctx)) return;
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastShiftMs < Prefs.burnInterval(ctx) * 1000L) return;
        lastShiftMs = nowMs;
        int dist = Prefs.burnDistance(ctx);
        if (dist <= 0) return;
        float density = ctx.getResources().getDisplayMetrics().density;
        float dx = (random.nextInt(dist * 2 + 1) - dist) * density;
        float dy = (random.nextInt(dist * 2 + 1) - dist) * density;
        clockContainer.animate().translationX(dx).translationY(dy)
                .setDuration(1200).start();
    }
}
