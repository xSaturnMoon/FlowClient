package com.flowclient.mods.hud;

import java.util.Locale;

public final class HudScale {
    public static final float MIN = 0.25f;
    public static final float MAX = 3.0f;
    public static final float DEFAULT = 1.0f;

    private HudScale() {}

    public static float clamp(float scale) {
        if (Float.isNaN(scale) || Float.isInfinite(scale)) {
            return DEFAULT;
        }
        return Math.max(MIN, Math.min(MAX, scale));
    }

    public static boolean isDefault(float scale) {
        return Math.abs(scale - DEFAULT) < 0.001f;
    }

    public static boolean isUnity(float scale) {
        return Math.abs(scale - DEFAULT) < 0.001f;
    }

    /**
     * Parses a user-entered decimal, accepting both {@code 1.00} and {@code 1,00}.
     * Returns null while the text is still being typed (e.g. {@code "0."} or {@code "0,"}).
     */
    public static Float tryParse(String text) {
        if (text == null) return null;
        String normalized = text.trim().replace(',', '.');
        if (normalized.isEmpty()) return null;
        if (normalized.endsWith(".")
                || normalized.equals("-")
                || normalized.equals("+")
                || normalized.equals("0")
                || normalized.equals("-0")
                || normalized.equals("0.")
                || normalized.equals("0,")) {
            return null;
        }
        try {
            return clamp(Float.parseFloat(normalized));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static float parseOrDefault(String text, float fallback) {
        Float parsed = tryParse(text);
        return parsed != null ? parsed : clamp(fallback);
    }

    public static String format(float scale) {
        return String.format(Locale.ROOT, "%.2f", clamp(scale));
    }
}
