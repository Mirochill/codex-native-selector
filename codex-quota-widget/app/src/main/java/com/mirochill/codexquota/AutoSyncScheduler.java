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
    private AutoSyncScheduler() {}

    public static void ensureScheduled(Context context) {
        try {
            Context app = context.getApplicationContext();
            JobScheduler scheduler = app.getSystemService(JobScheduler.class);
            if (scheduler == null) return;

            long period = SyncPreferences.intervalMillis(app);
            if (period == SyncPreferences.DISABLED) {
                scheduler.cancel(PERIODIC_JOB_ID);
                return;
            }
            if (!ChatGptAuthStore.hasTokens(app) || !CodexWidgetProvider.hasWidgets(app)) return;

            JobInfo pending = scheduler.getPendingJob(PERIODIC_JOB_ID);
            if (pending != null && pending.getIntervalMillis() == period) return;
            if (pending != null) scheduler.cancel(PERIODIC_JOB_ID);

            long flex = Math.max(5L * 60L * 1000L,
                    Math.min(10L * 60L * 1000L, period / 3L));
            JobInfo job = base(app, PERIODIC_JOB_ID)
                    .setPersisted(true)
                    .setRequiresBatteryNotLow(true)
                    .setPeriodic(period, flex)
                    .build();
            scheduler.schedule(job);
        } catch (RuntimeException ignored) {
            // Scheduling must never be able to crash the app or the widget host.
        }
    }

    public static void reschedule(Context context) {
        try {
            JobScheduler scheduler = context.getSystemService(JobScheduler.class);
            if (scheduler != null) scheduler.cancel(PERIODIC_JOB_ID);
        } catch (RuntimeException ignored) {
            // ensureScheduled below performs the same best-effort recovery.
        }
        ensureScheduled(context);
    }

    public static void requestImmediateSync(Context context) {
        try {
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
            if (scheduler.schedule(builder.build()) == JobScheduler.RESULT_SUCCESS) {
                CodexWidgetProvider.showSyncing(app);
            }
        } catch (RuntimeException ignored) {
            CodexWidgetProvider.updateAll(context);
        }
    }

    public static void cancel(Context context) {
        try {
            JobScheduler scheduler = context.getSystemService(JobScheduler.class);
            if (scheduler == null) return;
            scheduler.cancel(PERIODIC_JOB_ID);
            scheduler.cancel(IMMEDIATE_JOB_ID);
        } catch (RuntimeException ignored) {
            // Nothing remains alive in-process, so there is nothing else to stop.
        }
    }

    private static JobInfo.Builder base(Context context, int id) {
        return new JobInfo.Builder(id, new ComponentName(context, QuotaSyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
    }
}
