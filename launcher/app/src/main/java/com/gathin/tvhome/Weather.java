package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Ergo fork: the weather for one place from Open-Meteo (no key), for the panel under the top-right buttons
 * (theme key weather_panel). One small request at most every 30 minutes and only while Home is on screen; the
 * last answer is kept, so Home shows it straight away after a restart. Upstream keeps the launcher off the
 * network on purpose; this lives only in the fork.
 */
final class Weather {

    private static final String TAG = "TvHome.Weather";
    private static final String PREFS = "weather";
    private static final long REFRESH_MS = 30 * 60 * 1000L;
    /** After a failed request, try again sooner than the full interval. */
    private static final long RETRY_MS = 5 * 60 * 1000L;

    /** What the panel shows. Temperatures in °C, rounded. */
    static final class Report {
        final int temperature;
        final int feelsLike;
        final int code;
        final String[] days = new String[3];
        final int[] dayMax = new int[3];
        final int[] dayCode = new int[3];

        Report(int temperature, int feelsLike, int code) {
            this.temperature = temperature;
            this.feelsLike = feelsLike;
            this.code = code;
        }
    }

    interface Listener {
        void onWeather(Report report);
    }

    private final Context app;
    private final double lat, lon;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private long nextFetchAt;
    private boolean fetching;

    Weather(Context ctx, double lat, double lon, Listener listener) {
        this.app = ctx.getApplicationContext();
        this.lat = lat;
        this.lon = lon;
        this.listener = listener;
    }

    /** The last answer, from the previous run; null when there is none yet. */
    Report cached() {
        String text = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key(), null);
        return text == null ? null : parse(text);
    }

    /** Fetches when the interval is up. Call it on resume and once a minute while Home is visible. */
    void refreshIfDue() {
        long now = System.currentTimeMillis();
        if (fetching || now < nextFetchAt) return;
        fetching = true;
        Thread t = new Thread(this::fetch, "weather");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private String key() {
        return String.format(Locale.US, "json_%.2f_%.2f", lat, lon);
    }

    private void fetch() {
        String url = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                + "&current=temperature_2m,apparent_temperature,weather_code"
                + "&daily=temperature_2m_max,weather_code&timezone=auto&forecast_days=4", lat, lon);
        String text = null;
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(10_000);
            c.setReadTimeout(10_000);
            if (c.getResponseCode() == 200) {
                try (InputStream in = c.getInputStream()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) > 0 && out.size() < 64 * 1024) out.write(buf, 0, n);
                    text = new String(out.toByteArray(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "fetch failed: " + e.getMessage());
        } finally {
            if (c != null) c.disconnect();
        }
        Report report = text == null ? null : parse(text);
        if (report != null) {
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key(), text).apply();
        }
        final Report r = report;
        main.post(() -> {
            fetching = false;
            nextFetchAt = System.currentTimeMillis() + (r != null ? REFRESH_MS : RETRY_MS);
            if (r != null) listener.onWeather(r);
        });
    }

    static Report parse(String text) {
        try {
            JSONObject root = new JSONObject(text);
            JSONObject cur = root.getJSONObject("current");
            Report r = new Report((int) Math.round(cur.getDouble("temperature_2m")),
                    (int) Math.round(cur.getDouble("apparent_temperature")), cur.getInt("weather_code"));
            JSONObject daily = root.getJSONObject("daily");
            JSONArray time = daily.getJSONArray("time");
            JSONArray max = daily.getJSONArray("temperature_2m_max");
            JSONArray codes = daily.getJSONArray("weather_code");
            // Index 0 is today; the panel lists the next three days.
            for (int i = 0; i < 3 && i + 1 < time.length(); i++) {
                LocalDate day = LocalDate.parse(time.getString(i + 1));
                r.days[i] = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        .toUpperCase(Locale.getDefault());
                r.dayMax[i] = (int) Math.round(max.getDouble(i + 1));
                r.dayCode[i] = codes.getInt(i + 1);
            }
            return r;
        } catch (Exception e) {
            Log.w(TAG, "unreadable answer: " + e.getMessage());
            return null;
        }
    }

    /** +14°, -3°, 0°. */
    static String degrees(int t) {
        return (t > 0 ? "+" : "") + t + "°";
    }

    /** WMO weather code, as Open-Meteo reports it, to a short English condition. */
    static String condition(int code) {
        if (code == 0) return "Clear";
        if (code <= 2) return "Partly cloudy";
        if (code == 3) return "Cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67 || code >= 80 && code <= 82) return "Rain";
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "Snow";
        if (code >= 95) return "Thunderstorm";
        return "Cloudy";
    }
}
