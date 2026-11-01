package com.killercats.playletmobile;

import java.util.Locale;

/** Display text: "1.2M views", "12:34". */
public final class Format {
    private Format() {}

    public static String views(long count) {
        if (count < 0) return "";
        if (count < 1000) return count + (count == 1 ? " view" : " views");
        if (count < 1000000) return trim(count / 1000.0) + "K views";
        if (count < 1000000000L) return trim(count / 1000000.0) + "M views";
        return trim(count / 1000000000.0) + "B views";
    }

    public static String count(long count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return trim(count / 1000.0) + "K";
        return trim(count / 1000000.0) + "M";
    }

    private static String trim(double value) {
        if (value >= 100) return String.valueOf((long) value);
        String s = String.format(Locale.US, "%.1f", value);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    public static String duration(long seconds) {
        if (seconds <= 0) return "";
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    /** "Channel · 1.2M views · 3 days ago", leaving out empty parts. */
    public static String meta(Video v) {
        StringBuilder sb = new StringBuilder();
        append(sb, v.author);
        append(sb, v.live ? "LIVE" : views(v.viewCount));
        append(sb, v.publishedText);
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (part == null || part.isEmpty()) return;
        if (sb.length() > 0) sb.append(" · ");
        sb.append(part);
    }
}
