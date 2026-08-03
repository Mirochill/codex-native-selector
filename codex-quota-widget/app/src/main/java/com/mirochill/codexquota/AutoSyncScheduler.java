package com.mirochill.codexquota;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

/** Lets Android batch quota refreshes instead of keeping a process or timer alive. */
public final class AutoSyncScheduler {
    static final int PERIODIC_JOB_ID = 0xC0D301;
    static final int IMMEDIATE_JOB_ID = 0xC0D302;
    private static final String EXTRA_SCHEDULE_REVISION = "schedule_revision";
    private static final int SCHEDULE_REVISION = 2;
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
            if (pending != null
                    && pending.getIntervalMillis() == period
                    && pending.getExtras().getInt(EXTRA_SCHEDULE_REVISION, 0)
                    == SCHEDULE_REVISION) return;
            if (pending != null) scheduler.cancel(PERIODIC_JOB_ID);

            long flex = Math.max(5L * 60L * 1000L,
                    Math.min(10L * 60L * 1000L, period / 3L));
            JobInfo job = base(app, PERIODIC_JOB_ID)
                    .setExtras(scheduleExtras())
                    .setPersisted(true)
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

    /**
     * Converts a launcher/widget refresh into a real authenticated sync whenever the
     * last successful server fetch is older than the interval selected by the user.
     * This is a low-cost fallback for manufacturers that aggressively defer periodic jobs.
     */
    public static void requestSyncIfDue(Context context) {
        try {
            Context app = context.getApplicationContext();
            long interval = SyncPreferences.intervalMillis(app);
            if (interval == SyncPreferences.DISABLED
                    || !ChatGptAuthStore.hasTokens(app)
                    || !CodexWidgetProvider.hasWidgets(app)) return;

            long updatedAt = QuotaStore.get(app).updatedAt;
            long age = updatedAt <= 0L ? Long.MAX_VALUE
                    : Math.max(0L, System.currentTimeMillis() - updatedAt);
            long tolerance = Math.min(60_000L, Math.max(5_000L, interval / 20L));
            if (age >= interval - tolerance) requestImmediateSync(app);
        } catch (RuntimeException ignored) {
            // The normal periodic job remains available if the launcher fallback fails.
        }
    }

    public static void requestImmediateSync(Context context) {
        try {
            Context app = context.getApplicationContext();
            if (!ChatGptAuthStore.hasTokens(app) || !CodexWidgetProvider.hasWidgets(app)) return;
            JobScheduler scheduler = app.getSystemService(JobScheduler.class);
            if (scheduler == null) return;

            // A widget update, app resume and package event can arrive together. Keep one
            // network request in flight instead of replacing the same immediate job repeatedly.
            if (scheduler.getPendingJob(IMMEDIATE_JOB_ID) != null) return;

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

    private static PersistableBundle scheduleExtras() {
        PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_SCHEDULE_REVISION, SCHEDULE_REVISION);
        return extras;
    }
}
