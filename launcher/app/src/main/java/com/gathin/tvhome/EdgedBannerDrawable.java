package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Ergo fork: a bundled banner (app_banners) with a thin neon edge round it, as the app_cards tiles have, so
 * every tile in the dock carries the same outline. Keeps the banner's own size and proportions.
 */
final class EdgedBannerDrawable extends Drawable {

    private final Drawable art;
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    EdgedBannerDrawable(Drawable art, int edgeColor) {
        this.art = art;
        edge.setStyle(Paint.Style.STROKE);
        edge.setColor(edgeColor);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        art.setBounds(bounds);
    }

    @Override
    public void draw(Canvas c) {
        art.draw(c);
        Rect b = getBounds();
        // About 2 px and the tile's 16 dp corners once centreCrop has scaled the banner to the tile.
        float scale = b.height() / 101f;
        edge.setStrokeWidth(2f * scale);
        float inset = edge.getStrokeWidth() / 2f;
        rect.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
        c.drawRoundRect(rect, 16f * scale, 16f * scale, edge);
    }

    @Override
    public int getIntrinsicWidth() {
        return art.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return art.getIntrinsicHeight();
    }

    @Override
    public void setAlpha(int alpha) {
        art.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        art.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
