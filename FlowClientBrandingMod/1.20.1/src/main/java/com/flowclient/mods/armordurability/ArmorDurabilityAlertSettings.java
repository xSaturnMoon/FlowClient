package com.flowclient.mods.armordurability;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class ArmorDurabilityAlertSettings {
    private static final ArmorDurabilityAlertSettings DEFAULTS = new ArmorDurabilityAlertSettings();

    private int thresholdPercent = 20;
    private boolean showBackground = true;
    private int backgroundOpacity = 75;
    private boolean textShadow = true;

    private static ArmorDurabilityAlertSettings current = copy(DEFAULTS);

    private ArmorDurabilityAlertSettings() {
    }

    public static ArmorDurabilityAlertSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("armorDurabilityAlertSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "armorDurabilityAlertSettings");
        ArmorDurabilityAlertSettings settings = copy(DEFAULTS);
        settings.thresholdPercent = clamp(GsonHelper.getAsInt(json, "thresholdPercent", DEFAULTS.thresholdPercent), 5, 50);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.textShadow = GsonHelper.getAsBoolean(json, "textShadow", DEFAULTS.textShadow);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("thresholdPercent", current.thresholdPercent);
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("textShadow", current.textShadow);
        root.add("armorDurabilityAlertSettings", json);
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

    public int thresholdPercent() {
        return thresholdPercent;
    }

    public boolean showBackground() {
        return showBackground;
    }

    public int backgroundOpacity() {
        return backgroundOpacity;
    }

    public boolean textShadow() {
        return textShadow;
    }

    public void cycleThresholdPercent() {
        thresholdPercent = cycle(thresholdPercent, 5, 50, 5);
    }

    public void toggleShowBackground() {
        showBackground = !showBackground;
    }

    public void cycleBackgroundOpacity() {
        backgroundOpacity = cycle(backgroundOpacity, 0, 100, 10);
    }

    public void toggleTextShadow() {
        textShadow = !textShadow;
    }

    public void resetThresholdPercent() {
        thresholdPercent = DEFAULTS.thresholdPercent;
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

    private static int cycle(int value, int min, int max, int step) {
        int next = value + step;
        return next > max ? min : next;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ArmorDurabilityAlertSettings copy(ArmorDurabilityAlertSettings source) {
        ArmorDurabilityAlertSettings copy = new ArmorDurabilityAlertSettings();
        copy.thresholdPercent = source.thresholdPercent;
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.textShadow = source.textShadow;
        return copy;
    }
}
