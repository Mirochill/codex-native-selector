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

        FutureTask<Void> task = new FutureTask<>(() -> {
            boolean retry = false;
            try {
                // Every scheduled execution performs a fresh authenticated server request.
                // Redrawing cached values is handled separately by CodexWidgetProvider.
                QuotaSnapshot snapshot = ChatGptAuthClient.sync(this);
                QuotaStore.save(this, snapshot);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                retry = true;
            } catch (java.io.IOException transientNetworkFailure) {
                retry = true;
            } catch (Exception ignored) {
                // Auth/server errors keep the last snapshot and wait for the next interval.
            } finally {
                CodexWidgetProvider.updateAll(this);
                if (running.remove(params.getJobId()) != null) {
                    jobFinished(params, retry);
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
        return true;
    }

    @Override
    public void onDestroy() {
        for (Future<?> task : running.values()) task.cancel(true);
        running.clear();
        executor.shutdownNow();
        super.onDestroy();
    }
}
