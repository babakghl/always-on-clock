package com.hana.aodclock;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherStore {
    public static final int KIND_CLEAR = 0;
    public static final int KIND_PARTLY = 1;
    public static final int KIND_CLOUDY = 2;
    public static final int KIND_FOG = 3;
    public static final int KIND_DRIZZLE = 4;
    public static final int KIND_RAIN = 5;
    public static final int KIND_SNOW = 6;
    public static final int KIND_THUNDER = 7;

    private static final String[] KIND_EMOJI = {
            "☀️", "🌤️", "☁️", "🌫️", "🌦️", "🌧️", "❄️", "⛈️"
    };

    private static final String[] KIND_LABEL = {
            "Clear", "Partly cloudy", "Cloudy", "Fog",
            "Drizzle", "Rain", "Snow", "Thunderstorm"
    };

    private static final String[] KIND_SYMBOL = {
            "☀", "⛅", "☁", "≋", "☂", "☂", "☃", "⚡"
    };

    /** Icon for a condition kind, honouring the user's icon-style preference. */
    public static String kindIcon(Context ctx, int kind) {
        String style = Prefs.wxIconStyle(ctx);
        if (Prefs.ICON_NONE.equals(style)) return "";
        if (Prefs.ICON_SYMBOL.equals(style)) {
            return (kind >= 0 && kind < KIND_SYMBOL.length) ? KIND_SYMBOL[kind] : "";
        }
        return kindEmoji(kind);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    public static String kindEmoji(int kind) {
        return (kind >= 0 && kind < KIND_EMOJI.length) ? KIND_EMOJI[kind] : "";
    }

    public static String kindLabel(int kind) {
        return (kind >= 0 && kind < KIND_LABEL.length) ? KIND_LABEL[kind] : "";
    }

    public static class Model {
        public boolean hasCurrent;
        public double currentTemp;
        public int currentKind;
        public String unit = "c";
        public String[] names = new String[0];
        public int[] kinds = new int[0];
        public double[] max = new double[0];
        public double[] min = new double[0];
        public int[] pop = new int[0]; // percent, -1 = unavailable
    }

    // ---------- cache ----------

    public static Model load(Context ctx) {
        String raw = Prefs.sp(ctx).getString(Prefs.KEY_WX_CACHE, null);
        if (raw == null) return null;
        try {
            JSONObject o = new JSONObject(raw);
            Model m = new Model();
            m.hasCurrent = o.optBoolean("hasCurrent", false);
            m.currentTemp = o.optDouble("cur", 0);
            m.currentKind = o.optInt("curKind", KIND_CLEAR);
            m.unit = o.optString("unit", "c");
            JSONArray days = o.optJSONArray("days");
            if (days != null) {
                int n = days.length();
                m.names = new String[n];
                m.kinds = new int[n];
                m.max = new double[n];
                m.min = new double[n];
                m.pop = new int[n];
                for (int i = 0; i < n; i++) {
                    JSONObject d = days.getJSONObject(i);
                    m.names[i] = d.optString("name", "");
                    m.kinds[i] = d.optInt("kind", KIND_CLEAR);
                    m.max[i] = d.optDouble("max", 0);
                    m.min[i] = d.optDouble("min", 0);
                    m.pop[i] = d.optInt("pop", -1);
                }
            }
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    public static long cacheTime(Context ctx) {
        return Prefs.sp(ctx).getLong(Prefs.KEY_WX_CACHE_AT, 0);
    }

    public static boolean isStale(Context ctx) {
        long at = cacheTime(ctx);
        if (at == 0) return true;
        // Also stale if the cached unit no longer matches the preference
        Model m = load(ctx);
        String wantUnit = Prefs.wxFahrenheit(ctx) ? "f" : "c";
        if (m != null && !wantUnit.equals(m.unit)) return true;
        return System.currentTimeMillis() - at > Prefs.wxRefreshMin(ctx) * 60000L;
    }

    // ---------- fetching ----------

    public static void refreshAsync(Context ctx, Runnable onDone) {
        final Context app = ctx.getApplicationContext();
        EXEC.execute(() -> {
            refresh(app);
            if (onDone != null) onDone.run();
        });
    }

    /** Synchronous; call from a background thread only. Returns true on success. */
    public static boolean refresh(Context ctx) {
        try {
            double lat = Prefs.wxLat(ctx);
            double lon = Prefs.wxLon(ctx);
            boolean fahrenheit = Prefs.wxFahrenheit(ctx);
            JSONObject norm;
            if (Prefs.SOURCE_OPENWEATHER.equals(Prefs.wxSource(ctx))) {
                String key = Prefs.wxApiKey(ctx).trim();
                if (key.isEmpty()) return false;
                norm = fetchOpenWeather(lat, lon, fahrenheit, key);
            } else {
                norm = fetchOpenMeteo(lat, lon, fahrenheit);
            }
            if (norm == null) return false;
            Prefs.sp(ctx).edit()
                    .putString(Prefs.KEY_WX_CACHE, norm.toString())
                    .putLong(Prefs.KEY_WX_CACHE_AT, System.currentTimeMillis())
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static JSONObject fetchOpenMeteo(double lat, double lon, boolean fahrenheit)
            throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                + "&forecast_days=7&timezone=auto"
                + (fahrenheit ? "&temperature_unit=fahrenheit" : "");
        JSONObject root = new JSONObject(httpGet(url));
        JSONObject out = baseOut(fahrenheit);

        JSONObject cur = root.optJSONObject("current");
        if (cur != null) {
            out.put("hasCurrent", true);
            out.put("cur", cur.optDouble("temperature_2m", 0));
            out.put("curKind", wmoToKind(cur.optInt("weather_code", 0)));
        }

        JSONObject daily = root.optJSONObject("daily");
        if (daily != null) {
            JSONArray time = daily.optJSONArray("time");
            JSONArray codes = daily.optJSONArray("weather_code");
            JSONArray tmax = daily.optJSONArray("temperature_2m_max");
            JSONArray tmin = daily.optJSONArray("temperature_2m_min");
            JSONArray pop = daily.optJSONArray("precipitation_probability_max");
            JSONArray days = new JSONArray();
            int n = time != null ? time.length() : 0;
            for (int i = 0; i < n; i++) {
                JSONObject d = new JSONObject();
                d.put("name", dayName(time.getString(i)));
                d.put("kind", wmoToKind(codes != null ? codes.optInt(i, 0) : 0));
                d.put("max", tmax != null ? tmax.optDouble(i, 0) : 0);
                d.put("min", tmin != null ? tmin.optDouble(i, 0) : 0);
                int p = -1;
                if (pop != null && !pop.isNull(i)) p = (int) Math.round(pop.optDouble(i, -1));
                d.put("pop", p);
                days.put(d);
            }
            out.put("days", days);
        }
        return out;
    }

    private static JSONObject fetchOpenWeather(double lat, double lon, boolean fahrenheit,
                                               String apiKey) throws Exception {
        String url = "https://api.openweathermap.org/data/3.0/onecall"
                + "?lat=" + lat + "&lon=" + lon
                + "&exclude=minutely,hourly,alerts"
                + "&units=" + (fahrenheit ? "imperial" : "metric")
                + "&appid=" + URLEncoder.encode(apiKey, "UTF-8");
        JSONObject root = new JSONObject(httpGet(url));
        JSONObject out = baseOut(fahrenheit);

        JSONObject cur = root.optJSONObject("current");
        if (cur != null) {
            out.put("hasCurrent", true);
            out.put("cur", cur.optDouble("temp", 0));
            out.put("curKind", owmToKind(iconOf(cur)));
        }

        JSONArray daily = root.optJSONArray("daily");
        if (daily != null) {
            JSONArray days = new JSONArray();
            SimpleDateFormat fmt = new SimpleDateFormat("EEE", Locale.getDefault());
            for (int i = 0; i < daily.length(); i++) {
                JSONObject src = daily.getJSONObject(i);
                JSONObject d = new JSONObject();
                long dt = src.optLong("dt", 0);
                d.put("name", dt > 0 ? fmt.format(new Date(dt * 1000L)) : "");
                d.put("kind", owmToKind(iconOf(src)));
                JSONObject temp = src.optJSONObject("temp");
                d.put("max", temp != null ? temp.optDouble("max", 0) : 0);
                d.put("min", temp != null ? temp.optDouble("min", 0) : 0);
                double popFrac = src.optDouble("pop", -1);
                d.put("pop", popFrac < 0 ? -1 : (int) Math.round(popFrac * 100));
                days.put(d);
            }
            out.put("days", days);
        }
        return out;
    }

    private static String iconOf(JSONObject obj) {
        JSONArray w = obj.optJSONArray("weather");
        if (w != null && w.length() > 0) {
            return w.optJSONObject(0) != null
                    ? w.optJSONObject(0).optString("icon", "") : "";
        }
        return "";
    }

    private static JSONObject baseOut(boolean fahrenheit) throws Exception {
        JSONObject out = new JSONObject();
        out.put("unit", fahrenheit ? "f" : "c");
        out.put("hasCurrent", false);
        return out;
    }

    // ---------- geocoding (Open-Meteo, free) ----------

    /** Returns {lat, lon, displayName} or null. Call from a background thread. */
    public static double[] geocode(Context ctx, String city, String[] nameOut) {
        try {
            String url = "https://geocoding-api.open-meteo.com/v1/search"
                    + "?name=" + URLEncoder.encode(city, "UTF-8")
                    + "&count=1&format=json&language=en";
            JSONObject root = new JSONObject(httpGet(url));
            JSONArray results = root.optJSONArray("results");
            if (results == null || results.length() == 0) return null;
            JSONObject r = results.getJSONObject(0);
            double lat = r.getDouble("latitude");
            double lon = r.getDouble("longitude");
            String name = r.optString("name", city);
            String country = r.optString("country", "");
            if (nameOut != null && nameOut.length > 0) {
                nameOut[0] = country.isEmpty() ? name : name + ", " + country;
            }
            return new double[]{lat, lon};
        } catch (Exception e) {
            return null;
        }
    }

    public static void geocodeAsync(Context ctx, String city,
                                    java.util.function.Consumer<double[]> onOk,
                                    Runnable onFail) {
        final Context app = ctx.getApplicationContext();
        EXEC.execute(() -> {
            String[] name = new String[1];
            double[] coords = geocode(app, city, name);
            if (coords != null) {
                String display = name[0] != null ? name[0] : city;
                Prefs.setWxCoords(app, coords[0], coords[1], display);
                if (onOk != null) onOk.accept(coords);
            } else if (onFail != null) {
                onFail.run();
            }
        });
    }

    // ---------- helpers ----------

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "AlwaysOnClock/2.0 (Android)");
        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();
        return sb.toString();
    }

    private static String dayName(String isoDate) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate);
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(d);
        } catch (Exception e) {
            return "";
        }
    }

    public static int wmoToKind(int code) {
        switch (code) {
            case 0: return KIND_CLEAR;
            case 1: case 2: return KIND_PARTLY;
            case 3: return KIND_CLOUDY;
            case 45: case 48: return KIND_FOG;
            case 51: case 53: case 55: case 56: case 57: return KIND_DRIZZLE;
            case 61: case 63: case 65: case 66: case 67:
            case 80: case 81: case 82: return KIND_RAIN;
            case 71: case 73: case 75: case 77: case 85: case 86: return KIND_SNOW;
            case 95: case 96: case 99: return KIND_THUNDER;
            default: return KIND_CLOUDY;
        }
    }

    public static int owmToKind(String icon) {
        if (icon == null || icon.length() < 2) return KIND_CLOUDY;
        String p = icon.substring(0, 2);
        switch (p) {
            case "01": return KIND_CLEAR;
            case "02": return KIND_PARTLY;
            case "03": case "04": return KIND_CLOUDY;
            case "09": case "10": return KIND_RAIN;
            case "11": return KIND_THUNDER;
            case "13": return KIND_SNOW;
            case "50": return KIND_FOG;
            default: return KIND_CLOUDY;
        }
    }
}
