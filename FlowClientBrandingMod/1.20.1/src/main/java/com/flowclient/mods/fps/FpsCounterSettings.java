package com.flowclient.mods.fps;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class FpsCounterSettings {
    private static final FpsCounterSettings DEFAULTS = new FpsCounterSettings();

    private FpsDisplayFormat format = FpsDisplayFormat.FPS_SUFFIX;
    private FpsColorMode colorMode = FpsColorMode.DYNAMIC;
    private boolean showBackground = true;
    private int backgroundOpacity = 70;
    private boolean textShadow = true;
    private boolean smoothing = true;
    private int paddingX = 6;
    private int paddingY = 3;
    private int lowThreshold = 30;
    private int midThreshold = 60;
    private int highThreshold = 120;
    private boolean showOutline = false;
    private boolean boldNumbers = false;

    private static FpsCounterSettings current = copy(DEFAULTS);

    private FpsCounterSettings() {
    }

    public static FpsCounterSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("fpsCounterSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "fpsCounterSettings");
        FpsCounterSettings settings = copy(DEFAULTS);
        settings.format = parseEnum(GsonHelper.getAsString(json, "format", DEFAULTS.format.name()), FpsDisplayFormat.class, DEFAULTS.format);
        settings.colorMode = parseEnum(GsonHelper.getAsString(json, "colorMode", DEFAULTS.colorMode.name()), FpsColorMode.class, DEFAULTS.colorMode);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.textShadow = GsonHelper.getAsBoolean(json, "textShadow", DEFAULTS.textShadow);
        settings.smoothing = GsonHelper.getAsBoolean(json, "smoothing", DEFAULTS.smoothing);
        settings.paddingX = clamp(GsonHelper.getAsInt(json, "paddingX", DEFAULTS.paddingX), 0, 16);
        settings.paddingY = clamp(GsonHelper.getAsInt(json, "paddingY", DEFAULTS.paddingY), 0, 12);
        settings.lowThreshold = clamp(GsonHelper.getAsInt(json, "lowThreshold", DEFAULTS.lowThreshold), 1, 240);
        settings.midThreshold = clamp(GsonHelper.getAsInt(json, "midThreshold", DEFAULTS.midThreshold), 1, 240);
        settings.highThreshold = clamp(GsonHelper.getAsInt(json, "highThreshold", DEFAULTS.highThreshold), 1, 240);
        settings.showOutline = GsonHelper.getAsBoolean(json, "showOutline", DEFAULTS.showOutline);
        settings.boldNumbers = GsonHelper.getAsBoolean(json, "boldNumbers", DEFAULTS.boldNumbers);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("format", current.format.name());
        json.addProperty("colorMode", current.colorMode.name());
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("textShadow", current.textShadow);
        json.addProperty("smoothing", current.smoothing);
        json.addProperty("paddingX", current.paddingX);
        json.addProperty("paddingY", current.paddingY);
        json.addProperty("lowThreshold", current.lowThreshold);
        json.addProperty("midThreshold", current.midThreshold);
        json.addProperty("highThreshold", current.highThreshold);
        json.addProperty("showOutline", current.showOutline);
        json.addProperty("boldNumbers", current.boldNumbers);
        root.add("fpsCounterSettings", json);
    }

    public static void reset(boolean persist) {
        current = copy(DEFAULTS);
        FpsSmoother.reset();
        if (persist) {
            FlowModConfig.save();
        }
    }

    public void persist() {
        FlowModConfig.save();
    }

    public void resetField(String fieldId) {
        switch (fieldId) {
            case "format" -> this.format = DEFAULTS.format;
            case "colorMode" -> this.colorMode = DEFAULTS.colorMode;
            case "showBackground" -> this.showBackground = DEFAULTS.showBackground;
            case "backgroundOpacity" -> this.backgroundOpacity = DEFAULTS.backgroundOpacity;
            case "textShadow" -> this.textShadow = DEFAULTS.textShadow;
            case "smoothing" -> this.smoothing = DEFAULTS.smoothing;
            case "paddingX" -> this.paddingX = DEFAULTS.paddingX;
            case "paddingY" -> this.paddingY = DEFAULTS.paddingY;
            case "lowThreshold" -> this.lowThreshold = DEFAULTS.lowThreshold;
            case "midThreshold" -> this.midThreshold = DEFAULTS.midThreshold;
            case "highThreshold" -> this.highThreshold = DEFAULTS.highThreshold;
            case "showOutline" -> this.showOutline = DEFAULTS.showOutline;
            case "boldNumbers" -> this.boldNumbers = DEFAULTS.boldNumbers;
            default -> {
            }
        }
    }

    public FpsDisplayFormat format() { return this.format; }
    public FpsColorMode colorMode() { return this.colorMode; }
    public boolean showBackground() { return this.showBackground; }
    public int backgroundOpacity() { return this.backgroundOpacity; }
    public boolean textShadow() { return this.textShadow; }
    public boolean smoothing() { return this.smoothing; }
    public int paddingX() { return this.paddingX; }
    public int paddingY() { return this.paddingY; }
    public int lowThreshold() { return this.lowThreshold; }
    public int midThreshold() { return this.midThreshold; }
    public int highThreshold() { return this.highThreshold; }
    public boolean showOutline() { return this.showOutline; }
    public boolean boldNumbers() { return this.boldNumbers; }

    public void setFormat(FpsDisplayFormat format) { this.format = format; }
    public void setColorMode(FpsColorMode colorMode) { this.colorMode = colorMode; }
    public void setShowBackground(boolean showBackground) { this.showBackground = showBackground; }
    public void setBackgroundOpacity(int backgroundOpacity) { this.backgroundOpacity = clamp(backgroundOpacity, 0, 100); }
    public void setTextShadow(boolean textShadow) { this.textShadow = textShadow; }
    public void setSmoothing(boolean smoothing) { this.smoothing = smoothing; }
    public void setPaddingX(int paddingX) { this.paddingX = clamp(paddingX, 0, 16); }
    public void setPaddingY(int paddingY) { this.paddingY = clamp(paddingY, 0, 12); }
    public void setLowThreshold(int lowThreshold) { this.lowThreshold = clamp(lowThreshold, 1, 240); }
    public void setMidThreshold(int midThreshold) { this.midThreshold = clamp(midThreshold, 1, 240); }
    public void setHighThreshold(int highThreshold) { this.highThreshold = clamp(highThreshold, 1, 240); }
    public void setShowOutline(boolean showOutline) { this.showOutline = showOutline; }
    public void setBoldNumbers(boolean boldNumbers) { this.boldNumbers = boldNumbers; }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static FpsCounterSettings copy(FpsCounterSettings source) {
        FpsCounterSettings copy = new FpsCounterSettings();
        copy.format = source.format;
        copy.colorMode = source.colorMode;
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.textShadow = source.textShadow;
        copy.smoothing = source.smoothing;
        copy.paddingX = source.paddingX;
        copy.paddingY = source.paddingY;
        copy.lowThreshold = source.lowThreshold;
        copy.midThreshold = source.midThreshold;
        copy.highThreshold = source.highThreshold;
        copy.showOutline = source.showOutline;
        copy.boldNumbers = source.boldNumbers;
        return copy;
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
