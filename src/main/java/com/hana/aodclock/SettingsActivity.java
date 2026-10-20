package com.hana.aodclock;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.AdapterView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends Activity {

    private final Handler previewHandler = new Handler(Looper.getMainLooper());
    private final Runnable previewTick = new Runnable() {
        @Override
        public void run() {
            renderPreview();
            previewHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        previewHandler.removeCallbacks(previewTick);
        previewHandler.post(previewTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        previewHandler.removeCallbacks(previewTick);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SharedPreferences sp = Prefs.sp(this);

        // --- Clock style ---
        RadioGroup styleGroup = findViewById(R.id.styleGroup);
        if (Prefs.STYLE_ANALOG.equals(Prefs.style(this))) {
            styleGroup.check(R.id.radioAnalog);
        } else {
            styleGroup.check(R.id.radioDigital);
        }
        styleGroup.setOnCheckedChangeListener((group, checkedId) ->
                sp.edit().putString(Prefs.KEY_STYLE,
                        checkedId == R.id.radioAnalog
                                ? Prefs.STYLE_ANALOG : Prefs.STYLE_DIGITAL).apply());

        // --- Switches ---
        bindSwitch(sp, R.id.swFormat24, Prefs.KEY_FORMAT24, Prefs.format24(this));
        bindSwitch(sp, R.id.swSeconds, Prefs.KEY_SECONDS, Prefs.showSeconds(this));
        bindSwitch(sp, R.id.swDate, Prefs.KEY_DATE, Prefs.showDate(this));
        bindSwitch(sp, R.id.swBattery, Prefs.KEY_BATTERY, Prefs.showBattery(this));
        bindSwitch(sp, R.id.swBold, Prefs.KEY_BOLD, Prefs.bold(this));
        bindSwitch(sp, R.id.swKeepOn, Prefs.KEY_KEEP_ON, Prefs.keepOn(this));
        bindSwitch(sp, R.id.swBurnIn, Prefs.KEY_BURN_IN, Prefs.burnIn(this));

        // --- Clock size (40..160 sp) ---
        TextView sizeLabel = findViewById(R.id.sizeLabel);
        SeekBar sbSize = findViewById(R.id.sbSize);
        sbSize.setProgress(Prefs.fontSize(this) - 40);
        sizeLabel.setText("Clock size: " + Prefs.fontSize(this) + "sp");
        sbSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int size = progress + 40;
                sizeLabel.setText("Clock size: " + size + "sp");
                sp.edit().putInt(Prefs.KEY_SIZE, size).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) { }

            @Override
            public void onStopTrackingTouch(SeekBar bar) { }
        });

        // --- Colour swatches ---
        buildSwatches(sp, R.id.colorRow, Prefs.CLOCK_COLORS, Prefs.KEY_COLOR, Prefs.color(this));
        buildSwatches(sp, R.id.bgRow, Prefs.BG_COLORS, Prefs.KEY_BG, Prefs.bgColor(this));

        // --- Font spinner ---
        Spinner spFont = findViewById(R.id.spFont);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Prefs.FONT_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFont.setAdapter(adapter);
        spFont.setSelection(Prefs.font(this));
        spFont.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sp.edit().putInt(Prefs.KEY_FONT, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // --- Brightness (5..100%) ---
        TextView brightnessLabel = findViewById(R.id.brightnessLabel);
        SeekBar sbBrightness = findViewById(R.id.sbBrightness);
        sbBrightness.setProgress(Prefs.brightness(this) - 5);
        brightnessLabel.setText("Screen brightness: " + Prefs.brightness(this) + "%");
        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int value = progress + 5;
                brightnessLabel.setText("Screen brightness: " + value + "%");
                sp.edit().putInt(Prefs.KEY_BRIGHTNESS, value).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) { }

            @Override
            public void onStopTrackingTouch(SeekBar bar) { }
        });

        // --- Weather ---
        bindSwitch(sp, R.id.swWxEnabled, Prefs.KEY_WX_ENABLED, Prefs.wxEnabled(this));
        bindSwitch(sp, R.id.swWxCurrent, Prefs.KEY_WX_SHOW_CURRENT, Prefs.wxShowCurrent(this));
        bindSwitch(sp, R.id.swWxCondition, Prefs.KEY_WX_SHOW_CONDITION, Prefs.wxShowCondition(this));
        bindSwitch(sp, R.id.swWxMax, Prefs.KEY_WX_SHOW_MAX, Prefs.wxShowMax(this));
        bindSwitch(sp, R.id.swWxMin, Prefs.KEY_WX_SHOW_MIN, Prefs.wxShowMin(this));
        bindSwitch(sp, R.id.swWxPrecip, Prefs.KEY_WX_SHOW_PRECIP, Prefs.wxShowPrecip(this));
        bindSwitch(sp, R.id.swWxFahrenheit, Prefs.KEY_WX_FAHRENHEIT, Prefs.wxFahrenheit(this));

        // Data source
        Spinner spWxSource = findViewById(R.id.spWxSource);
        String[] sourceNames = {"Open-Meteo (free, no key)", "OpenWeatherMap (needs API key)"};
        ArrayAdapter<String> wxAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sourceNames);
        wxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWxSource.setAdapter(wxAdapter);
        spWxSource.setSelection(Prefs.SOURCE_OPENWEATHER.equals(Prefs.wxSource(this)) ? 1 : 0);
        android.widget.EditText etApiKey = findViewById(R.id.etApiKey);
        etApiKey.setText(Prefs.wxApiKey(this));
        etApiKey.setVisibility(Prefs.SOURCE_OPENWEATHER.equals(Prefs.wxSource(this))
                ? View.VISIBLE : View.GONE);
        spWxSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String src = position == 1 ? Prefs.SOURCE_OPENWEATHER : Prefs.SOURCE_OPENMETEO;
                sp.edit().putString(Prefs.KEY_WX_SOURCE, src)
                        .putLong(Prefs.KEY_WX_CACHE_AT, 0).apply();
                etApiKey.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        etApiKey.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                sp.edit().putString(Prefs.KEY_WX_API_KEY, s.toString().trim())
                        .putLong(Prefs.KEY_WX_CACHE_AT, 0).apply();
            }
        });

        // Location
        TextView tvWxPlace = findViewById(R.id.tvWxPlace);
        tvWxPlace.setText("Location: " + Prefs.wxPlaceName(this));
        android.widget.EditText etCity = findViewById(R.id.etCity);
        etCity.setText(Prefs.wxCity(this));
        findViewById(R.id.btnSetCity).setOnClickListener(v -> {
            String city = etCity.getText().toString().trim();
            if (city.isEmpty()) return;
            sp.edit().putString(Prefs.KEY_WX_CITY, city)
                    .putBoolean(Prefs.KEY_WX_USE_DEVICE_LOC, false).apply();
            Switch swDev = findViewById(R.id.swDeviceLoc);
            swDev.setChecked(false);
            android.widget.Toast.makeText(this, "Looking up " + city + "…",
                    android.widget.Toast.LENGTH_SHORT).show();
            WeatherStore.geocodeAsync(this, city,
                    coords -> runOnUiThread(() -> {
                        tvWxPlace.setText("Location: " + Prefs.wxPlaceName(this));
                        android.widget.Toast.makeText(this,
                                "Location set: " + Prefs.wxPlaceName(this),
                                android.widget.Toast.LENGTH_SHORT).show();
                        WeatherStore.refreshAsync(this, null);
                    }),
                    () -> runOnUiThread(() -> android.widget.Toast.makeText(this,
                            "Couldn't find that city", android.widget.Toast.LENGTH_SHORT).show()));
        });

        // Device location
        Switch swDeviceLoc = findViewById(R.id.swDeviceLoc);
        swDeviceLoc.setChecked(Prefs.wxUseDeviceLoc(this));
        swDeviceLoc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean(Prefs.KEY_WX_USE_DEVICE_LOC, isChecked).apply();
            if (isChecked) {
                if (checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION")
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, 2002);
                } else {
                    captureDeviceLocation(tvWxPlace);
                }
            }
        });

        // Forecast days (1..7)
        TextView wxDaysLabel = findViewById(R.id.wxDaysLabel);
        SeekBar sbWxDays = findViewById(R.id.sbWxDays);
        sbWxDays.setProgress(Prefs.wxDays(this) - 1);
        wxDaysLabel.setText("Forecast days: " + Prefs.wxDays(this));
        sbWxDays.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int days = progress + 1;
                wxDaysLabel.setText("Forecast days: " + days);
                sp.edit().putInt(Prefs.KEY_WX_DAYS, days).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) { }

            @Override
            public void onStopTrackingTouch(SeekBar bar) { }
        });

        // Refresh interval
        Spinner spWxRefresh = findViewById(R.id.spWxRefresh);
        String[] refreshNames = {"15 minutes", "30 minutes", "1 hour", "3 hours"};
        int[] refreshValues = {15, 30, 60, 180};
        ArrayAdapter<String> refreshAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, refreshNames);
        refreshAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWxRefresh.setAdapter(refreshAdapter);
        int sel = 1;
        for (int i = 0; i < refreshValues.length; i++) {
            if (refreshValues[i] == Prefs.wxRefreshMin(this)) sel = i;
        }
        spWxRefresh.setSelection(sel);
        spWxRefresh.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sp.edit().putInt(Prefs.KEY_WX_REFRESH_MIN, refreshValues[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Weather text size (10..30 sp)
        TextView wxSizeLabel = findViewById(R.id.wxSizeLabel);
        SeekBar sbWxSize = findViewById(R.id.sbWxSize);
        sbWxSize.setProgress(Prefs.wxSize(this) - 10);
        wxSizeLabel.setText("Weather text size: " + Prefs.wxSize(this) + "sp");
        sbWxSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int size = progress + 10;
                wxSizeLabel.setText("Weather text size: " + size + "sp");
                sp.edit().putInt(Prefs.KEY_WX_SIZE, size).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) { }

            @Override
            public void onStopTrackingTouch(SeekBar bar) { }
        });

        // Weather colour
        buildSwatches(sp, R.id.wxColorRow, Prefs.CLOCK_COLORS, Prefs.KEY_WX_COLOR, Prefs.wxColor(this));

        // --- Background service ---
        Switch swService = findViewById(R.id.swService);
        swService.setChecked(Prefs.serviceEnabled(this));
        swService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean(Prefs.KEY_SERVICE, isChecked).apply();
            android.content.Intent svc = new android.content.Intent(this, ClockService.class);
            if (isChecked) {
                startForegroundService(svc);
            } else {
                stopService(svc);
            }
        });

        // --- Categories (collapsible sections) ---
        setupSection(sp, R.id.secClockHeader, R.id.secClockBody,
                "sec_open_clock", "CLOCK", true);
        setupSection(sp, R.id.secAppearanceHeader, R.id.secAppearanceBody,
                "sec_open_appearance", "APPEARANCE", true);
        setupSection(sp, R.id.secWeatherHeader, R.id.secWeatherBody,
                "sec_open_weather", "WEATHER", false);
        setupSection(sp, R.id.secDisplayHeader, R.id.secDisplayBody,
                "sec_open_display", "DISPLAY & BEHAVIOUR", false);
        setupSection(sp, R.id.secBackgroundHeader, R.id.secBackgroundBody,
                "sec_open_background", "BACKGROUND", false);

        // --- Display & behaviour extras ---
        bindSwitch(sp, R.id.swOnlyCharging, Prefs.KEY_ONLY_CHARGING, Prefs.onlyCharging(this));

        Spinner spOrientation = findViewById(R.id.spOrientation);
        String[] orientNames = {"Auto (system)", "Portrait", "Landscape"};
        String[] orientValues = {Prefs.ORIENT_AUTO, Prefs.ORIENT_PORTRAIT, Prefs.ORIENT_LANDSCAPE};
        ArrayAdapter<String> orientAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, orientNames);
        orientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrientation.setAdapter(orientAdapter);
        int oSel = 0;
        for (int i = 0; i < orientValues.length; i++) {
            if (orientValues[i].equals(Prefs.orientation(this))) oSel = i;
        }
        spOrientation.setSelection(oSel);
        spOrientation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sp.edit().putString(Prefs.KEY_ORIENTATION, orientValues[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Burn-in distance (0..60 dp)
        TextView burnDistLabel = findViewById(R.id.burnDistLabel);
        SeekBar sbBurnDist = findViewById(R.id.sbBurnDist);
        sbBurnDist.setProgress(Prefs.burnDistance(this));
        burnDistLabel.setText("Move distance: " + Prefs.burnDistance(this) + "dp");
        sbBurnDist.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                burnDistLabel.setText("Move distance: " + progress + "dp");
                sp.edit().putInt(Prefs.KEY_BURN_DISTANCE, progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) { }

            @Override
            public void onStopTrackingTouch(SeekBar bar) { }
        });

        // Burn-in interval
        Spinner spBurnInterval = findViewById(R.id.spBurnInterval);
        String[] burnNames = {"30 seconds", "1 minute", "2 minutes", "5 minutes"};
        int[] burnValues = {30, 60, 120, 300};
        ArrayAdapter<String> burnAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, burnNames);
        burnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBurnInterval.setAdapter(burnAdapter);
        int bSel = 1;
        for (int i = 0; i < burnValues.length; i++) {
            if (burnValues[i] == Prefs.burnInterval(this)) bSel = i;
        }
        spBurnInterval.setSelection(bSel);
        spBurnInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sp.edit().putInt(Prefs.KEY_BURN_INTERVAL, burnValues[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Weather icon style
        Spinner spWxIcon = findViewById(R.id.spWxIcon);
        String[] iconNames = {"Emoji", "Simple symbols", "No icons"};
        String[] iconValues = {Prefs.ICON_EMOJI, Prefs.ICON_SYMBOL, Prefs.ICON_NONE};
        ArrayAdapter<String> iconAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, iconNames);
        iconAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWxIcon.setAdapter(iconAdapter);
        int iSel = 0;
        for (int i = 0; i < iconValues.length; i++) {
            if (iconValues[i].equals(Prefs.wxIconStyle(this))) iSel = i;
        }
        spWxIcon.setSelection(iSel);
        spWxIcon.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sp.edit().putString(Prefs.KEY_WX_ICON_STYLE, iconValues[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Screen saver (AOD replacement) settings
        findViewById(R.id.btnDreamSettings).setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent("android.settings.DREAM_SETTINGS"));
            } catch (Exception e) {
                try {
                    startActivity(new android.content.Intent(
                            android.provider.Settings.ACTION_DISPLAY_SETTINGS));
                } catch (Exception e2) {
                    android.widget.Toast.makeText(this,
                            "Open Settings → Display → Screen saver on your phone",
                            android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });

        renderPreview();

        // --- Buttons ---
        findViewById(R.id.btnReset).setOnClickListener(v -> {
            Prefs.reset(this);
            recreate();
        });
        findViewById(R.id.btnDone).setOnClickListener(v -> finish());
    }

    private void bindSwitch(SharedPreferences sp, int id, String key, boolean current) {
        Switch sw = findViewById(id);
        sw.setChecked(current);
        sw.setOnCheckedChangeListener((buttonView, isChecked) ->
                sp.edit().putBoolean(key, isChecked).apply());
    }

    private void buildSwatches(SharedPreferences sp, int rowId, int[] colors,
                               String key, int selected) {
        LinearLayout row = findViewById(rowId);
        row.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int size = Math.round(46 * density);
        int margin = Math.round(6 * density);
        for (int color : colors) {
            View swatch = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            swatch.setLayoutParams(params);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            if (color == selected) {
                bg.setStroke(Math.round(3 * density), Color.parseColor("#4FC3F7"));
            } else {
                bg.setStroke(Math.round(1 * density), Color.parseColor("#666666"));
            }
            swatch.setBackground(bg);
            swatch.setOnClickListener(v -> {
                sp.edit().putInt(key, color).apply();
                buildSwatches(sp, rowId, colors, key, color);
            });
            row.addView(swatch);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2002) {
            TextView tvWxPlace = findViewById(R.id.tvWxPlace);
            if (grantResults.length > 0
                    && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                captureDeviceLocation(tvWxPlace);
            } else {
                Switch swDev = findViewById(R.id.swDeviceLoc);
                swDev.setChecked(false);
                Prefs.sp(this).edit()
                        .putBoolean(Prefs.KEY_WX_USE_DEVICE_LOC, false).apply();
                android.widget.Toast.makeText(this,
                        "Location permission denied — using city instead",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void captureDeviceLocation(TextView tvWxPlace) {
        try {
            android.location.LocationManager lm =
                    (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
            android.location.Location loc = null;
            if (lm != null) {
                loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                if (loc == null) {
                    loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                }
            }
            if (loc != null) {
                Prefs.setWxCoords(this, loc.getLatitude(), loc.getLongitude(),
                        "Device location");
                tvWxPlace.setText("Location: Device location");
                android.widget.Toast.makeText(this, "Using device location",
                        android.widget.Toast.LENGTH_SHORT).show();
                WeatherStore.refreshAsync(this, null);
            } else {
                android.widget.Toast.makeText(this,
                        "Couldn't get device location yet — try again shortly",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            android.widget.Toast.makeText(this, "Location permission needed",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSection(SharedPreferences sp, int headerId, int bodyId,
                              String key, String title, boolean defaultOpen) {
        TextView header = findViewById(headerId);
        View body = findViewById(bodyId);
        boolean open = sp.getBoolean(key, defaultOpen);
        body.setVisibility(open ? View.VISIBLE : View.GONE);
        header.setText((open ? "▾  " : "▸  ") + title);
        header.setOnClickListener(v -> {
            boolean nowOpen = body.getVisibility() != View.VISIBLE;
            body.setVisibility(nowOpen ? View.VISIBLE : View.GONE);
            header.setText((nowOpen ? "▾  " : "▸  ") + title);
            sp.edit().putBoolean(key, nowOpen).apply();
        });
    }

    private void renderPreview() {
        View previewRoot = findViewById(R.id.previewRoot);
        if (previewRoot == null) return;
        previewRoot.setBackgroundColor(Prefs.bgColor(this));

        int color = Prefs.color(this);
        boolean analog = Prefs.STYLE_ANALOG.equals(Prefs.style(this));
        Typeface tf = Typeface.create(
                Prefs.FONT_FAMILIES[Prefs.font(this)],
                Prefs.bold(this) ? Typeface.BOLD : Typeface.NORMAL);
        Calendar now = Calendar.getInstance();
        Locale locale = Locale.getDefault();

        AnalogClockView pAnalog = findViewById(R.id.previewAnalog);
        View pDigitalRow = findViewById(R.id.previewDigitalRow);
        pAnalog.setVisibility(analog ? View.VISIBLE : View.GONE);
        pDigitalRow.setVisibility(analog ? View.GONE : View.VISIBLE);
        pAnalog.setClockColor(color);
        pAnalog.setShowSeconds(Prefs.showSeconds(this));
        if (analog) pAnalog.invalidate();

        TextView pTime = findViewById(R.id.previewTime);
        TextView pAmPm = findViewById(R.id.previewAmPm);
        boolean f24 = Prefs.format24(this);
        boolean secs = Prefs.showSeconds(this);
        String pattern = f24 ? (secs ? "HH:mm:ss" : "HH:mm")
                : (secs ? "h:mm:ss" : "h:mm");
        pTime.setText(new SimpleDateFormat(pattern, locale).format(now.getTime()));
        float scaled = Math.max(20f, Math.min(60f, Prefs.fontSize(this) * 0.45f));
        pTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaled);
        pTime.setTextColor(color);
        pTime.setTypeface(tf);
        pAmPm.setVisibility(f24 || analog ? View.GONE : View.VISIBLE);
        if (!f24) {
            pAmPm.setText(new SimpleDateFormat("a", locale).format(now.getTime()));
        }
        pAmPm.setTextColor(color);
        pAmPm.setTypeface(tf);

        TextView pDate = findViewById(R.id.previewDate);
        pDate.setVisibility(Prefs.showDate(this) ? View.VISIBLE : View.GONE);
        pDate.setText(new SimpleDateFormat("EEEE, MMMM d", locale).format(now.getTime()));
        pDate.setTextColor(color);
        pDate.setTypeface(tf);

        TextView pBattery = findViewById(R.id.previewBattery);
        pBattery.setVisibility(Prefs.showBattery(this) ? View.VISIBLE : View.GONE);
        pBattery.setTextColor(color);
        pBattery.setTypeface(tf);
        if (Prefs.showBattery(this)) {
            android.content.Intent bi = registerReceiver(null, new android.content.IntentFilter(
                    android.content.Intent.ACTION_BATTERY_CHANGED));
            if (bi != null) {
                int level = bi.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = bi.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    pBattery.setText(Math.round(level * 100f / scale) + "%");
                }
            }
        }

        // Weather (compact mirror of the clock face)
        LinearLayout pWx = findViewById(R.id.previewWeather);
        pWx.removeAllViews();
        if (!Prefs.wxEnabled(this)) return;
        WeatherStore.Model m = WeatherStore.load(this);
        if (m == null) return;
        int wxColor = Prefs.wxColor(this);
        float wxSize = Math.max(9f, Prefs.wxSize(this) * 0.75f);
        String deg = m.unit.equals("f") ? "°F" : "°C";
        boolean useIcons = !Prefs.ICON_NONE.equals(Prefs.wxIconStyle(this));
        if (m.hasCurrent && (Prefs.wxShowCurrent(this) || Prefs.wxShowCondition(this))) {
            StringBuilder sb = new StringBuilder();
            if (Prefs.wxShowCondition(this) && useIcons) {
                sb.append(WeatherStore.kindIcon(this, m.currentKind)).append(" ");
            }
            if (Prefs.wxShowCurrent(this)) {
                sb.append(Math.round(m.currentTemp)).append(deg);
            }
            if (Prefs.wxShowCondition(this)) {
                if (Prefs.wxShowCurrent(this)) sb.append(" · ");
                sb.append(WeatherStore.kindLabel(m.currentKind));
            }
            pWx.addView(previewWxText(sb.toString(), wxColor, wxSize, tf));
        }
        int days = Math.min(Prefs.wxDays(this), m.names.length);
        for (int i = 0; i < days; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(i == 0 ? "Today" : m.names[i]);
            if (Prefs.wxShowCondition(this) && useIcons) {
                sb.append("  ").append(WeatherStore.kindIcon(this, m.kinds[i]));
            }
            if (Prefs.wxShowMax(this) && Prefs.wxShowMin(this)) {
                sb.append("  ").append(Math.round(m.max[i])).append("° / ")
                        .append(Math.round(m.min[i])).append("°");
            } else if (Prefs.wxShowMax(this)) {
                sb.append("  ").append(Math.round(m.max[i])).append("°");
            } else if (Prefs.wxShowMin(this)) {
                sb.append("  ").append(Math.round(m.min[i])).append("°");
            }
            if (Prefs.wxShowPrecip(this) && m.pop[i] >= 0) {
                sb.append("  💧").append(m.pop[i]).append("%");
            }
            pWx.addView(previewWxText(sb.toString(), wxColor, wxSize, tf));
        }
    }

    private TextView previewWxText(String text, int color, float sizeSp, Typeface tf) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        tv.setTypeface(tf);
        tv.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        return tv;
    }
}
