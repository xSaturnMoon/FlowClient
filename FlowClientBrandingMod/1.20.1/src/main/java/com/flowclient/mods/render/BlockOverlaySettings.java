package com.flowclient.mods.render;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class BlockOverlaySettings {
    private static final BlockOverlaySettings DEFAULTS = new BlockOverlaySettings();

    private BlockOverlayStyle style = BlockOverlayStyle.GLOW;
    private ScoreboardColor lineColor = ScoreboardColor.ACCENT;
    private ScoreboardColor glowColor = ScoreboardColor.AQUA;
    private int lineOpacity = 100;
    private int glowOpacity = 35;
    private float lineWidth = 2.5F;
    private float glowWidth = 5.5F;
    private boolean outline = true;
    private boolean pulse = true;
    private boolean rainbow = false;
    private boolean hideVanillaContrast = true;
    private int cornerLength = 6;

    private static BlockOverlaySettings current = copy(DEFAULTS);

    private BlockOverlaySettings() {
    }

    public static BlockOverlaySettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("blockOverlaySettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "blockOverlaySettings");
        BlockOverlaySettings settings = copy(DEFAULTS);
        settings.style = parseEnum(GsonHelper.getAsString(json, "style", DEFAULTS.style.name()), BlockOverlayStyle.class, DEFAULTS.style);
        settings.lineColor = parseColor(GsonHelper.getAsString(json, "lineColor", DEFAULTS.lineColor.name()), DEFAULTS.lineColor);
        settings.glowColor = parseColor(GsonHelper.getAsString(json, "glowColor", DEFAULTS.glowColor.name()), DEFAULTS.glowColor);
        settings.lineOpacity = clamp(GsonHelper.getAsInt(json, "lineOpacity", DEFAULTS.lineOpacity), 0, 100);
        settings.glowOpacity = clamp(GsonHelper.getAsInt(json, "glowOpacity", DEFAULTS.glowOpacity), 0, 100);
        settings.lineWidth = clampFloat(GsonHelper.getAsFloat(json, "lineWidth", DEFAULTS.lineWidth), 0.5F, 8.0F);
        settings.glowWidth = clampFloat(GsonHelper.getAsFloat(json, "glowWidth", DEFAULTS.glowWidth), 1.0F, 12.0F);
        settings.outline = GsonHelper.getAsBoolean(json, "outline", DEFAULTS.outline);
        settings.pulse = GsonHelper.getAsBoolean(json, "pulse", DEFAULTS.pulse);
        settings.rainbow = GsonHelper.getAsBoolean(json, "rainbow", DEFAULTS.rainbow);
        settings.hideVanillaContrast = GsonHelper.getAsBoolean(json, "hideVanillaContrast", DEFAULTS.hideVanillaContrast);
        settings.cornerLength = clamp(GsonHelper.getAsInt(json, "cornerLength", DEFAULTS.cornerLength), 2, 16);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("style", current.style.name());
        json.addProperty("lineColor", current.lineColor.name());
        json.addProperty("glowColor", current.glowColor.name());
        json.addProperty("lineOpacity", current.lineOpacity);
        json.addProperty("glowOpacity", current.glowOpacity);
        json.addProperty("lineWidth", current.lineWidth);
        json.addProperty("glowWidth", current.glowWidth);
        json.addProperty("outline", current.outline);
        json.addProperty("pulse", current.pulse);
        json.addProperty("rainbow", current.rainbow);
        json.addProperty("hideVanillaContrast", current.hideVanillaContrast);
        json.addProperty("cornerLength", current.cornerLength);
        root.add("blockOverlaySettings", json);
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
            case "lineColor" -> this.lineColor = DEFAULTS.lineColor;
            case "glowColor" -> this.glowColor = DEFAULTS.glowColor;
            case "lineOpacity" -> this.lineOpacity = DEFAULTS.lineOpacity;
            case "glowOpacity" -> this.glowOpacity = DEFAULTS.glowOpacity;
            case "lineWidth" -> this.lineWidth = DEFAULTS.lineWidth;
            case "glowWidth" -> this.glowWidth = DEFAULTS.glowWidth;
            case "outline" -> this.outline = DEFAULTS.outline;
            case "pulse" -> this.pulse = DEFAULTS.pulse;
            case "rainbow" -> this.rainbow = DEFAULTS.rainbow;
            case "hideVanillaContrast" -> this.hideVanillaContrast = DEFAULTS.hideVanillaContrast;
            case "cornerLength" -> this.cornerLength = DEFAULTS.cornerLength;
            default -> {
            }
        }
    }

    public BlockOverlayStyle style() { return this.style; }
    public ScoreboardColor lineColor() { return this.lineColor; }
    public ScoreboardColor glowColor() { return this.glowColor; }
    public int lineOpacity() { return this.lineOpacity; }
    public int glowOpacity() { return this.glowOpacity; }
    public float lineWidth() { return this.lineWidth; }
    public float glowWidth() { return this.glowWidth; }
    public boolean outline() { return this.outline; }
    public boolean pulse() { return this.pulse; }
    public boolean rainbow() { return this.rainbow; }
    public boolean hideVanillaContrast() { return this.hideVanillaContrast; }
    public int cornerLength() { return this.cornerLength; }

    public void setStyle(BlockOverlayStyle style) { this.style = style; }
    public void setLineColor(ScoreboardColor lineColor) { this.lineColor = lineColor; }
    public void setGlowColor(ScoreboardColor glowColor) { this.glowColor = glowColor; }
    public void setLineOpacity(int lineOpacity) { this.lineOpacity = clamp(lineOpacity, 0, 100); }
    public void setGlowOpacity(int glowOpacity) { this.glowOpacity = clamp(glowOpacity, 0, 100); }
    public void setLineWidth(float lineWidth) { this.lineWidth = clampFloat(lineWidth, 0.5F, 8.0F); }
    public void setGlowWidth(float glowWidth) { this.glowWidth = clampFloat(glowWidth, 1.0F, 12.0F); }
    public void setOutline(boolean outline) { this.outline = outline; }
    public void setPulse(boolean pulse) { this.pulse = pulse; }
    public void setRainbow(boolean rainbow) { this.rainbow = rainbow; }
    public void setHideVanillaContrast(boolean hideVanillaContrast) { this.hideVanillaContrast = hideVanillaContrast; }
    public void setCornerLength(int cornerLength) { this.cornerLength = clamp(cornerLength, 2, 16); }

    public int resolveLineColorArgb() {
        return withOpacity(resolveColor(this.lineColor), this.lineOpacity);
    }

    public int resolveGlowColorArgb() {
        return withOpacity(resolveColor(this.glowColor), this.glowOpacity);
    }

    public int resolveOutlineColorArgb() {
        return withOpacity(0xFF000000, Math.min(100, this.lineOpacity + 20));
    }

    private int resolveColor(ScoreboardColor color) {
        if (this.rainbow) {
            int hue = (int) (System.currentTimeMillis() / 12L % 360L);
            float h = hue / 360.0F;
            int rgb = java.awt.Color.HSBtoRGB(h, 0.85F, 1.0F) & 0xFFFFFF;
            return 0xFF000000 | rgb;
        }
        return color.argb();
    }

    public float resolveLineWidth() {
        if (!this.pulse) {
            return this.lineWidth;
        }
        float wave = (float) (Math.sin(System.currentTimeMillis() / 180.0) * 0.35 + 1.0);
        return this.lineWidth * wave;
    }

    public float resolveGlowWidth() {
        if (!this.pulse) {
            return this.glowWidth;
        }
        float wave = (float) (Math.sin(System.currentTimeMillis() / 220.0) * 0.45 + 1.0);
        return this.glowWidth * wave;
    }

    private static int withOpacity(int argb, int opacityPercent) {
        int alpha = (opacityPercent * 255 / 100) << 24;
        return alpha | (argb & 0x00FFFFFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static BlockOverlaySettings copy(BlockOverlaySettings source) {
        BlockOverlaySettings copy = new BlockOverlaySettings();
        copy.style = source.style;
        copy.lineColor = source.lineColor;
        copy.glowColor = source.glowColor;
        copy.lineOpacity = source.lineOpacity;
        copy.glowOpacity = source.glowOpacity;
        copy.lineWidth = source.lineWidth;
        copy.glowWidth = source.glowWidth;
        copy.outline = source.outline;
        copy.pulse = source.pulse;
        copy.rainbow = source.rainbow;
        copy.hideVanillaContrast = source.hideVanillaContrast;
        copy.cornerLength = source.cornerLength;
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
