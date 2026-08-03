package com.mirochill.codexquota;

import android.content.Context;
import android.content.SharedPreferences;

/** Local-only storage; no quota or account data leaves the phone. */
public final class QuotaStore {
    private static final String PREFS = "codex_quota_local";
    private static final String PRIMARY_LABEL = "primary_label";
    private static final String PRIMARY_VALUE = "primary_value";
    private static final String SECONDARY_LABEL = "secondary_label";
    private static final String SECONDARY_VALUE = "secondary_value";
    private static final String UPDATED_AT = "updated_at";

    private QuotaStore() {}

    public static QuotaSnapshot get(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new QuotaSnapshot(
                p.getString(PRIMARY_LABEL, "5h"),
                p.getString(PRIMARY_VALUE, "—"),
                p.getString(SECONDARY_LABEL, "Semaine"),
                p.getString(SECONDARY_VALUE, "—"),
                p.getLong(UPDATED_AT, 0L));
    }

    public static void save(Context context, QuotaSnapshot snapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(PRIMARY_LABEL, snapshot.primaryLabel)
                .putString(PRIMARY_VALUE, snapshot.primaryValue)
                .putString(SECONDARY_LABEL, snapshot.secondaryLabel)
                .putString(SECONDARY_VALUE, snapshot.secondaryValue)
                .putLong(UPDATED_AT, snapshot.updatedAt)
                .apply();
    }
}
