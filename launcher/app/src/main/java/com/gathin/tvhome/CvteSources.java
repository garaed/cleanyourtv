package com.gathin.tvhome;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Ergo fork (theme key cvte_sources): inputs on CVTE boards whose sources live in CVTE's TV API, not in
 * TvInputManager (Ergo 32, HiSilicon Hi3751V350). The API is the firmware's shared library cn.cvte.tv.api,
 * reached by reflection so the launcher still builds and runs anywhere else. Switching does what the stock
 * Barnard launcher does: set the source through the API, then bring up CVTE's TV window (TvSettingActivity)
 * on that source, so the picture comes straight up with no source panel in between.
 */
final class CvteSources {

    private static final String TAG = "TvHome.CvteSources";

    static final class Source {
        final int id;
        final String label;
        /** HDMI, AV, YPBPR, ... (EnumInputSourceCategory without its INPUTSOURCE_ prefix). */
        final String category;

        Source(int id, String label, String category) {
            this.id = id;
            this.label = label;
            this.category = category;
        }
    }

    private static Object api;          // ITvApiManager
    private static Object sourceApi;    // ITVApiSystemInputSourceAidl
    private static volatile List<Source> cached = Collections.emptyList();
    private static boolean connecting;

    private CvteSources() {
    }

    /** True where the firmware has CVTE's TV API library. */
    static boolean available() {
        try {
            Class.forName("com.cvte.tv.api.TvApiSDKManager");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Connects once; the source list is read on connection. Safe to call again. */
    static void connect(Context ctx) {
        if (api != null || connecting || !available()) return;
        connecting = true;
        try {
            Application app = (Application) ctx.getApplicationContext();
            Class<?> mgr = Class.forName("com.cvte.tv.api.TvApiSDKManager");
            mgr.getMethod("init", Application.class).invoke(null, app);
            Object instance = mgr.getMethod("getInstance").invoke(null);
            Class<?> listener = Class.forName("com.cvte.tv.api.TvServiceConnectListener");
            Object callback = Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[]{listener},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "OnConnected":
                                if (args != null && args.length > 0) onConnected(args[0]);
                                return null;
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return args != null && proxy == args[0];
                            case "toString":
                                return "CvteSources.listener";
                            default:
                                return null;
                        }
                    });
            mgr.getMethod("getTvApi", listener).invoke(instance, callback);
        } catch (Throwable t) {
            Log.w(TAG, "TV API not reachable: " + t);
            connecting = false;
        }
    }

    private static void onConnected(Object manager) {
        connecting = false;
        try {
            api = manager;
            Class<?> mgr = Class.forName("com.cvte.tv.api.aidl.ITvApiManager");
            sourceApi = mgr.getMethod("getTVApiSystemInputSource").invoke(manager);
            new Thread(CvteSources::readList, "cvte-sources").start();
        } catch (Throwable t) {
            Log.w(TAG, "no input source API: " + t);
        }
    }

    private static void readList() {
        try {
            Class<?> src = Class.forName("com.cvte.tv.api.aidl.ITVApiSystemInputSourceAidl");
            Object list = src.getMethod("eventSystemInputSourceGetList").invoke(sourceApi);
            if (!(list instanceof List)) return;
            Class<?> entity = Class.forName("com.cvte.tv.api.aidl.EntityInputSource");
            List<Source> out = new ArrayList<>();
            for (Object e : (List<?>) list) {
                int id = entity.getField("id").getInt(e);
                Object n = entity.getField("name").get(e);
                Object c = entity.getField("category").get(e);
                String category = c == null ? "" : c.toString().replace("INPUTSOURCE_", "");
                if (!shown(category)) continue;
                out.add(new Source(id, label(n == null ? category : n.toString(), category), category));
            }
            Collections.sort(out, (a, b) -> rank(a.category) != rank(b.category)
                    ? Integer.compare(rank(a.category), rank(b.category)) : a.label.compareToIgnoreCase(b.label));
            cached = Collections.unmodifiableList(out);
        } catch (Throwable t) {
            Log.w(TAG, "source list unreadable: " + t);
        }
    }

    /** The external inputs; the tuners and APP (Android itself) are left out, as Barnard's bar does. */
    private static boolean shown(String category) {
        return rank(category) < 9;
    }

    private static int rank(String category) {
        switch (category) {
            case "HDMI": return 0;
            case "AV": return 1;
            case "YPBPR": return 2;
            case "PC": return 3;
            case "SCART": return 4;
            case "SVIDEO": return 5;
            default: return 9;
        }
    }

    /** "HDMI1" -> "HDMI 1", "YPBPR" -> "YPbPr". */
    static String label(String name, String category) {
        String s = name.trim();
        if (s.isEmpty()) s = category;
        s = s.replaceAll("(?i)^(HDMI|AV|PC)\\s*(\\d)$", "$1 $2").toUpperCase(Locale.ROOT);
        return s.replace("YPBPR", "YPbPr");
    }

    /** The external inputs, or empty before the API has answered or where there is none. */
    static List<Source> list() {
        return cached;
    }

    /**
     * Switches to the source on a worker thread, then runs {@code after} on the main thread to bring up the TV
     * window; false when the API is not there. The caller starts the window from its own activity: started from the
     * application context it never came up on the Ergo 32, while the same intent from Home or the shell did.
     */
    static boolean open(Source s, Runnable after) {
        if (sourceApi == null) return false;
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                Class<?> src = Class.forName("com.cvte.tv.api.aidl.ITVApiSystemInputSourceAidl");
                src.getMethod("eventSystemInputSourceSetInputSource", int.class).invoke(sourceApi, s.id);
                Log.i(TAG, "switched to " + s.id + " (" + s.label + ")");
            } catch (Throwable t) {
                Log.w(TAG, "switch to " + s.id + " failed: " + t);
            }
            main.post(after);
        }, "cvte-switch").start();
        return true;
    }

    /** CVTE's TV window on the current source, as Barnard's "go to TV": TvSettingActivity as TV_HOME. */
    static Intent tvWindow() {
        return new Intent(Intent.ACTION_MAIN)
                .addCategory("android.intent.category.TV_HOME")
                .setClassName("com.cvte.tv.setting", "com.cvte.tv.setting.TvSettingActivity")
                .putExtra("isLauncherGoToTv", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
