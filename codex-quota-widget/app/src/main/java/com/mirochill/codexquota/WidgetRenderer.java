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
import android.graphics.Typeface;
import android.os.Bundle;

import java.util.Locale;

/** Pixel-perfect, battery-cheap renderer for the 4×1 widget. */
public final class WidgetRenderer {
    private static final float DESIGN_W = 360f;
    private static final float DESIGN_H = 88f;
    private static final int GREEN = Color.rgb(73, 234, 157);
    private static final int VIOLET = Color.rgb(132, 145, 255);
    private static final int TEXT = Color.rgb(245, 249, 250);
    private static final int MUTED = Color.rgb(144, 160, 170);
    private static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

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
        drawQuotaCard(canvas, new RectF(9f, 8f, 112f, 63f), "5 HEURES", "COURT TERME",
                snapshot.primaryValue, true);
        drawQuotaCard(canvas, new RectF(117f, 8f, 220f, 63f), "7 JOURS", "HEBDOMADAIRE",
                snapshot.secondaryValue, false);
        drawTokenStrip(canvas, snapshot);
        drawAccountCard(canvas, snapshot);
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
                new int[]{Color.argb(58, 36, 202, 124), Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(40f, 20f, 92f, paint);
        paint.setShader(new RadialGradient(175f, 12f, 86f,
                new int[]{Color.argb(45, 94, 107, 255), Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP));
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

    private static void drawQuotaCard(Canvas canvas, RectF card, String title,
                                      String subtitle, String value, boolean primary) {
        int percent = QuotaSnapshot.percentFromValue(value);
        int accent = quotaColor(percent, primary ? GREEN : VIOLET);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(154, 17, 28, 34));
        canvas.drawRoundRect(card, 14f, 14f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(.7f);
        paint.setColor(withAlpha(accent, 67));
        canvas.drawRoundRect(card, 14f, 14f, paint);

        float cx = card.left + 29f;
        float cy = card.top + 27.5f;
        float radius = 18.5f;
        RectF ring = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(4.7f);
        paint.setColor(Color.rgb(34, 48, 55));
        canvas.drawArc(ring, -90f, 360f, false, paint);

        if (percent >= 0) {
            paint.setStrokeWidth(7.2f);
            paint.setColor(withAlpha(accent, 52));
            paint.setMaskFilter(new BlurMaskFilter(5.5f, BlurMaskFilter.Blur.NORMAL));
            canvas.drawArc(ring, -90f, Math.min(359.8f, 360f * percent / 100f), false, paint);
            paint.setMaskFilter(null);
            paint.setStrokeWidth(4.7f);
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

        drawCentered(canvas, value == null ? "—" : value, cx, cy + .5f,
                12.5f, TEXT, MEDIUM);
        drawText(canvas, title, card.left + 56f, card.top + 18f,
                8.2f, TEXT, MEDIUM);
        drawText(canvas, subtitle, card.left + 56f, card.top + 31.5f,
                5.8f, MUTED, MEDIUM);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accent);
        canvas.drawCircle(card.left + 57.5f, card.top + 43.5f, 1.6f, paint);
        drawText(canvas, percent < 0 ? "EN ATTENTE" : "DISPONIBLE", card.left + 62f,
                card.top + 46f, 5.8f, Color.rgb(176, 191, 197), MEDIUM);
    }

    private static void drawTokenStrip(Canvas canvas, QuotaSnapshot snapshot) {
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

        metric(canvas, "TOK/DAY", QuotaSnapshot.compactTokens(snapshot.dailyTokens), 18f, 73.8f);
        metric(canvas, "TOK/TOTAL", QuotaSnapshot.compactTokens(snapshot.lifetimeTokens), 124f, 73.8f);
    }

    private static void metric(Canvas canvas, String label, String value, float x, float y) {
        drawText(canvas, label, x, y, 5.8f, MUTED, MEDIUM);
        drawText(canvas, value, x + 39f, y + 1.3f, 8.6f, TEXT, MEDIUM);
    }

    private static void drawAccountCard(Canvas canvas, QuotaSnapshot snapshot) {
        RectF card = new RectF(226f, 8f, 351f, 80.5f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(174, 10, 16, 23));
        canvas.drawRoundRect(card, 15f, 15f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(.7f);
        paint.setColor(Color.rgb(38, 49, 63));
        canvas.drawRoundRect(card, 15f, 15f, paint);

        drawChip(canvas, new RectF(234f, 14f, 299f, 32f), snapshot.planType,
                GREEN, Color.argb(86, 33, 108, 76));
        drawChip(canvas, new RectF(304f, 14f, 343f, 32f),
                "BANK " + (snapshot.resetCredits < 0 ? "—" : snapshot.resetCredits),
                VIOLET, Color.argb(86, 57, 65, 128));

        drawText(canvas, "NEXT RESET", 235f, 44f, 6.2f, MUTED, MEDIUM);
        drawText(canvas, resetValue(snapshot.nextResetAt()), 235f, 60.5f,
                15.2f, TEXT, MEDIUM);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(snapshot.updatedAt == 0L ? Color.rgb(104, 116, 123) : GREEN);
        canvas.drawCircle(237f, 71.3f, 1.7f, paint);
        String sync = snapshot.updatedAt == 0L ? "SYNC —" : "SYNC  " + MainActivity.time(snapshot.updatedAt);
        drawText(canvas, sync, 242f, 73.6f, 6.7f, Color.rgb(161, 175, 182), MEDIUM);
    }

    private static void drawChip(Canvas canvas, RectF bounds, String text,
                                 int accent, int fill) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawRoundRect(bounds, 9f, 9f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(.7f);
        paint.setColor(withAlpha(accent, 120));
        canvas.drawRoundRect(bounds, 9f, 9f, paint);
        float textSize = text != null && text.length() > 8 ? 6.2f : 7.1f;
        drawCentered(canvas, text == null ? "CODEX" : text, bounds.centerX(), bounds.centerY() + .2f,
                textSize, lighten(accent, .38f), MEDIUM);
    }

    private static String resetValue(long resetsAt) {
        if (resetsAt <= 0L) return "—";
        long seconds = Math.max(0L, (resetsAt - System.currentTimeMillis()) / 1000L);
        if (seconds == 0L) return "MAINT.";
        long days = seconds / 86_400L;
        long hours = (seconds % 86_400L) / 3_600L;
        long minutes = (seconds % 3_600L) / 60L;
        if (days > 0L) return String.format(Locale.FRANCE, "%dJ %02dH", days, hours);
        if (hours > 0L) return String.format(Locale.FRANCE, "%dH %02dM", hours, minutes);
        return String.format(Locale.FRANCE, "%d MIN", Math.max(1L, minutes));
    }

    private static void drawText(Canvas canvas, String text, float x, float baseline,
                                 float size, int color, Typeface typeface) {
        Paint paint = textPaint(size, color, typeface);
        canvas.drawText(text == null ? "" : text, x, baseline, paint);
    }

    private static void drawCentered(Canvas canvas, String text, float cx, float cy,
                                     float size, int color, Typeface typeface) {
        String safeText = text == null ? "" : text;
        Paint paint = textPaint(size, color, typeface);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = cy - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(safeText, cx - paint.measureText(safeText) / 2f, baseline, paint);
    }

    private static Paint textPaint(float size, int color, Typeface typeface) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(typeface == null ? REGULAR : typeface);
        return paint;
    }

    private static int quotaColor(int percent, int normal) {
        if (percent >= 0 && percent <= 15) return Color.rgb(255, 101, 112);
        if (percent <= 35 && percent >= 0) return Color.rgb(255, 186, 87);
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
