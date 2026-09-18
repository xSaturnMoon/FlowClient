package com.flowclient.mods.blockspeed;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class BlockSpeedSettings {
    private static final BlockSpeedSettings DEFAULTS = new BlockSpeedSettings();

    private BlockSpeedDisplayFormat format = BlockSpeedDisplayFormat.BPS_SUFFIX;
    private BlockSpeedColorMode colorMode = BlockSpeedColorMode.ACCENT;
    private boolean smoothing = true;
    private boolean showBackground = true;
    private int backgroundOpacity = 70;
    private boolean textShadow = true;
    private int paddingX = 6;
    private int paddingY = 3;

    private static BlockSpeedSettings current = copy(DEFAULTS);

    private BlockSpeedSettings() {
    }

    public static BlockSpeedSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("blockSpeedSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "blockSpeedSettings");
        BlockSpeedSettings settings = copy(DEFAULTS);
        settings.format = parseEnum(GsonHelper.getAsString(json, "format", DEFAULTS.format.name()), BlockSpeedDisplayFormat.class, DEFAULTS.format);
        settings.colorMode = parseEnum(GsonHelper.getAsString(json, "colorMode", DEFAULTS.colorMode.name()), BlockSpeedColorMode.class, DEFAULTS.colorMode);
        settings.smoothing = GsonHelper.getAsBoolean(json, "smoothing", DEFAULTS.smoothing);
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
        json.addProperty("smoothing", current.smoothing);
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("textShadow", current.textShadow);
        json.addProperty("paddingX", current.paddingX);
        json.addProperty("paddingY", current.paddingY);
        root.add("blockSpeedSettings", json);
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

    public BlockSpeedDisplayFormat format() {
        return format;
    }

    public void cycleFormat() {
        BlockSpeedDisplayFormat[] values = BlockSpeedDisplayFormat.values();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == format) {
                index = i;
                break;
            }
        }
        format = values[(index + 1) % values.length];
    }

    public BlockSpeedColorMode colorMode() {
        return colorMode;
    }

    public void cycleColorMode() {
        BlockSpeedColorMode[] values = BlockSpeedColorMode.values();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == colorMode) {
                index = i;
                break;
            }
        }
        colorMode = values[(index + 1) % values.length];
    }

    public boolean smoothing() {
        return smoothing;
    }

    public void toggleSmoothing() {
        smoothing = !smoothing;
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
        backgroundOpacity = (backgroundOpacity + 10) % 110;
        if (backgroundOpacity < 10) {
            backgroundOpacity = 10;
        }
    }

    public boolean textShadow() {
        return textShadow;
    }

    public void toggleTextShadow() {
        textShadow = !textShadow;
    }

    public int paddingX() {
        return paddingX;
    }

    public int paddingY() {
        return paddingY;
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

    public String formatValue(float blocksPerSecond) {
        String value = String.format("%.1f", blocksPerSecond);
        return switch (format) {
            case BPS_ONLY -> value;
            case BPS_SUFFIX -> value + " b/s";
            case BPS_PREFIX -> "BPS " + value;
            case BLOCKS_ONLY -> String.format("%.0f blocks/s", blocksPerSecond);
        };
    }

    public void resetFormat() {
        format = DEFAULTS.format;
    }

    public void resetColorMode() {
        colorMode = DEFAULTS.colorMode;
    }

    public void resetSmoothing() {
        smoothing = DEFAULTS.smoothing;
    }

    public void resetShowBackground() {
        showBackground = DEFAULTS.showBackground;
    }

    public void resetBackgroundOpacity() {
        backgroundOpacity = DEFAULTS.backgroundOpacity;
    }

    public void resetTextShadow() {
        textShadow = DEFAULTS.textShadow;
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static BlockSpeedSettings copy(BlockSpeedSettings source) {
        BlockSpeedSettings copy = new BlockSpeedSettings();
        copy.format = source.format;
        copy.colorMode = source.colorMode;
        copy.smoothing = source.smoothing;
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.textShadow = source.textShadow;
        copy.paddingX = source.paddingX;
        copy.paddingY = source.paddingY;
        return copy;
    }
}
