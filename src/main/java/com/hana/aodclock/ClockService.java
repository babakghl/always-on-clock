package com.hana.aodclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ClockService extends Service {
    private static final String CHANNEL_ID = "clock_status";
    private static final int NOTIF_ID = 42;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastMinuteText = "";

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!Prefs.serviceEnabled(ClockService.this)) {
                stopSelf();
                return;
            }
            updateNotification();
            // Keep weather fresh in the background
            if (Prefs.wxEnabled(ClockService.this) && WeatherStore.isStale(ClockService.this)) {
                WeatherStore.refreshAsync(ClockService.this,
                        () -> handler.post(() -> updateNotification()));
            }
            handler.postDelayed(this, 20000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Clock status", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Shows that Always On Clock is running");
        channel.setShowBadge(false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateNotification() {
        String time = new SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        if (time.equals(lastMinuteText) && WeatherStore.cacheTime(this) != 0) {
            // Still refresh occasionally; cheap enough to just notify below.
        }
        lastMinuteText = time;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification());
    }

    private Notification buildNotification() {
        Calendar now = Calendar.getInstance();
        Locale locale = Locale.getDefault();
        String time = new SimpleDateFormat("h:mm a", locale).format(now.getTime());
        String day = new SimpleDateFormat("EEE, MMM d", locale).format(now.getTime());

        StringBuilder text = new StringBuilder();
        text.append(day);
        if (Prefs.wxEnabled(this)) {
            WeatherStore.Model m = WeatherStore.load(this);
            if (m != null && m.hasCurrent) {
                text.append("  ·  ")
                        .append(WeatherStore.kindEmoji(m.currentKind))
                        .append(" ")
                        .append(Math.round(m.currentTemp))
                        .append(m.unit.equals("f") ? "°F " : "°C ")
                        .append(WeatherStore.kindLabel(m.currentKind))
                        .append(" in ")
                        .append(shortPlace(Prefs.wxPlaceName(this)));
            }
        }

        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent settings = new Intent(this, SettingsActivity.class);
        PendingIntent settingsPi = PendingIntent.getActivity(this, 1, settings,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Always On Clock · " + time)
                .setContentText(text.toString())
                .setContentIntent(pi)
                .addAction(0, "Settings", settingsPi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
    }

    private String shortPlace(String place) {
        if (place == null) return "";
        int comma = place.indexOf(',');
        return comma > 0 ? place.substring(0, comma) : place;
    }
}
