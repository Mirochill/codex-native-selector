package com.mirochill.codexquota;

import android.app.job.JobParameters;
import android.app.job.JobService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ConcurrentHashMap;

/** Performs one short sync, then releases the process immediately. */
public class QuotaSyncJobService extends JobService {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<Integer, Future<?>> running = new ConcurrentHashMap<>();

    @Override
    public boolean onStartJob(JobParameters params) {
        if (!ChatGptAuthStore.hasTokens(this) || !CodexWidgetProvider.hasWidgets(this)) {
            return false;
        }

        QuotaSnapshot current = QuotaStore.get(this);
        boolean periodic = params.getJobId() == AutoSyncScheduler.PERIODIC_JOB_ID;
        long recentEnough = Math.min(20L * 60L * 1000L,
                Math.max(2L * 60L * 1000L, SyncPreferences.intervalMillis(this) / 2L));
        if (periodic && current.updatedAt > 0L
                && System.currentTimeMillis() - current.updatedAt < recentEnough) {
            CodexWidgetProvider.updateAll(this);
            return false;
        }

        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                QuotaSnapshot snapshot = ChatGptAuthClient.sync(this);
                QuotaStore.save(this, snapshot);
            } catch (Exception ignored) {
                // Keep the last valid snapshot; the next scheduled run can try again.
            } finally {
                CodexWidgetProvider.updateAll(this);
                if (running.remove(params.getJobId()) != null) {
                    jobFinished(params, false);
                }
            }
        }, null);
        running.put(params.getJobId(), task);
        executor.execute(task);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Future<?> task = running.remove(params.getJobId());
        if (task != null) task.cancel(true);
        return false;
    }

    @Override
    public void onDestroy() {
        for (Future<?> task : running.values()) task.cancel(true);
        running.clear();
        executor.shutdownNow();
        super.onDestroy();
    }
}
