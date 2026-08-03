package com.mirochill.codexquota;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Bundle;

/** Draws only the premium visual layer; Android renders all text natively above it. */
public final class WidgetRenderer {
    private static final float DESIGN_W = 360f;
    private static final float DESIGN_H = 88f;
    private static final int GREEN = Color.rgb(73, 234, 157);
    private static final int VIOLET = Color.rgb(132, 145, 255);

    private WidgetRenderer() {}

    public static Bitmap render(Context context, QuotaSnapshot snapshot, Bundle options) {
        int widthDp = options == null ? 360 : options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 360);
        int heightDp = options == null ? 88 : options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 88);
        widthDp = clamp(widthDp, 280, 460);
        heightDp = clamp(heightDp, 70, 120);
        float renderScale = Math.min(2f, context.getResources().getDisplayMetrics().density);
        int width = Math.max(560, Math.round(widthDp * renderScale));
        int height = Math.max(140, Math.round(heightDp * renderScale));
        if ((long) width * height > 180_000L) {
            float shrink = (float) Math.sqrt(180_000d / ((double) width * height));
            width = Math.round(width * shrink);
            height = Math.round(height * shrink);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(width / DESIGN_W, height / DESIGN_H);
        drawBackground(canvas);
        if (snapshot.hasSecondaryWindow()) {
            drawQuotaCard(canvas, new RectF(9f, 8f, 112f, 63f), snapshot.primaryValue, true, false);
            drawQuotaCard(canvas, new RectF(117f, 8f, 220f, 63f), snapshot.secondaryValue, false, false);
        } else {
            drawQuotaCard(canvas, new RectF(9f, 8f, 220f, 63f), snapshot.primaryValue, true, true);
        }
        drawTokenStrip(canvas);
        drawAccountCard(canvas, snapshot.updatedAt != 0L);
        return bitmap;
    }

    private static void drawBackground(Canvas canvas) {
        RectF bounds = new RectF(1f, 1f, 359f, 87f);
        Path clip = new Path();
        clip.addRoundRect(bounds, 20f, 20f, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0f, 0f, 360f, 88f,
                new int[]{Color.rgb(13, 24, 24), Color.rgb(11, 17, 24), Color.rgb(8, 12, 18)},
                new float[]{0f, .52f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, 360f, 88f, paint);
        paint.setShader(new RadialGradient(40f, 20f, 92f,
                new int[]{Color.argb(58, 36, 202, 124), Color.TRANSPARENT}, null,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(40f, 20f, 92f, paint);
        paint.setShader(new RadialGradient(175f, 12f, 86f,
                new int[]{Color.argb(45, 94, 107, 255), Color.TRANSPARENT}, null,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(175f, 12f, 86f, paint);
        paint.setShader(null);
        paint.setColor(Color.argb(20, 255, 255, 255));
        canvas.drawRect(18f, 1.5f, 342f, 2f, paint);
        canvas.restore();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(.75f);
        paint.setColor(Color.rgb(48, 68, 67));
        canvas.drawRoundRect(bounds, 20f, 20f, paint);
    }

    private static void drawQuotaCard(Canvas canvas, RectF card, String value,
                                      boolean primary, boolean single) {
        int percent = QuotaSnapshot.percentFromValue(value);
        int accent = quotaColor(percent, primary ? GREEN : VIOLET);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(154, 17, 28, 34));
        canvas.drawRoundRect(card, 14f, 14f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(.7f);
        paint.setColor(withAlpha(accent, 67));
        canvas.drawRoundRect(card, 14f, 14f, paint);

        float cx = single ? card.left + 48f : card.left + 29f;
        float cy = card.top + 27.5f;
        float radius = single ? 21.5f : 18.5f;
        RectF ring = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(single ? 5.2f : 4.7f);
        paint.setColor(Color.rgb(34, 48, 55));
        canvas.drawArc(ring, -90f, 360f, false, paint);
        if (percent < 0) return;

        paint.setStrokeWidth(single ? 7.8f : 7.2f);
        paint.setColor(withAlpha(accent, 52));
        paint.setMaskFilter(new BlurMaskFilter(5.5f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawArc(ring, -90f, Math.min(359.8f, 360f * percent / 100f), false, paint);
        paint.setMaskFilter(null);
        paint.setStrokeWidth(single ? 5.2f : 4.7f);
        SweepGradient gradient = new SweepGradient(cx, cy,
                new int[]{darken(accent, .72f), accent, lighten(accent, .30f)},
                new float[]{0f, .72f, 1f});
        Matrix matrix = new Matrix();
        matrix.setRotate(-90f, cx, cy);
        gradient.setLocalMatrix(matrix);
        paint.setShader(gradient);
        canvas.drawArc(ring, -90f, Math.min(359.8f, 360f * percent / 100f), false, paint);
        paint.setShader(null);
    }

    private static void drawTokenStrip(Canvas canvas) {
        RectF strip = new RectF(9f, 67f, 220f, 80.5f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(162, 17, 28, 35));
        canvas.drawRoundRect(strip, 6.75f, 6.75f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(.6f);
        paint.setColor(Color.rgb(42, 59, 68));
        canvas.drawRoundRect(strip, 6.75f, 6.75f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(48, 65, 73));
        canvas.drawRect(113.8f, 70f, 114.3f, 77.5f, paint);
    }

    private static void drawAccountCard(Canvas canvas, boolean synced) {
        RectF card = new RectF(226f, 8f, 351f, 80.5f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(174, 10, 16, 23));
        canvas.drawRoundRect(card, 15f, 15f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(.7f);
        paint.setColor(Color.rgb(38, 49, 63));
        canvas.drawRoundRect(card, 15f, 15f, paint);
        chip(canvas, new RectF(234f, 14f, 299f, 32f), GREEN, Color.argb(86, 33, 108, 76));
        chip(canvas, new RectF(304f, 14f, 343f, 32f), VIOLET, Color.argb(86, 57, 65, 128));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(synced ? GREEN : Color.rgb(104, 116, 123));
        canvas.drawCircle(237f, 71.3f, 1.7f, paint);
    }

    private static void chip(Canvas canvas, RectF bounds, int accent, int fill) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(fill);
        canvas.drawRoundRect(bounds, 9f, 9f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(.7f);
        paint.setColor(withAlpha(accent, 120));
        canvas.drawRoundRect(bounds, 9f, 9f, paint);
    }

    private static int quotaColor(int percent, int normal) {
        if (percent >= 0 && percent <= 15) return Color.rgb(255, 101, 112);
        if (percent >= 0 && percent <= 35) return Color.rgb(255, 186, 87);
        return normal;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int darken(int color, float factor) {
        return Color.rgb(Math.round(Color.red(color) * factor),
                Math.round(Color.green(color) * factor), Math.round(Color.blue(color) * factor));
    }

    private static int lighten(int color, float factor) {
        return Color.rgb(
                Math.min(255, Math.round(Color.red(color) + (255 - Color.red(color)) * factor)),
                Math.min(255, Math.round(Color.green(color) + (255 - Color.green(color)) * factor)),
                Math.min(255, Math.round(Color.blue(color) + (255 - Color.blue(color)) * factor)));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
