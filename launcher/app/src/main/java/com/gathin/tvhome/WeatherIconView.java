package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Ergo fork: the weather panel's icon, drawn as one neon tube (sun, cloud, rain, snow, fog or storm) with a soft
 * glow, so it needs no image files and takes the theme's neon colour. Drawn once per weather update.
 */
public class WeatherIconView extends View {

    private final Paint tube = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final float density;
    private int code = 3;

    public WeatherIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        for (Paint p : new Paint[]{tube, core}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
        tube.setStrokeWidth(3f * density);
        core.setStrokeWidth(1.2f * density);
        core.setColor(0xFFFFE6FF);
        setColor(0xFFF050F8);
    }

    void setColor(int argb) {
        tube.setColor(argb | 0xFF000000);
        // The glow: a shadow in the tube's colour (drawn by the GPU on Android 9 and later; plain lines before).
        tube.setShadowLayer(8f * density, 0, 0, argb | 0xFF000000);
        invalidate();
    }

    void setCode(int wmo) {
        code = wmo;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float u = Math.min(w, h) / 100f;   // a 100 x 100 design box
        float ox = (w - 100 * u) / 2f, oy = (h - 100 * u) / 2f;
        c.save();
        c.translate(ox, oy);
        if (code == 0) {
            sun(c, u, 50, 50, 18);
        } else if (code <= 2) {
            sun(c, u, 36, 36, 13);
            cloud(c, u, 8);
        } else {
            cloud(c, u, 0);
            if (code >= 51 && code <= 67 || code >= 80 && code <= 82) {
                for (int i = 0; i < 3; i++) line(c, u, 34 + i * 16, 74, 30 + i * 16, 88);
            } else if (code >= 71 && code <= 77 || code == 85 || code == 86) {
                for (int i = 0; i < 3; i++) dot(c, u, 34 + i * 16, 82);
            } else if (code == 45 || code == 48) {
                line(c, u, 24, 78, 76, 78);
                line(c, u, 32, 88, 68, 88);
            } else if (code >= 95) {
                path.reset();
                path.moveTo(54 * u, 68 * u);
                path.lineTo(44 * u, 82 * u);
                path.lineTo(56 * u, 82 * u);
                path.lineTo(46 * u, 96 * u);
                stroke(c, path);
            }
        }
        c.restore();
    }

    private void sun(Canvas c, float u, float cx, float cy, float r) {
        path.reset();
        path.addCircle(cx * u, cy * u, r * u, Path.Direction.CW);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float x1 = (float) (cx + Math.cos(a) * (r + 6)), y1 = (float) (cy + Math.sin(a) * (r + 6));
            float x2 = (float) (cx + Math.cos(a) * (r + 12)), y2 = (float) (cy + Math.sin(a) * (r + 12));
            path.moveTo(x1 * u, y1 * u);
            path.lineTo(x2 * u, y2 * u);
        }
        stroke(c, path);
    }

    /** A cloud outline: two arcs on top of a flat base, shifted down by dy. */
    private void cloud(Canvas c, float u, float dy) {
        path.reset();
        path.moveTo(26 * u, (66 + dy) * u);
        oval.set(14 * u, (42 + dy) * u, 40 * u, (68 + dy) * u);
        path.arcTo(oval, 90, 180);
        oval.set(30 * u, (24 + dy) * u, 70 * u, (64 + dy) * u);
        path.arcTo(oval, 200, 160);
        oval.set(60 * u, (38 + dy) * u, 88 * u, (66 + dy) * u);
        path.arcTo(oval, 250, 200);
        path.lineTo(26 * u, (66 + dy) * u);
        stroke(c, path);
    }

    private void line(Canvas c, float u, float x1, float y1, float x2, float y2) {
        path.reset();
        path.moveTo(x1 * u, y1 * u);
        path.lineTo(x2 * u, y2 * u);
        stroke(c, path);
    }

    private void dot(Canvas c, float u, float x, float y) {
        path.reset();
        path.addCircle(x * u, y * u, 1.5f * u, Path.Direction.CW);
        stroke(c, path);
    }

    private void stroke(Canvas c, Path p) {
        c.drawPath(p, tube);
        c.drawPath(p, core);
    }
}
