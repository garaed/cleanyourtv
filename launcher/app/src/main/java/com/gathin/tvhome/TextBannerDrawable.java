package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Ergo fork (theme key app_cards): a calm tile in place of an app's own banner, a flat colour with the app's
 * name in white, centred. Banners clash (a white YouTube slab, a noisy OTT picture); these keep the dock even.
 * Drawn at banner proportions (16:9), so the tile's centreCrop scales it like a real banner.
 */
final class TextBannerDrawable extends Drawable {

    static final int W = 320, H = 180;

    private final int color;
    private final String text;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    TextBannerDrawable(int color, String text, Typeface typeface) {
        this.color = color | 0xFF000000;
        this.text = text;
        paint.setColor(0xFFFFFFFF);
        paint.setTextAlign(Paint.Align.CENTER);
        if (typeface != null) paint.setTypeface(typeface);
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        c.drawColor(color);
        // The name fills at most 80% of the width, at a size that reads at 3 m on a 32" set.
        float size = b.height() * 0.2f;
        paint.setTextSize(size);
        float w = paint.measureText(text);
        if (w > b.width() * 0.8f) paint.setTextSize(size * b.width() * 0.8f / w);
        Paint.FontMetrics fm = paint.getFontMetrics();
        c.drawText(text, b.exactCenterX(), b.exactCenterY() - (fm.ascent + fm.descent) / 2f, paint);
    }

    @Override
    public int getIntrinsicWidth() {
        return W;
    }

    @Override
    public int getIntrinsicHeight() {
        return H;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
