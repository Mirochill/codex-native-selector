package com.mirochill.codexquota;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compact, serialisable view of all values rendered by the home-screen widget. */
public final class QuotaSnapshot {
    private static final Pattern PERCENT = Pattern.compile("(\\d{1,3})\\s*%");

    public final String primaryLabel;
    public final String primaryValue;
    public final String secondaryLabel;
    public final String secondaryValue;
    public final long primaryResetsAt;
    public final long secondaryResetsAt;
    public final long dailyTokens;
    public final long lifetimeTokens;
    public final String planType;
    public final int resetCredits;
    public final long updatedAt;

    public QuotaSnapshot(String primaryLabel, String primaryValue,
                         String secondaryLabel, String secondaryValue,
                         long primaryResetsAt, long secondaryResetsAt,
                         long dailyTokens, long lifetimeTokens,
                         String planType, int resetCredits, long updatedAt) {
        this.primaryLabel = safe(primaryLabel, "5 H");
        this.primaryValue = safe(primaryValue, "—");
        this.secondaryLabel = safe(secondaryLabel, "7 J");
        this.secondaryValue = safe(secondaryValue, "—");
        this.primaryResetsAt = primaryResetsAt;
        this.secondaryResetsAt = secondaryResetsAt;
        this.dailyTokens = dailyTokens;
        this.lifetimeTokens = lifetimeTokens;
        this.planType = safe(planType, "CODEX");
        this.resetCredits = resetCredits;
        this.updatedAt = updatedAt;
    }

    public static QuotaSnapshot empty() {
        return new QuotaSnapshot("5 H", "—", "7 J", "—", 0L, 0L,
                -1L, -1L, "CODEX", -1, 0L);
    }

    public static QuotaSnapshot manual(String primary, String secondary) {
        return new QuotaSnapshot("5 H", primary, "7 J", secondary, 0L, 0L,
                -1L, -1L, "CODEX", -1, System.currentTimeMillis());
    }

    /** Parses the rate-limit payload returned by ChatGPT's Codex usage service. */
    public static QuotaSnapshot fromUsageJson(String raw, String fallbackPlan,
                                               QuotaSnapshot previous) {
        if (raw == null || raw.trim().isEmpty()) return previous == null ? empty() : previous;
        try {
            JSONObject root = new JSONObject(raw);
            JSONObject rateLimit = firstObject(root, "rate_limit", "rateLimits");
            JSONObject primary = rateLimit == null ? null
                    : firstObject(rateLimit, "primary_window", "primary");
            JSONObject secondary = rateLimit == null ? null
                    : firstObject(rateLimit, "secondary_window", "secondary");

            JSONArray additional = firstArray(root, "additional_rate_limits", "additionalRateLimits");
            if (secondary == null && additional != null) {
                JSONObject fallback = null;
                for (int i = 0; i < additional.length(); i++) {
                    JSONObject entry = additional.optJSONObject(i);
                    JSONObject additionalLimit = entry == null ? null
                            : firstObject(entry, "rate_limit", "rateLimit");
                    if (additionalLimit == null) continue;
                    JSONObject candidate = firstObject(additionalLimit,
                            "secondary_window", "secondary", "primary_window", "primary");
                    if (candidate == null) continue;
                    if (fallback == null) fallback = candidate;
                    if (windowSeconds(candidate) >= 2L * 24L * 60L * 60L) {
                        secondary = candidate;
                        break;
                    }
                }
                if (secondary == null) secondary = fallback;
            }

            String plan = firstString(root, "plan_type", "planType");
            if (plan.isEmpty() && rateLimit != null) {
                plan = firstString(rateLimit, "plan_type", "planType");
            }
            if (plan.isEmpty()) plan = fallbackPlan;
            if ((plan == null || plan.trim().isEmpty()) && previous != null) plan = previous.planType;

            JSONObject resetBank = firstObject(root,
                    "rate_limit_reset_credits", "rateLimitResetCredits");
            int resetCredits = resetBank == null ? -1
                    : firstInt(resetBank, -1, "available_count", "availableCount");
            if (resetCredits < 0 && previous != null) resetCredits = previous.resetCredits;

            return new QuotaSnapshot(
                    labelFor(primary, "5 H"), remaining(primary),
                    labelFor(secondary, "7 J"), remaining(secondary),
                    resetAt(primary), resetAt(secondary),
                    -1L,
                    previous == null ? -1L : previous.lifetimeTokens,
                    displayPlan(plan), resetCredits, System.currentTimeMillis());
        } catch (Exception ignored) {
            return previous == null ? empty() : previous;
        }
    }

    /** Merges the profile/token-activity response without discarding valid quota data. */
    public QuotaSnapshot withProfileJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) return this;
        try {
            JSONObject root = new JSONObject(raw);
            JSONObject stats = firstObject(root, "stats", "usage_stats", "usageStats");
            if (stats == null) stats = root;

            long lifetime = firstLong(stats, -1L, "lifetime_tokens", "lifetimeTokens");
            if (lifetime < 0L) lifetime = firstLong(root, -1L, "lifetime_tokens", "lifetimeTokens");
            if (lifetime < 0L) lifetime = lifetimeTokens;

            JSONArray daily = firstArray(root, "daily_usage_buckets", "dailyUsageBuckets");
            if (daily == null) daily = firstArray(stats, "daily_usage_buckets", "dailyUsageBuckets");
            long todayTokens = firstLong(root, -1L,
                    "today_tokens", "todayTokens", "daily_tokens", "dailyTokens");
            if (todayTokens < 0L) todayTokens = firstLong(stats, -1L,
                    "today_tokens", "todayTokens", "daily_tokens", "dailyTokens");
            if (todayTokens < 0L) todayTokens = tokensForToday(daily);

            return new QuotaSnapshot(primaryLabel, primaryValue, secondaryLabel, secondaryValue,
                    primaryResetsAt, secondaryResetsAt, todayTokens, lifetime,
                    planType, resetCredits, updatedAt);
        } catch (Exception ignored) {
            return this;
        }
    }

    public boolean hasAnyValue() {
        return !"—".equals(primaryValue) || !"—".equals(secondaryValue);
    }

    public long nextResetAt() {
        long now = System.currentTimeMillis();
        long first = primaryResetsAt > now ? primaryResetsAt : 0L;
        long second = secondaryResetsAt > now ? secondaryResetsAt : 0L;
        if (first == 0L) return second;
        if (second == 0L) return first;
        return Math.min(first, second);
    }

    public static int percentFromValue(String value) {
        if (value == null) return -1;
        Matcher matcher = PERCENT.matcher(value);
        if (!matcher.find()) return -1;
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(matcher.group(1))));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static String compactTokens(long value) {
        if (value < 0L) return "—";
        if (value < 1_000L) return Long.toString(value);
        if (value < 1_000_000L) return compact(value / 1_000d) + "K";
        if (value < 1_000_000_000L) return compact(value / 1_000_000d) + "M";
        return compact(value / 1_000_000_000d) + "B";
    }

    private static String compact(double value) {
        String result = value >= 100d
                ? String.format(Locale.US, "%.0f", value)
                : value >= 10d
                ? String.format(Locale.US, "%.1f", value)
                : String.format(Locale.US, "%.2f", value);
        return result.replaceAll("\\.?0+$", "");
    }

    private static long tokensForToday(JSONArray buckets) {
        if (buckets == null) return -1L;
        String localDay = dayKey(System.currentTimeMillis(), TimeZone.getDefault());
        String utcDay = dayKey(System.currentTimeMillis(), TimeZone.getTimeZone("UTC"));
        for (int i = 0; i < buckets.length(); i++) {
            JSONObject bucket = buckets.optJSONObject(i);
            if (bucket == null) continue;
            String start = firstString(bucket, "start_date", "startDate");
            if (!localDay.equals(start) && !utcDay.equals(start)) continue;
            return firstLong(bucket, -1L, "tokens", "token_count", "tokenCount");
        }
        return -1L;
    }

    private static String dayKey(long millis, TimeZone zone) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(zone);
        return format.format(new Date(millis));
    }

    private static String remaining(JSONObject window) {
        if (window == null) return "—";
        int used = firstInt(window, -1, "used_percent", "usedPercent");
        if (used < 0) return "—";
        return (100 - Math.max(0, Math.min(100, used))) + "%";
    }

    private static String labelFor(JSONObject window, String fallback) {
        long seconds = windowSeconds(window);
        if (seconds > 0L && seconds <= 8L * 60L * 60L) return "5 H";
        if (seconds >= 2L * 24L * 60L * 60L) return "7 J";
        return fallback;
    }

    private static long windowSeconds(JSONObject window) {
        if (window == null) return 0L;
        long seconds = firstLong(window, 0L, "limit_window_seconds", "limitWindowSeconds");
        if (seconds == 0L) {
            long minutes = firstLong(window, 0L, "window_duration_mins", "windowDurationMins");
            seconds = minutes * 60L;
        }
        return seconds;
    }

    private static long resetAt(JSONObject window) {
        if (window == null) return 0L;
        long timestamp = firstLong(window, 0L, "reset_at", "resets_at", "resetsAt");
        if (timestamp > 0L && timestamp < 10_000_000_000L) timestamp *= 1000L;
        if (timestamp > 0L) return timestamp;
        long after = firstLong(window, 0L, "reset_after_seconds", "resetAfterSeconds");
        return after > 0L ? System.currentTimeMillis() + after * 1000L : 0L;
    }

    private static String displayPlan(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "CODEX";
        String plan = raw.trim().toUpperCase(Locale.ROOT);
        if (plan.startsWith("CHATGPT_")) plan = plan.substring("CHATGPT_".length());
        return plan.length() > 12 ? plan.substring(0, 12) : plan;
    }

    private static JSONObject firstObject(JSONObject object, String... keys) {
        if (object == null) return null;
        for (String key : keys) {
            JSONObject value = object.optJSONObject(key);
            if (value != null) return value;
        }
        return null;
    }

    private static JSONArray firstArray(JSONObject object, String... keys) {
        if (object == null) return null;
        for (String key : keys) {
            JSONArray value = object.optJSONArray(key);
            if (value != null) return value;
        }
        return null;
    }

    private static String firstString(JSONObject object, String... keys) {
        if (object == null) return "";
        for (String key : keys) {
            String value = object.optString(key, "").trim();
            if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) return value;
        }
        return "";
    }

    private static int firstInt(JSONObject object, int fallback, String... keys) {
        long value = firstLong(object, fallback, keys);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) return fallback;
        return (int) value;
    }

    private static long firstLong(JSONObject object, long fallback, String... keys) {
        if (object == null) return fallback;
        for (String key : keys) {
            if (!object.has(key) || object.isNull(key)) continue;
            Object raw = object.opt(key);
            if (raw instanceof Number) return ((Number) raw).longValue();
            try {
                return Long.parseLong(String.valueOf(raw));
            } catch (NumberFormatException ignored) {
                // Try the next spelling.
            }
        }
        return fallback;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
