package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Ergo fork (theme key app_cards): a calm tile in place of an app's own banner. A colour that darkens towards
 * the bottom-right, the app's own icon and its name in white, and a thin neon edge. Banners clash (a white
 * YouTube slab, a noisy OTT picture); these keep the dock even. Drawn at banner proportions (16:9), so the
 * tile's centreCrop scales it like a real banner; sizes below are in those 320 x 180 units.
 */
final class TextBannerDrawable extends Drawable {

    static final int W = 320, H = 180;

    private final int color;
    private final int edgeColor;
    private final String text;
    private final Drawable icon;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int shaderW = -1, shaderH = -1;

    /**
     * @param edgeColor the neon edge (0 for none); the tile's corners are 16 dp, 28 units at this size.
     * @param icon      the app's own icon, drawn beside the name or above it when both don't fit; null for the name only.
     */
    TextBannerDrawable(int color, String text, Typeface typeface, Drawable icon, int edgeColor) {
        this.color = color | 0xFF000000;
        this.text = text;
        this.icon = icon;
        this.edgeColor = edgeColor;
        paint.setColor(0xFFFFFFFF);
        paint.setTextAlign(Paint.Align.LEFT);
        if (typeface != null) paint.setTypeface(typeface);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(3.5f);   // about 2 px on a 180 dp tile
        edge.setColor(edgeColor);
    }

    private static int shade(int c, float f) {
        return 0xFF000000 | Math.round(((c >> 16) & 0xFF) * f) << 16 | Math.round(((c >> 8) & 0xFF) * f) << 8
                | Math.round((c & 0xFF) * f);
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        if (b.width() != shaderW || b.height() != shaderH) {
            fill.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom, shade(color, 1.18f), shade(color, 0.55f),
                    Shader.TileMode.CLAMP));
            shaderW = b.width();
            shaderH = b.height();
        }
        c.drawRect(b, fill);

        float h = b.height(), w = b.width();
        float iconSize = icon == null ? 0 : h * 0.36f, gap = h * 0.07f;
        float textSize = h * 0.19f;
        paint.setTextSize(textSize);
        float textW = paint.measureText(text);
        boolean stacked = icon != null && iconSize + gap + textW > w * 0.84f;
        if (stacked) {
            textSize = h * 0.15f;
            paint.setTextSize(textSize);
            textW = paint.measureText(text);
        }
        if (textW > w * 0.84f) {
            paint.setTextSize(textSize * w * 0.84f / textW);
            textW = paint.measureText(text);
        }
        Paint.FontMetrics fm = paint.getFontMetrics();
        float textH = fm.descent - fm.ascent;
        if (stacked) {
            float top = b.exactCenterY() - (iconSize + gap * 0.6f + textH) / 2f;
            drawIcon(c, b.exactCenterX() - iconSize / 2f, top, iconSize);
            c.drawText(text, b.exactCenterX() - textW / 2f, top + iconSize + gap * 0.6f - fm.ascent, paint);
        } else {
            float total = icon == null ? textW : iconSize + gap + textW;
            float x = b.exactCenterX() - total / 2f;
            if (icon != null) {
                drawIcon(c, x, b.exactCenterY() - iconSize / 2f, iconSize);
                x += iconSize + gap;
            }
            c.drawText(text, x, b.exactCenterY() - (fm.ascent + fm.descent) / 2f, paint);
        }

        if (edgeColor != 0) {
            float inset = edge.getStrokeWidth() / 2f;
            rect.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
            c.drawRoundRect(rect, 28f, 28f, edge);
        }
    }

    private void drawIcon(Canvas c, float x, float y, float size) {
        icon.setBounds(Math.round(x), Math.round(y), Math.round(x + size), Math.round(y + size));
        icon.draw(c);
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
