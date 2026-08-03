package com.mirochill.codexquota;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
        views.setTextViewText(R.id.widget_primary_label, snapshot.primaryLabel);
        views.setTextViewText(R.id.widget_primary, snapshot.primaryValue);
        views.setTextViewText(R.id.widget_secondary_label, snapshot.secondaryLabel);
        views.setTextViewText(R.id.widget_secondary, snapshot.secondaryValue);
        views.setTextViewText(R.id.widget_updated, snapshot.updatedAt == 0L ? "À synchroniser" : MainActivity.time(snapshot.updatedAt));
        setProgress(views, R.id.widget_primary_progress, snapshot.primaryValue);
        setProgress(views, R.id.widget_secondary_progress, snapshot.secondaryValue);

        // Touching the widget opens the lightweight sync screen, so one tap refreshes it.
        Intent open = new Intent(context, SyncActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                context, 1001, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);
        manager.updateAppWidget(id, views);
    }

    private static void setProgress(RemoteViews views, int id, String value) {
        int percent = QuotaSnapshot.percentFromValue(value);
        if (percent < 0) {
            views.setViewVisibility(id, android.view.View.GONE);
        } else {
            views.setViewVisibility(id, android.view.View.VISIBLE);
            views.setProgressBar(id, 100, percent, false);
        }
    }
}
