package com.mirochill.codexquota;

import android.content.Context;
import android.content.SharedPreferences;

/** User-selected refresh cadence. Android's minimum background cadence is 15 minutes. */
public final class SyncPreferences {
    private static final String PREFS = "codex_sync_settings";
    private static final String INTERVAL = "interval_minutes";
    private static final String NEXT_ALARM_AT = "next_alarm_at";
    public static final long DISABLED = 0L;
    private static final int DEFAULT_MINUTES = 30;

    public static final String[] LABELS = {
            "Toutes les 15 minutes",
            "Toutes les 30 minutes",
            "Toutes les heures",
            "Toutes les 2 heures",
            "Toutes les 4 heures",
            "Toutes les 8 heures",
            "Toutes les 12 heures",
            "Toutes les 24 heures",
            "Désactivée"
    };

    private static final int[] MINUTES = {15, 30, 60, 120, 240, 480, 720, 1_440, 0};

    private SyncPreferences() {}

    public static long intervalMillis(Context context) {
        return (long) selectedMinutes(context) * 60L * 1000L;
    }

    public static int selectedIndex(Context context) {
        int selected = selectedMinutes(context);
        for (int i = 0; i < MINUTES.length; i++) {
            if (MINUTES[i] == selected) return i;
        }
        return 1;
    }

    public static String currentLabel(Context context) {
        return LABELS[selectedIndex(context)];
    }

    public static void select(Context context, int index) {
        int safe = index >= 0 && index < MINUTES.length ? index : 1;
        prefs(context).edit()
                .putInt(INTERVAL, MINUTES[safe])
                .putLong(NEXT_ALARM_AT, 0L)
                .apply();
    }

    static long nextAlarmAt(Context context) {
        return prefs(context).getLong(NEXT_ALARM_AT, 0L);
    }

    static void markNextAlarmAt(Context context, long timestamp) {
        prefs(context).edit().putLong(NEXT_ALARM_AT, Math.max(0L, timestamp)).apply();
    }

    private static int selectedMinutes(Context context) {
        int stored = prefs(context).getInt(INTERVAL, DEFAULT_MINUTES);
        for (int value : MINUTES) if (value == stored) return stored;
        return DEFAULT_MINUTES;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
