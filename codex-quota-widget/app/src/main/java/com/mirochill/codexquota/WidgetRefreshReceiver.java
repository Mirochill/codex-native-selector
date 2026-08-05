package com.mirochill.codexquota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Receives private sync alarms and system lifecycle events. */
public class WidgetRefreshReceiver extends BroadcastReceiver {
    public static final String ACTION_REFRESH = "com.mirochill.codexquota.REFRESH_WIDGET";
    public static final String ACTION_AUTO_SYNC_ALARM =
            "com.mirochill.codexquota.AUTO_SYNC_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (ACTION_AUTO_SYNC_ALARM.equals(action)) {
            AutoSyncScheduler.onAlarm(context);
        } else if (ACTION_REFRESH.equals(action)) {
            AutoSyncScheduler.requestImmediateSync(context);
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            CodexWidgetProvider.updateAll(context);
            AutoSyncScheduler.reschedule(context);
            AutoSyncScheduler.requestSyncIfDue(context);
        }
    }
}
