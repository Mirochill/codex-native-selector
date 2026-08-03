package com.mirochill.codexquota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Private receiver reached only through the widget's immutable PendingIntent. */
public class WidgetRefreshReceiver extends BroadcastReceiver {
    public static final String ACTION_REFRESH = "com.mirochill.codexquota.REFRESH_WIDGET";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_REFRESH.equals(intent.getAction())) {
            AutoSyncScheduler.requestImmediateSync(context);
        }
    }
}
