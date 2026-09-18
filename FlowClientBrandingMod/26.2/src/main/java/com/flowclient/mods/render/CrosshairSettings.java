package com.flowclient.mods.render;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class CrosshairSettings {
    private static final CrosshairSettings DEFAULTS = new CrosshairSettings();

    private CrosshairStyle style = CrosshairStyle.GAP_CROSS;
    private ScoreboardColor color = ScoreboardColor.WHITE;
    private ScoreboardColor outlineColor = ScoreboardColor.BLACK;
    private int size = 6;
    private int thickness = 1;
    private int gap = 2;
    private int dotSize = 1;
    private int opacity = 100;
    private int outlineOpacity = 80;
    private boolean outline = true;
    private boolean dot = false;
    private boolean rainbow = false;
    private boolean dynamicColor = false;
    private boolean showWhileDebug = false;

    private static CrosshairSettings current = copy(DEFAULTS);

    private CrosshairSettings() {
    }

    public static CrosshairSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("crosshairSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "crosshairSettings");
        CrosshairSettings settings = copy(DEFAULTS);
        settings.style = parseEnum(GsonHelper.getAsString(json, "style", DEFAULTS.style.name()), CrosshairStyle.class, DEFAULTS.style);
        settings.color = parseColor(GsonHelper.getAsString(json, "color", DEFAULTS.color.name()), DEFAULTS.color);
        settings.outlineColor = parseColor(GsonHelper.getAsString(json, "outlineColor", DEFAULTS.outlineColor.name()), DEFAULTS.outlineColor);
        settings.size = clamp(GsonHelper.getAsInt(json, "size", DEFAULTS.size), 2, 16);
        settings.thickness = clamp(GsonHelper.getAsInt(json, "thickness", DEFAULTS.thickness), 1, 4);
        settings.gap = clamp(GsonHelper.getAsInt(json, "gap", DEFAULTS.gap), 0, 8);
        settings.dotSize = clamp(GsonHelper.getAsInt(json, "dotSize", DEFAULTS.dotSize), 1, 4);
        settings.opacity = clamp(GsonHelper.getAsInt(json, "opacity", DEFAULTS.opacity), 0, 100);
        settings.outlineOpacity = clamp(GsonHelper.getAsInt(json, "outlineOpacity", DEFAULTS.outlineOpacity), 0, 100);
        settings.outline = GsonHelper.getAsBoolean(json, "outline", DEFAULTS.outline);
        settings.dot = GsonHelper.getAsBoolean(json, "dot", DEFAULTS.dot);
        settings.rainbow = GsonHelper.getAsBoolean(json, "rainbow", DEFAULTS.rainbow);
        settings.dynamicColor = GsonHelper.getAsBoolean(json, "dynamicColor", DEFAULTS.dynamicColor);
        settings.showWhileDebug = GsonHelper.getAsBoolean(json, "showWhileDebug", DEFAULTS.showWhileDebug);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("style", current.style.name());
        json.addProperty("color", current.color.name());
        json.addProperty("outlineColor", current.outlineColor.name());
        json.addProperty("size", current.size);
        json.addProperty("thickness", current.thickness);
        json.addProperty("gap", current.gap);
        json.addProperty("dotSize", current.dotSize);
        json.addProperty("opacity", current.opacity);
        json.addProperty("outlineOpacity", current.outlineOpacity);
        json.addProperty("outline", current.outline);
        json.addProperty("dot", current.dot);
        json.addProperty("rainbow", current.rainbow);
        json.addProperty("dynamicColor", current.dynamicColor);
        json.addProperty("showWhileDebug", current.showWhileDebug);
        root.add("crosshairSettings", json);
    }

    public static void reset(boolean persist) {
        current = copy(DEFAULTS);
        if (persist) {
            FlowModConfig.save();
        }
    }

    public void persist() {
        FlowModConfig.save();
    }

    public void resetField(String fieldId) {
        switch (fieldId) {
            case "style" -> this.style = DEFAULTS.style;
            case "color" -> this.color = DEFAULTS.color;
            case "outlineColor" -> this.outlineColor = DEFAULTS.outlineColor;
            case "size" -> this.size = DEFAULTS.size;
            case "thickness" -> this.thickness = DEFAULTS.thickness;
            case "gap" -> this.gap = DEFAULTS.gap;
            case "dotSize" -> this.dotSize = DEFAULTS.dotSize;
            case "opacity" -> this.opacity = DEFAULTS.opacity;
            case "outlineOpacity" -> this.outlineOpacity = DEFAULTS.outlineOpacity;
            case "outline" -> this.outline = DEFAULTS.outline;
            case "dot" -> this.dot = DEFAULTS.dot;
            case "rainbow" -> this.rainbow = DEFAULTS.rainbow;
            case "dynamicColor" -> this.dynamicColor = DEFAULTS.dynamicColor;
            case "showWhileDebug" -> this.showWhileDebug = DEFAULTS.showWhileDebug;
            default -> {
            }
        }
    }

    public CrosshairStyle style() { return this.style; }
    public ScoreboardColor color() { return this.color; }
    public ScoreboardColor outlineColor() { return this.outlineColor; }
    public int size() { return this.size; }
    public int thickness() { return this.thickness; }
    public int gap() { return this.gap; }
    public int dotSize() { return this.dotSize; }
    public int opacity() { return this.opacity; }
    public int outlineOpacity() { return this.outlineOpacity; }
    public boolean outline() { return this.outline; }
    public boolean dot() { return this.dot; }
    public boolean rainbow() { return this.rainbow; }
    public boolean dynamicColor() { return this.dynamicColor; }
    public boolean showWhileDebug() { return this.showWhileDebug; }

    public void setStyle(CrosshairStyle style) { this.style = style; }
    public void setColor(ScoreboardColor color) { this.color = color; }
    public void setOutlineColor(ScoreboardColor outlineColor) { this.outlineColor = outlineColor; }
    public void setSize(int size) { this.size = clamp(size, 2, 16); }
    public void setThickness(int thickness) { this.thickness = clamp(thickness, 1, 4); }
    public void setGap(int gap) { this.gap = clamp(gap, 0, 8); }
    public void setDotSize(int dotSize) { this.dotSize = clamp(dotSize, 1, 4); }
    public void setOpacity(int opacity) { this.opacity = clamp(opacity, 0, 100); }
    public void setOutlineOpacity(int outlineOpacity) { this.outlineOpacity = clamp(outlineOpacity, 0, 100); }
    public void setOutline(boolean outline) { this.outline = outline; }
    public void setDot(boolean dot) { this.dot = dot; }
    public void setRainbow(boolean rainbow) { this.rainbow = rainbow; }
    public void setDynamicColor(boolean dynamicColor) { this.dynamicColor = dynamicColor; }
    public void setShowWhileDebug(boolean showWhileDebug) { this.showWhileDebug = showWhileDebug; }

    public int resolveColorArgb(boolean entityUnderCrosshair) {
        if (this.rainbow) {
            int hue = (int) (System.currentTimeMillis() / 14L % 360L);
            float h = hue / 360.0F;
            int rgb = java.awt.Color.HSBtoRGB(h, 0.85F, 1.0F) & 0xFFFFFF;
            return withOpacity(0xFF000000 | rgb, this.opacity);
        }
        if (this.dynamicColor && entityUnderCrosshair) {
            return withOpacity(ScoreboardColor.RED.argb(), this.opacity);
        }
        return withOpacity(this.color.argb(), this.opacity);
    }

    public int resolveOutlineColorArgb() {
        return withOpacity(this.outlineColor.argb(), this.outlineOpacity);
    }

    private static int withOpacity(int argb, int opacityPercent) {
        int alpha = (opacityPercent * 255 / 100) << 24;
        return alpha | (argb & 0x00FFFFFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static CrosshairSettings copy(CrosshairSettings source) {
        CrosshairSettings copy = new CrosshairSettings();
        copy.style = source.style;
        copy.color = source.color;
        copy.outlineColor = source.outlineColor;
        copy.size = source.size;
        copy.thickness = source.thickness;
        copy.gap = source.gap;
        copy.dotSize = source.dotSize;
        copy.opacity = source.opacity;
        copy.outlineOpacity = source.outlineOpacity;
        copy.outline = source.outline;
        copy.dot = source.dot;
        copy.rainbow = source.rainbow;
        copy.dynamicColor = source.dynamicColor;
        copy.showWhileDebug = source.showWhileDebug;
        return copy;
    }

    private static ScoreboardColor parseColor(String value, ScoreboardColor fallback) {
        try {
            return ScoreboardColor.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
