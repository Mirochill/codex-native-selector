package com.mirochill.codexquota;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;

/** Schedules short quota fetches without keeping a process or timer alive. */
public final class AutoSyncScheduler {
    static final int PERIODIC_JOB_ID = 0xC0D301;
    static final int IMMEDIATE_JOB_ID = 0xC0D302;
    private static final int ALARM_REQUEST_CODE = 0xC0D303;
    private static final String EXTRA_SCHEDULE_REVISION = "schedule_revision";
    private static final int SCHEDULE_REVISION = 3;

    private AutoSyncScheduler() {}

    public static void ensureScheduled(Context context) {
        try {
            Context app = context.getApplicationContext();
            long period = SyncPreferences.intervalMillis(app);
            if (period == SyncPreferences.DISABLED) {
                cancel(app);
                return;
            }
            if (!ChatGptAuthStore.hasTokens(app) || !CodexWidgetProvider.hasWidgets(app)) return;

            JobScheduler scheduler = app.getSystemService(JobScheduler.class);
            if (scheduler != null) {
                JobInfo pending = scheduler.getPendingJob(PERIODIC_JOB_ID);
                boolean current = pending != null
                        && pending.getIntervalMillis() == period
                        && pending.getExtras().getInt(EXTRA_SCHEDULE_REVISION, 0)
                        == SCHEDULE_REVISION;
                if (!current) {
                    if (pending != null) scheduler.cancel(PERIODIC_JOB_ID);
                    long flex = Math.max(5L * 60L * 1000L,
                            Math.min(10L * 60L * 1000L, period / 3L));
                    JobInfo job = base(app, PERIODIC_JOB_ID)
                            .setExtras(scheduleExtras())
                            .setPersisted(true)
                            .setPeriodic(period, flex)
                            .build();
                    scheduler.schedule(job);
                }
            }

            // Samsung can defer periodic jobs heavily. A single self-renewing alarm wakes
            // the job at the chosen cadence while keeping no service alive between syncs.
            ensureAlarmScheduled(app, period);
        } catch (RuntimeException ignored) {
            // Scheduling must never crash the app or the widget host.
        }
    }

    public static void reschedule(Context context) {
        Context app = context.getApplicationContext();
        try {
            JobScheduler scheduler = app.getSystemService(JobScheduler.class);
            if (scheduler != null) scheduler.cancel(PERIODIC_JOB_ID);
        } catch (RuntimeException ignored) {
            // ensureScheduled below performs the same best-effort recovery.
        }
        cancelAlarm(app);
        ensureScheduled(app);
    }

    public static void onAlarm(Context context) {
        Context app = context.getApplicationContext();
        SyncPreferences.markNextAlarmAt(app, 0L);
        ensureScheduled(app);
        requestSyncIfDue(app);
    }

    /** Starts a real authenticated fetch when the last successful one is old enough. */
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
            // The periodic job and alarm remain available if this fallback fails.
        }
    }

    public static void requestImmediateSync(Context context) {
        try {
            Context app = context.getApplicationContext();
            if (!ChatGptAuthStore.hasTokens(app) || !CodexWidgetProvider.hasWidgets(app)) return;
            JobScheduler scheduler = app.getSystemService(JobScheduler.class);
            if (scheduler == null || scheduler.getPendingJob(IMMEDIATE_JOB_ID) != null) return;

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
        Context app = context.getApplicationContext();
        try {
            JobScheduler scheduler = app.getSystemService(JobScheduler.class);
            if (scheduler != null) {
                scheduler.cancel(PERIODIC_JOB_ID);
                scheduler.cancel(IMMEDIATE_JOB_ID);
            }
        } catch (RuntimeException ignored) {
            // No process or timer remains alive in the application.
        }
        cancelAlarm(app);
    }

    private static void ensureAlarmScheduled(Context context, long period) {
        long now = System.currentTimeMillis();
        long existing = SyncPreferences.nextAlarmAt(context);
        if (existing > now + 30_000L && existing <= now + period + 60_000L) return;

        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) return;
        long next = now + period;
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, alarmIntent(context));
        SyncPreferences.markNextAlarmAt(context, next);
    }

    private static void cancelAlarm(Context context) {
        try {
            AlarmManager alarms = context.getSystemService(AlarmManager.class);
            if (alarms != null) alarms.cancel(alarmIntent(context));
        } catch (RuntimeException ignored) {
            // The JobScheduler path remains available.
        }
        SyncPreferences.markNextAlarmAt(context, 0L);
    }

    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, WidgetRefreshReceiver.class)
                .setAction(WidgetRefreshReceiver.ACTION_AUTO_SYNC_ALARM);
        return PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
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
