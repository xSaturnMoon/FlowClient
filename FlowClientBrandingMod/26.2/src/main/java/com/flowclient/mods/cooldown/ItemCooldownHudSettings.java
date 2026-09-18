package com.flowclient.mods.cooldown;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class ItemCooldownHudSettings {
    private static final ItemCooldownHudSettings DEFAULTS = new ItemCooldownHudSettings();

    private boolean showAttackCooldown = true;
    private boolean showItemCooldown = true;
    private int barWidth = 64;
    private boolean showBackground = true;
    private int backgroundOpacity = 70;

    private static ItemCooldownHudSettings current = copy(DEFAULTS);

    private ItemCooldownHudSettings() {
    }

    public static ItemCooldownHudSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("itemCooldownHudSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "itemCooldownHudSettings");
        ItemCooldownHudSettings settings = copy(DEFAULTS);
        settings.showAttackCooldown = GsonHelper.getAsBoolean(json, "showAttackCooldown", DEFAULTS.showAttackCooldown);
        settings.showItemCooldown = GsonHelper.getAsBoolean(json, "showItemCooldown", DEFAULTS.showItemCooldown);
        settings.barWidth = clamp(GsonHelper.getAsInt(json, "barWidth", DEFAULTS.barWidth), 32, 120);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("showAttackCooldown", current.showAttackCooldown);
        json.addProperty("showItemCooldown", current.showItemCooldown);
        json.addProperty("barWidth", current.barWidth);
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        root.add("itemCooldownHudSettings", json);
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

    public boolean showAttackCooldown() {
        return showAttackCooldown;
    }

    public boolean showItemCooldown() {
        return showItemCooldown;
    }

    public int barWidth() {
        return barWidth;
    }

    public boolean showBackground() {
        return showBackground;
    }

    public int backgroundOpacity() {
        return backgroundOpacity;
    }

    public void toggleShowAttackCooldown() {
        showAttackCooldown = !showAttackCooldown;
    }

    public void toggleShowItemCooldown() {
        showItemCooldown = !showItemCooldown;
    }

    public void cycleBarWidth() {
        barWidth = cycle(barWidth, 32, 120, 8);
    }

    public void toggleShowBackground() {
        showBackground = !showBackground;
    }

    public void cycleBackgroundOpacity() {
        backgroundOpacity = cycle(backgroundOpacity, 0, 100, 10);
    }

    public void resetShowAttackCooldown() {
        showAttackCooldown = DEFAULTS.showAttackCooldown;
    }

    public void resetShowItemCooldown() {
        showItemCooldown = DEFAULTS.showItemCooldown;
    }

    public void resetBarWidth() {
        barWidth = DEFAULTS.barWidth;
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

    private static ItemCooldownHudSettings copy(ItemCooldownHudSettings source) {
        ItemCooldownHudSettings copy = new ItemCooldownHudSettings();
        copy.showAttackCooldown = source.showAttackCooldown;
        copy.showItemCooldown = source.showItemCooldown;
        copy.barWidth = source.barWidth;
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        return copy;
    }
}
