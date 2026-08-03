package com.mirochill.codexquota;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

/** Lets Android batch quota refreshes instead of keeping a process or timer alive. */
public final class AutoSyncScheduler {
    static final int PERIODIC_JOB_ID = 0xC0D301;
    static final int IMMEDIATE_JOB_ID = 0xC0D302;
    static final long PERIOD_MS = 30L * 60L * 1000L;
    private static final long FLEX_MS = 10L * 60L * 1000L;

    private AutoSyncScheduler() {}

    public static void ensureScheduled(Context context) {
        Context app = context.getApplicationContext();
        if (!ChatGptAuthStore.hasTokens(app) || !CodexWidgetProvider.hasWidgets(app)) return;
        JobScheduler scheduler = app.getSystemService(JobScheduler.class);
        if (scheduler == null || scheduler.getPendingJob(PERIODIC_JOB_ID) != null) return;

        JobInfo job = base(app, PERIODIC_JOB_ID)
                .setPersisted(true)
                .setRequiresBatteryNotLow(true)
                .setPeriodic(PERIOD_MS, FLEX_MS)
                .build();
        scheduler.schedule(job);
    }

    public static void requestImmediateSync(Context context) {
        Context app = context.getApplicationContext();
        if (!ChatGptAuthStore.hasTokens(app) || !CodexWidgetProvider.hasWidgets(app)) return;
        JobScheduler scheduler = app.getSystemService(JobScheduler.class);
        if (scheduler == null) return;

        JobInfo.Builder builder = base(app, IMMEDIATE_JOB_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setExpedited(true);
        } else {
            builder.setMinimumLatency(0L);
        }
        JobInfo job = builder.build();
        if (scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS) {
            CodexWidgetProvider.showSyncing(app);
        }
    }

    public static void cancel(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null) return;
        scheduler.cancel(PERIODIC_JOB_ID);
        scheduler.cancel(IMMEDIATE_JOB_ID);
    }

    private static JobInfo.Builder base(Context context, int id) {
        return new JobInfo.Builder(id, new ComponentName(context, QuotaSyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
    }
}
