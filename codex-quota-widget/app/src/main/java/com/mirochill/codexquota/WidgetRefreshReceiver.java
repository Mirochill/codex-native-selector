package com.mirochill.codexquota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Private receiver reached only through the widget's immutable PendingIntent. */
public class WidgetRefreshReceiver extends BroadcastReceiver {
    public static final String ACTION_REFRESH = "com.mirochill.codexquota.REFRESH_WIDGET";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AutoSyncScheduler.requestImmediateSync(context);
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            CodexWidgetProvider.updateAll(context);
            AutoSyncScheduler.ensureScheduled(context);
            AutoSyncScheduler.requestSyncIfDue(context);
        }
    }
}
