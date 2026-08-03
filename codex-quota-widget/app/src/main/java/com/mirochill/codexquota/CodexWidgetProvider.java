package com.mirochill.codexquota;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

public class CodexWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) updateWidget(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, CodexWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) updateWidget(context, manager, id);
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int id) {
        QuotaSnapshot snapshot = QuotaStore.get(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_codex);
        views.setImageViewBitmap(R.id.widget_canvas,
                WidgetRenderer.render(context, snapshot, manager.getAppWidgetOptions(id)));

        boolean single = !snapshot.hasSecondaryWindow();
        views.setViewVisibility(R.id.widget_dual_quotas, single ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.widget_single_quota, single ? View.VISIBLE : View.GONE);

        views.setTextViewText(R.id.widget_primary, snapshot.primaryValue);
        views.setTextViewText(R.id.widget_primary_label, displayWindow(snapshot.primaryLabel));
        views.setTextViewText(R.id.widget_secondary, snapshot.secondaryValue);
        views.setTextViewText(R.id.widget_secondary_label, displayWindow(snapshot.secondaryLabel));
        views.setTextViewText(R.id.widget_single_value, snapshot.primaryValue);
        views.setTextViewText(R.id.widget_single_label, displayWindow(snapshot.primaryLabel));
        views.setTextViewText(R.id.widget_tokens_day, QuotaSnapshot.compactTokens(snapshot.dailyTokens));
        views.setTextViewText(R.id.widget_tokens_total, QuotaSnapshot.compactTokens(snapshot.lifetimeTokens));
        views.setTextViewText(R.id.widget_plan, snapshot.planType);
        views.setTextViewText(R.id.widget_reset_bank,
                "BANK " + (snapshot.resetCredits < 0 ? "—" : snapshot.resetCredits));
        views.setTextViewText(R.id.widget_next_reset, resetValue(snapshot.nextResetAt()));
        views.setTextViewText(R.id.widget_updated, snapshot.updatedAt == 0L
                ? "SYNC —" : "SYNC  " + MainActivity.time(snapshot.updatedAt));

        Intent open = new Intent(context, SyncActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                context, 1001, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);
        manager.updateAppWidget(id, views);
    }

    private static String displayWindow(String label) {
        if (label == null || label.trim().isEmpty()) return "QUOTA";
        String clean = label.trim().toUpperCase();
        if (clean.endsWith(" H")) return clean.substring(0, clean.length() - 2) + " HEURES";
        if (clean.endsWith(" J")) return clean.substring(0, clean.length() - 2) + " JOURS";
        return clean;
    }

    private static String resetValue(long resetsAt) {
        if (resetsAt <= 0L) return "—";
        long seconds = Math.max(0L, (resetsAt - System.currentTimeMillis()) / 1000L);
        if (seconds == 0L) return "MAINT.";
        long days = seconds / 86_400L;
        long hours = (seconds % 86_400L) / 3_600L;
        long minutes = (seconds % 3_600L) / 60L;
        if (days > 0L) return String.format(java.util.Locale.FRANCE, "%dJ %02dH", days, hours);
        if (hours > 0L) return String.format(java.util.Locale.FRANCE, "%dH %02dM", hours, minutes);
        return String.format(java.util.Locale.FRANCE, "%d MIN", Math.max(1L, minutes));
    }
}
