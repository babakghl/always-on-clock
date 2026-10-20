package com.hana.aodclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class Prefs {
    private static final String NAME = "aod_prefs";

    public static final String KEY_STYLE = "style";
    public static final String KEY_FORMAT24 = "format24";
    public static final String KEY_SECONDS = "show_seconds";
    public static final String KEY_DATE = "show_date";
    public static final String KEY_BATTERY = "show_battery";
    public static final String KEY_BOLD = "bold";
    public static final String KEY_COLOR = "color";
    public static final String KEY_BG = "bg_color";
    public static final String KEY_SIZE = "font_size";
    public static final String KEY_FONT = "font";
    public static final String KEY_BRIGHTNESS = "brightness";
    public static final String KEY_KEEP_ON = "keep_on";
    public static final String KEY_BURN_IN = "burn_in";

    // Weather
    public static final String KEY_WX_ENABLED = "wx_enabled";
    public static final String KEY_WX_SOURCE = "wx_source"; // openmeteo | openweather
    public static final String KEY_WX_API_KEY = "wx_api_key";
    public static final String KEY_WX_CITY = "wx_city";
    public static final String KEY_WX_PLACE_NAME = "wx_place_name";
    public static final String KEY_WX_LAT = "wx_lat";
    public static final String KEY_WX_LON = "wx_lon";
    public static final String KEY_WX_USE_DEVICE_LOC = "wx_use_device_loc";
    public static final String KEY_WX_DAYS = "wx_days";
    public static final String KEY_WX_SHOW_CURRENT = "wx_show_current";
    public static final String KEY_WX_SHOW_CONDITION = "wx_show_condition";
    public static final String KEY_WX_SHOW_MAX = "wx_show_max";
    public static final String KEY_WX_SHOW_MIN = "wx_show_min";
    public static final String KEY_WX_SHOW_PRECIP = "wx_show_precip";
    public static final String KEY_WX_FAHRENHEIT = "wx_fahrenheit";
    public static final String KEY_WX_REFRESH_MIN = "wx_refresh_min";
    public static final String KEY_WX_SIZE = "wx_size";
    public static final String KEY_WX_COLOR = "wx_color";
    public static final String KEY_SERVICE = "service_enabled";
    public static final String KEY_WX_CACHE = "wx_cache";
    public static final String KEY_WX_CACHE_AT = "wx_cache_at";

    public static final String SOURCE_OPENMETEO = "openmeteo";
    public static final String SOURCE_OPENWEATHER = "openweather";

    // Display & behaviour (v3)
    public static final String KEY_ORIENTATION = "orientation"; // auto | portrait | landscape
    public static final String KEY_ONLY_CHARGING = "only_charging";
    public static final String KEY_BURN_DISTANCE = "burn_distance"; // dp, 0..60
    public static final String KEY_BURN_INTERVAL = "burn_interval"; // seconds
    public static final String KEY_WX_ICON_STYLE = "wx_icon_style"; // emoji | symbol | none

    public static final String ORIENT_AUTO = "auto";
    public static final String ORIENT_PORTRAIT = "portrait";
    public static final String ORIENT_LANDSCAPE = "landscape";

    public static final String ICON_EMOJI = "emoji";
    public static final String ICON_SYMBOL = "symbol";
    public static final String ICON_NONE = "none";

    public static final double DEFAULT_LAT = 45.4215; // Ottawa
    public static final double DEFAULT_LON = -75.6972;

    public static final String STYLE_DIGITAL = "digital";
    public static final String STYLE_ANALOG = "analog";

    public static final int[] CLOCK_COLORS = {
            Color.parseColor("#FFFFFF"), Color.parseColor("#E8E8E8"),
            Color.parseColor("#00E676"), Color.parseColor("#40C4FF"),
            Color.parseColor("#448AFF"), Color.parseColor("#B388FF"),
            Color.parseColor("#FF4081"), Color.parseColor("#FF5252"),
            Color.parseColor("#FF9100"), Color.parseColor("#FFD740"),
            Color.parseColor("#C6FF00"), Color.parseColor("#64FFDA")
    };

    public static final int[] BG_COLORS = {
            Color.parseColor("#000000"), Color.parseColor("#0D0D0D"),
            Color.parseColor("#0A0F1E"), Color.parseColor("#07130C"),
            Color.parseColor("#140A18"), Color.parseColor("#120C07")
    };

    public static final String[] FONT_NAMES = {
            "Default", "Monospace", "Serif", "Light", "Thin", "Condensed"
    };

    public static final String[] FONT_FAMILIES = {
            "sans-serif", "monospace", "serif",
            "sans-serif-light", "sans-serif-thin", "sans-serif-condensed"
    };

    public static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String style(Context c) {
        return sp(c).getString(KEY_STYLE, STYLE_DIGITAL);
    }

    public static boolean format24(Context c) {
        return sp(c).getBoolean(KEY_FORMAT24, false);
    }

    public static boolean showSeconds(Context c) {
        return sp(c).getBoolean(KEY_SECONDS, false);
    }

    public static boolean showDate(Context c) {
        return sp(c).getBoolean(KEY_DATE, true);
    }

    public static boolean showBattery(Context c) {
        return sp(c).getBoolean(KEY_BATTERY, true);
    }

    public static boolean bold(Context c) {
        return sp(c).getBoolean(KEY_BOLD, false);
    }

    public static int color(Context c) {
        return sp(c).getInt(KEY_COLOR, Color.WHITE);
    }

    public static int bgColor(Context c) {
        return sp(c).getInt(KEY_BG, Color.BLACK);
    }

    public static int fontSize(Context c) {
        return sp(c).getInt(KEY_SIZE, 96);
    }

    public static int font(Context c) {
        return sp(c).getInt(KEY_FONT, 0);
    }

    public static int brightness(Context c) {
        return sp(c).getInt(KEY_BRIGHTNESS, 45);
    }

    public static boolean keepOn(Context c) {
        return sp(c).getBoolean(KEY_KEEP_ON, true);
    }

    public static boolean burnIn(Context c) {
        return sp(c).getBoolean(KEY_BURN_IN, true);
    }

    // --- Weather accessors ---

    public static boolean wxEnabled(Context c) {
        return sp(c).getBoolean(KEY_WX_ENABLED, true);
    }

    public static String wxSource(Context c) {
        return sp(c).getString(KEY_WX_SOURCE, SOURCE_OPENMETEO);
    }

    public static String wxApiKey(Context c) {
        return sp(c).getString(KEY_WX_API_KEY, "");
    }

    public static String wxCity(Context c) {
        return sp(c).getString(KEY_WX_CITY, "Ottawa");
    }

    public static String wxPlaceName(Context c) {
        return sp(c).getString(KEY_WX_PLACE_NAME, "Ottawa, Canada");
    }

    public static double wxLat(Context c) {
        return Double.longBitsToDouble(sp(c).getLong(KEY_WX_LAT,
                Double.doubleToLongBits(DEFAULT_LAT)));
    }

    public static double wxLon(Context c) {
        return Double.longBitsToDouble(sp(c).getLong(KEY_WX_LON,
                Double.doubleToLongBits(DEFAULT_LON)));
    }

    public static void setWxCoords(Context c, double lat, double lon, String placeName) {
        sp(c).edit()
                .putLong(KEY_WX_LAT, Double.doubleToLongBits(lat))
                .putLong(KEY_WX_LON, Double.doubleToLongBits(lon))
                .putString(KEY_WX_PLACE_NAME, placeName)
                .putLong(KEY_WX_CACHE_AT, 0)
                .apply();
    }

    public static boolean wxUseDeviceLoc(Context c) {
        return sp(c).getBoolean(KEY_WX_USE_DEVICE_LOC, false);
    }

    public static int wxDays(Context c) {
        return sp(c).getInt(KEY_WX_DAYS, 3);
    }

    public static boolean wxShowCurrent(Context c) {
        return sp(c).getBoolean(KEY_WX_SHOW_CURRENT, true);
    }

    public static boolean wxShowCondition(Context c) {
        return sp(c).getBoolean(KEY_WX_SHOW_CONDITION, true);
    }

    public static boolean wxShowMax(Context c) {
        return sp(c).getBoolean(KEY_WX_SHOW_MAX, true);
    }

    public static boolean wxShowMin(Context c) {
        return sp(c).getBoolean(KEY_WX_SHOW_MIN, true);
    }

    public static boolean wxShowPrecip(Context c) {
        return sp(c).getBoolean(KEY_WX_SHOW_PRECIP, true);
    }

    public static boolean wxFahrenheit(Context c) {
        return sp(c).getBoolean(KEY_WX_FAHRENHEIT, false);
    }

    public static int wxRefreshMin(Context c) {
        return sp(c).getInt(KEY_WX_REFRESH_MIN, 30);
    }

    public static int wxSize(Context c) {
        return sp(c).getInt(KEY_WX_SIZE, 17);
    }

    public static int wxColor(Context c) {
        return sp(c).getInt(KEY_WX_COLOR, Color.parseColor("#B3E5FC"));
    }

    public static boolean serviceEnabled(Context c) {
        return sp(c).getBoolean(KEY_SERVICE, true);
    }

    public static String orientation(Context c) {
        return sp(c).getString(KEY_ORIENTATION, ORIENT_AUTO);
    }

    public static boolean onlyCharging(Context c) {
        return sp(c).getBoolean(KEY_ONLY_CHARGING, false);
    }

    public static int burnDistance(Context c) {
        return sp(c).getInt(KEY_BURN_DISTANCE, 24);
    }

    public static int burnInterval(Context c) {
        return sp(c).getInt(KEY_BURN_INTERVAL, 60);
    }

    public static String wxIconStyle(Context c) {
        return sp(c).getString(KEY_WX_ICON_STYLE, ICON_EMOJI);
    }

    public static void reset(Context c) {
        sp(c).edit().clear().apply();
    }
}
