package com.flowclient.mods.clock;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.util.GsonHelper;

public final class FlowClockSettings {
    private static final FlowClockSettings DEFAULTS = new FlowClockSettings();

    private ClockDisplayFormat format = ClockDisplayFormat.HOURS_24;
    private ClockColorMode colorMode = ClockColorMode.WHITE;
    private boolean showBackground = true;
    private int backgroundOpacity = 70;
    private boolean textShadow = true;
    private int paddingX = 6;
    private int paddingY = 3;

    private static FlowClockSettings current = copy(DEFAULTS);

    private FlowClockSettings() {
    }

    public static FlowClockSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("flowClockSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "flowClockSettings");
        FlowClockSettings settings = copy(DEFAULTS);
        settings.format = parseEnum(
                GsonHelper.getAsString(json, "format", DEFAULTS.format.name()),
                ClockDisplayFormat.class,
                DEFAULTS.format);
        settings.colorMode = parseEnum(
                GsonHelper.getAsString(json, "colorMode", DEFAULTS.colorMode.name()),
                ClockColorMode.class,
                DEFAULTS.colorMode);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.textShadow = GsonHelper.getAsBoolean(json, "textShadow", DEFAULTS.textShadow);
        settings.paddingX = clamp(GsonHelper.getAsInt(json, "paddingX", DEFAULTS.paddingX), 0, 16);
        settings.paddingY = clamp(GsonHelper.getAsInt(json, "paddingY", DEFAULTS.paddingY), 0, 12);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("format", current.format.name());
        json.addProperty("colorMode", current.colorMode.name());
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("textShadow", current.textShadow);
        json.addProperty("paddingX", current.paddingX);
        json.addProperty("paddingY", current.paddingY);
        root.add("flowClockSettings", json);
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

    public ClockDisplayFormat format() {
        return format;
    }

    public void cycleFormat() {
        ClockDisplayFormat[] values = ClockDisplayFormat.values();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == format) {
                index = i;
                break;
            }
        }
        format = values[(index + 1) % values.length];
    }

    public ClockColorMode colorMode() {
        return colorMode;
    }

    public void cycleColorMode() {
        ClockColorMode[] values = ClockColorMode.values();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == colorMode) {
                index = i;
                break;
            }
        }
        colorMode = values[(index + 1) % values.length];
    }

    public boolean showBackground() {
        return showBackground;
    }

    public void toggleShowBackground() {
        showBackground = !showBackground;
    }

    public int backgroundOpacity() {
        return backgroundOpacity;
    }

    public void cycleBackgroundOpacity() {
        backgroundOpacity = backgroundOpacity >= 100 ? 30 : backgroundOpacity + 10;
    }

    public boolean textShadow() {
        return textShadow;
    }

    public void toggleTextShadow() {
        textShadow = !textShadow;
    }

    public void resetFormat() {
        format = ClockDisplayFormat.HOURS_24;
    }

    public void resetColorMode() {
        colorMode = ClockColorMode.WHITE;
    }

    public void resetBackgroundOpacity() {
        backgroundOpacity = 70;
    }

    public void resetShowBackground() {
        showBackground = true;
    }

    public void resetTextShadow() {
        textShadow = true;
    }

    public void setPaddingX(int paddingX) {
        this.paddingX = clamp(paddingX, 0, 16);
    }

    public void setPaddingY(int paddingY) {
        this.paddingY = clamp(paddingY, 0, 12);
    }

    public void resetPaddingX() {
        paddingX = DEFAULTS.paddingX;
    }

    public void resetPaddingY() {
        paddingY = DEFAULTS.paddingY;
    }

    public int paddingX() {
        return paddingX;
    }

    public int paddingY() {
        return paddingY;
    }

    public String formatNow() {
        return formatTime(LocalDateTime.now());
    }

    public String formatTime(LocalDateTime time) {
        return switch (format) {
            case HOURS_24 -> time.format(DateTimeFormatter.ofPattern("HH:mm"));
            case HOURS_24_SECONDS -> time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            case HOURS_12 -> time.format(DateTimeFormatter.ofPattern("hh:mm a"));
            case HOURS_12_SECONDS -> time.format(DateTimeFormatter.ofPattern("hh:mm:ss a"));
            case DATE_AND_TIME -> time.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        };
    }

    public int resolveColor() {
        return switch (colorMode) {
            case WHITE -> 0xFFFFFFFF;
            case ACCENT -> 0xFF4A9EE0;
            case SOFT_GRAY -> 0xFFB8BEC8;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static <T extends Enum<T>> T parseEnum(String name, Class<T> type, T fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static FlowClockSettings copy(FlowClockSettings source) {
        FlowClockSettings copy = new FlowClockSettings();
        copy.format = source.format;
        copy.colorMode = source.colorMode;
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.textShadow = source.textShadow;
        copy.paddingX = source.paddingX;
        copy.paddingY = source.paddingY;
        return copy;
    }
}
