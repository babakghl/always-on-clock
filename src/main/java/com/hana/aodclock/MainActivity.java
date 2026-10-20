package com.hana.aodclock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {
    private ClockFace face;
    private boolean finishing = false;

    private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())
                    && Prefs.onlyCharging(MainActivity.this)) {
                exitClock();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        face = new ClockFace(this, findViewById(R.id.root));
        findViewById(R.id.settingsBtn).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        applyImmersive();
        maybeStartService();
        maybeRequestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyOrientation();
        applyWindowPrefs();
        applyImmersive();
        face.applyPrefs();
        face.start();
        registerReceiver(powerReceiver,
                new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
        // Charging-only mode: leave if we're not plugged in
        if (Prefs.onlyCharging(this) && !ClockFace.isCharging(this)) {
            Toast.makeText(this,
                    "Charging-only mode is on — plug in to show the clock",
                    Toast.LENGTH_LONG).show();
            exitClock();
        }
        // If the background service is off, refresh weather from here
        if (Prefs.wxEnabled(this) && !Prefs.serviceEnabled(this)
                && WeatherStore.isStale(this)) {
            WeatherStore.refreshAsync(this,
                    () -> runOnUiThread(() -> face.renderWeather()));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        face.stop();
        try {
            unregisterReceiver(powerReceiver);
        } catch (Exception ignored) { }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    private void exitClock() {
        if (finishing) return;
        finishing = true;
        finish();
    }

    private void applyOrientation() {
        String o = Prefs.orientation(this);
        if (Prefs.ORIENT_PORTRAIT.equals(o)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (Prefs.ORIENT_LANDSCAPE.equals(o)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private void applyWindowPrefs() {
        if (Prefs.keepOn(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = Prefs.brightness(this) / 100f;
        getWindow().setAttributes(lp);
    }

    private void applyImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void maybeStartService() {
        if (Prefs.serviceEnabled(this)) {
            startForegroundService(new Intent(this, ClockService.class));
        }
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{"android.permission.POST_NOTIFICATIONS"}, 1001);
        }
    }
}
