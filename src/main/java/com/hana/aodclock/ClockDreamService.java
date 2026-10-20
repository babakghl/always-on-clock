package com.hana.aodclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.WindowManager;

/**
 * Screen-saver (Daydream) implementation — this is how the clock can take
 * over as the phone's idle display: Android starts it automatically when
 * the phone is charging/docked and the screen times out, once the user
 * picks "Always On Clock" in Settings → Display → Screen saver.
 */
public class ClockDreamService extends DreamService {
    private ClockFace face;

    private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())
                    && Prefs.onlyCharging(ClockDreamService.this)) {
                finish();
            }
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        setContentView(R.layout.activity_main);

        View gear = findViewById(R.id.settingsBtn);
        if (gear != null) gear.setVisibility(View.GONE);

        face = new ClockFace(this, findViewById(R.id.root));
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        // Honour the in-app brightness preference on the dream window too
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = Prefs.brightness(this) / 100f;
        getWindow().setAttributes(lp);

        if (Prefs.onlyCharging(this) && !ClockFace.isCharging(this)) {
            finish();
            return;
        }
        registerReceiver(powerReceiver,
                new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
        face.applyPrefs();
        face.start();
        if (Prefs.wxEnabled(this) && WeatherStore.isStale(this)) {
            WeatherStore.refreshAsync(this, null);
        }
    }

    @Override
    public void onDreamingStopped() {
        if (face != null) face.stop();
        try {
            unregisterReceiver(powerReceiver);
        } catch (Exception ignored) { }
        super.onDreamingStopped();
    }
}
