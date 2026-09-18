package com.flowclient.mods.blockbreak;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class BlockBreakProgressSettings {
    private static final BlockBreakProgressSettings DEFAULTS = new BlockBreakProgressSettings();

    private int barWidth = 80;
    private boolean showPercentage = true;
    private boolean showBackground = true;
    private int backgroundOpacity = 70;

    private static BlockBreakProgressSettings current = copy(DEFAULTS);

    private BlockBreakProgressSettings() {
    }

    public static BlockBreakProgressSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("blockBreakProgressSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "blockBreakProgressSettings");
        BlockBreakProgressSettings settings = copy(DEFAULTS);
        settings.barWidth = clamp(GsonHelper.getAsInt(json, "barWidth", DEFAULTS.barWidth), 40, 160);
        settings.showPercentage = GsonHelper.getAsBoolean(json, "showPercentage", DEFAULTS.showPercentage);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("barWidth", current.barWidth);
        json.addProperty("showPercentage", current.showPercentage);
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        root.add("blockBreakProgressSettings", json);
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

    public int barWidth() {
        return barWidth;
    }

    public boolean showPercentage() {
        return showPercentage;
    }

    public boolean showBackground() {
        return showBackground;
    }

    public int backgroundOpacity() {
        return backgroundOpacity;
    }

    public void cycleBarWidth() {
        barWidth = cycle(barWidth, 40, 160, 10);
    }

    public void toggleShowPercentage() {
        showPercentage = !showPercentage;
    }

    public void toggleShowBackground() {
        showBackground = !showBackground;
    }

    public void cycleBackgroundOpacity() {
        backgroundOpacity = cycle(backgroundOpacity, 0, 100, 10);
    }

    public void resetBarWidth() {
        barWidth = DEFAULTS.barWidth;
    }

    public void resetShowPercentage() {
        showPercentage = DEFAULTS.showPercentage;
    }

    public void resetShowBackground() {
        showBackground = DEFAULTS.showBackground;
    }

    public void resetBackgroundOpacity() {
        backgroundOpacity = DEFAULTS.backgroundOpacity;
    }

    private static int cycle(int value, int min, int max, int step) {
        int next = value + step;
        return next > max ? min : next;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static BlockBreakProgressSettings copy(BlockBreakProgressSettings source) {
        BlockBreakProgressSettings copy = new BlockBreakProgressSettings();
        copy.barWidth = source.barWidth;
        copy.showPercentage = source.showPercentage;
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        return copy;
    }
}
