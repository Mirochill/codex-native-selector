package com.mirochill.codexquota;

import android.content.Context;
import android.content.SharedPreferences;

/** Local-only storage; no quota or account data leaves the phone. */
public final class QuotaStore {
    private static final String PREFS = "codex_quota_local";

    private QuotaStore() {}

    public static QuotaSnapshot get(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new QuotaSnapshot(
                p.getString("primary_label", "5 H"),
                p.getString("primary_value", "—"),
                p.getString("secondary_label", "7 J"),
                p.getString("secondary_value", "—"),
                p.getLong("primary_resets_at", 0L),
                p.getLong("secondary_resets_at", 0L),
                p.getLong("daily_tokens", -1L),
                p.getLong("lifetime_tokens", -1L),
                p.getString("plan_type", "CODEX"),
                p.getInt("reset_credits", -1),
                p.getLong("updated_at", 0L));
    }

    public static void save(Context context, QuotaSnapshot snapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("primary_label", snapshot.primaryLabel)
                .putString("primary_value", snapshot.primaryValue)
                .putString("secondary_label", snapshot.secondaryLabel)
                .putString("secondary_value", snapshot.secondaryValue)
                .putLong("primary_resets_at", snapshot.primaryResetsAt)
                .putLong("secondary_resets_at", snapshot.secondaryResetsAt)
                .putLong("daily_tokens", snapshot.dailyTokens)
                .putLong("lifetime_tokens", snapshot.lifetimeTokens)
                .putString("plan_type", snapshot.planType)
                .putInt("reset_credits", snapshot.resetCredits)
                .putLong("updated_at", snapshot.updatedAt)
                .apply();
    }
}
