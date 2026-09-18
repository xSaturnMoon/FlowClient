package com.flowclient.mods.render;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class DamageIndicatorSettings {
    private static final DamageIndicatorSettings DEFAULTS = new DamageIndicatorSettings();

    private boolean showHealing;
    private int durationMs = 1200;
    private int textColor = 0xFFFF5555;

    private static DamageIndicatorSettings current = copy(DEFAULTS);

    private DamageIndicatorSettings() {
    }

    public static DamageIndicatorSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("damageIndicatorSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "damageIndicatorSettings");
        DamageIndicatorSettings settings = copy(DEFAULTS);
        settings.showHealing = GsonHelper.getAsBoolean(json, "showHealing", DEFAULTS.showHealing);
        settings.durationMs = clamp(GsonHelper.getAsInt(json, "durationMs", DEFAULTS.durationMs), 500, 5000);
        settings.textColor = GsonHelper.getAsInt(json, "textColor", DEFAULTS.textColor);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("showHealing", current.showHealing);
        json.addProperty("durationMs", current.durationMs);
        json.addProperty("textColor", current.textColor);
        root.add("damageIndicatorSettings", json);
    }

    public static void reset(boolean persist) {
        current = copy(DEFAULTS);
        if (persist) {
            current.persist();
        }
    }

    public void persist() {
        FlowModConfig.save();
    }

    public boolean showHealing() {
        return showHealing;
    }

    public int durationMs() {
        return durationMs;
    }

    public int textColor() {
        return textColor;
    }

    public void setShowHealing(boolean showHealing) {
        this.showHealing = showHealing;
    }

    public void cycleDuration() {
        int[] options = {800, 1200, 1800, 2500};
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == this.durationMs) {
                index = i;
                break;
            }
        }
        this.durationMs = options[(index + 1) % options.length];
    }

    public void cycleColor() {
        int[] options = {0xFFFF5555, 0xFFFFAA00, 0xFFFFFFFF, 0xFFFF66FF};
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == this.textColor) {
                index = i;
                break;
            }
        }
        this.textColor = options[(index + 1) % options.length];
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static DamageIndicatorSettings copy(DamageIndicatorSettings source) {
        DamageIndicatorSettings copy = new DamageIndicatorSettings();
        copy.showHealing = source.showHealing;
        copy.durationMs = source.durationMs;
        copy.textColor = source.textColor;
        return copy;
    }
}
