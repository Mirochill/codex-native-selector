package com.mirochill.codexquota;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A small, serialisable view of the last quota values found on the usage page. */
public final class QuotaSnapshot {
    private static final Pattern VALUE = Pattern.compile(
            "(?i)(\\b\\d{1,3}\\s*%|\\b\\d+\\s*(?:messages?|requests?|requêtes?|credits?|crédits?|tokens?)\\b|\\b\\d+\\s*(?:h|hr|hours?|heures?|m|min|minutes?)\\b)");
    private static final Pattern TOPIC = Pattern.compile(
            "(?i)\\b(5\\s*h|5\\s*hours?|daily|day|journalier|jour|weekly|week|hebdo|semaine)\\b");
    private static final Pattern SIGNAL = Pattern.compile(
            "(?i)(remaining|restant|restants|available|disponible|quota|limit|limite|usage|used|reset|renouvel|réinitial)");
    private static final Pattern PERCENT = Pattern.compile("(\\d{1,3})\\s*%");

    public final String primaryLabel;
    public final String primaryValue;
    public final String secondaryLabel;
    public final String secondaryValue;
    public final long updatedAt;

    public QuotaSnapshot(String primaryLabel, String primaryValue,
                         String secondaryLabel, String secondaryValue, long updatedAt) {
        this.primaryLabel = safe(primaryLabel, "5h");
        this.primaryValue = safe(primaryValue, "—");
        this.secondaryLabel = safe(secondaryLabel, "Semaine");
        this.secondaryValue = safe(secondaryValue, "—");
        this.updatedAt = updatedAt;
    }

    public static QuotaSnapshot empty() {
        return new QuotaSnapshot("5h", "—", "Semaine", "—", 0L);
    }

    public static QuotaSnapshot manual(String primary, String secondary) {
        return new QuotaSnapshot("5h", primary, "Semaine", secondary, System.currentTimeMillis());
    }

    /**
     * Converts the authenticated Codex usage response into the two values shown by the widget.
     * The backend reports used_percent, so the widget deliberately exposes the remaining percent.
     */
    public static QuotaSnapshot fromUsageJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) return empty();
        try {
            JSONObject root = new JSONObject(raw);
            JSONObject rateLimit = root.optJSONObject("rate_limit");
            if (rateLimit == null) rateLimit = root.optJSONObject("rateLimits");

            JSONObject primary = rateLimit == null ? null : rateLimit.optJSONObject("primary_window");
            JSONObject secondary = rateLimit == null ? null : rateLimit.optJSONObject("secondary_window");

            JSONArray additional = root.optJSONArray("additional_rate_limits");
            if (secondary == null && additional != null && additional.length() > 0) {
                JSONObject first = additional.optJSONObject(0);
                JSONObject additionalLimit = first == null ? null : first.optJSONObject("rate_limit");
                secondary = additionalLimit == null ? null : additionalLimit.optJSONObject("secondary_window");
                if (secondary == null && additionalLimit != null) {
                    secondary = additionalLimit.optJSONObject("primary_window");
                }
            }

            String primaryValue = remaining(primary);
            String secondaryValue = remaining(secondary);
            String primaryLabel = labelFor(primary, "5 h");
            String secondaryLabel = labelFor(secondary, "Semaine");
            return new QuotaSnapshot(primaryLabel, primaryValue, secondaryLabel, secondaryValue,
                    System.currentTimeMillis());
        } catch (Exception ignored) {
            return empty();
        }
    }

    public boolean hasAnyValue() {
        return !"—".equals(primaryValue) || !"—".equals(secondaryValue);
    }

    /** Returns a percentage suitable for a remaining-quota progress bar, or -1 when absent. */
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

    /**
     * Extracts only visible text from the dashboard. This intentionally avoids
     * private API endpoints and works as a best-effort parser when the page UI changes.
     */
    public static QuotaSnapshot fromPageText(String raw) {
        if (raw == null || raw.trim().isEmpty()) return empty();

        List<String> candidates = new ArrayList<>();
        String[] lines = raw.replace('\u00a0', ' ').split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String clean = line.replaceAll("\\s+", " ").trim();
            if (clean.length() < 2 || clean.length() > 240) continue;
            boolean topic = TOPIC.matcher(clean).find();
            if ((!SIGNAL.matcher(clean).find() && !topic) && !VALUE.matcher(clean).find()) continue;

            // Responsive dashboards often render the label and percentage on separate lines.
            if (topic && !VALUE.matcher(clean).find()) {
                for (int j = i + 1; j < Math.min(lines.length, i + 4); j++) {
                    String next = lines[j].replaceAll("\\s+", " ").trim();
                    if (next.isEmpty()) continue;
                    if (TOPIC.matcher(next).find()) break;
                    if (VALUE.matcher(next).find() || next.matches(".*\\d.*")) {
                        clean = clean + " " + next;
                        break;
                    }
                }
            }
            if (VALUE.matcher(clean).find() || clean.matches(".*\\d.*")) {
                candidates.add(clean);
            }
        }

        String[] primary = findCandidate(candidates, true, null);
        String[] secondary = findCandidate(candidates, false, primary == null ? null : primary[2]);
        if (primary == null) primary = findCandidate(candidates, false, null);
        if (secondary == null) secondary = findCandidate(candidates, false, primary == null ? null : primary[2]);

        String pLabel = primary == null ? "5h" : primary[0];
        String pValue = primary == null ? "—" : primary[1];
        String sLabel = secondary == null ? "Semaine" : secondary[0];
        String sValue = secondary == null ? "—" : secondary[1];
        return new QuotaSnapshot(pLabel, pValue, sLabel, sValue, System.currentTimeMillis());
    }

    private static String[] findCandidate(List<String> candidates, boolean preferPrimary, String skip) {
        for (String candidate : candidates) {
            if (skip != null && skip.equals(candidate)) continue;
            String lower = candidate.toLowerCase(Locale.ROOT);
            boolean isPrimary = lower.matches(".*(5\\s*h|5\\s*hours?|daily|day|journalier|jour).*" );
            boolean isSecondary = lower.matches(".*(weekly|week|hebdo|semaine).*" );
            if (preferPrimary && !isPrimary) continue;
            if (!preferPrimary && isPrimary && !isSecondary) continue;
            String value = valueFrom(candidate);
            if (value.isEmpty()) continue;
            String label = labelFrom(candidate, preferPrimary ? "5h" : "Semaine");
            return new String[]{label, value, candidate};
        }
        return null;
    }

    private static String valueFrom(String line) {
        Matcher matcher = VALUE.matcher(line);
        if (matcher.find()) return matcher.group(1).replaceAll("\\s+", "");
        Matcher number = Pattern.compile("\\b\\d{1,4}\\b").matcher(line);
        return number.find() ? number.group() : "";
    }

    private static String labelFrom(String line, String fallback) {
        Matcher topic = TOPIC.matcher(line);
        if (topic.find()) return topic.group(1).replaceAll("\\s+", " ");
        return fallback;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String remaining(JSONObject window) {
        if (window == null || !window.has("used_percent")) return "—";
        int used = Math.max(0, Math.min(100, window.optInt("used_percent", 100)));
        return (100 - used) + "%";
    }

    private static String labelFor(JSONObject window, String fallback) {
        if (window == null) return fallback;
        long seconds = window.optLong("limit_window_seconds", 0L);
        if (seconds > 0L && seconds <= 8L * 60L * 60L) return "5 h";
        if (seconds >= 2L * 24L * 60L * 60L) return "Semaine";
        return fallback;
    }
}
