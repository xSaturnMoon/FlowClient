package com.flowclient.mods.ping;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class PingHudSettings {
    private static final PingHudSettings DEFAULTS = new PingHudSettings();

    private boolean showBackground = true;
    private int backgroundOpacity = 70;
    private boolean textShadow = true;
    private int paddingX = 6;
    private int paddingY = 3;
    private boolean dynamicColor = true;

    private static PingHudSettings current = copy(DEFAULTS);

    private PingHudSettings() {
    }

    public static PingHudSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("pingHudSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "pingHudSettings");
        PingHudSettings settings = copy(DEFAULTS);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.textShadow = GsonHelper.getAsBoolean(json, "textShadow", DEFAULTS.textShadow);
        settings.paddingX = clamp(GsonHelper.getAsInt(json, "paddingX", DEFAULTS.paddingX), 0, 16);
        settings.paddingY = clamp(GsonHelper.getAsInt(json, "paddingY", DEFAULTS.paddingY), 0, 12);
        settings.dynamicColor = GsonHelper.getAsBoolean(json, "dynamicColor", DEFAULTS.dynamicColor);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("textShadow", current.textShadow);
        json.addProperty("paddingX", current.paddingX);
        json.addProperty("paddingY", current.paddingY);
        json.addProperty("dynamicColor", current.dynamicColor);
        root.add("pingHudSettings", json);
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

    public boolean showBackground() {
        return showBackground;
    }

    public void toggleShowBackground() {
        showBackground = !showBackground;
    }

    public static void resetShowBackground(PingHudSettings settings) {
        settings.showBackground = DEFAULTS.showBackground;
    }

    public int backgroundOpacity() {
        return backgroundOpacity;
    }

    public void cycleBackgroundOpacity() {
        backgroundOpacity = backgroundOpacity >= 100 ? 30 : backgroundOpacity + 10;
    }

    public static void resetBackgroundOpacity(PingHudSettings settings) {
        settings.backgroundOpacity = DEFAULTS.backgroundOpacity;
    }

    public boolean textShadow() {
        return textShadow;
    }

    public void toggleTextShadow() {
        textShadow = !textShadow;
    }

    public static void resetTextShadow(PingHudSettings settings) {
        settings.textShadow = DEFAULTS.textShadow;
    }

    public int paddingX() {
        return paddingX;
    }

    public void cyclePaddingX() {
        paddingX = paddingX >= 12 ? 0 : paddingX + 2;
    }

    public static void resetPaddingX(PingHudSettings settings) {
        settings.paddingX = DEFAULTS.paddingX;
    }

    public int paddingY() {
        return paddingY;
    }

    public void cyclePaddingY() {
        paddingY = paddingY >= 8 ? 0 : paddingY + 1;
    }

    public static void resetPaddingY(PingHudSettings settings) {
        settings.paddingY = DEFAULTS.paddingY;
    }

    public boolean dynamicColor() {
        return dynamicColor;
    }

    public void toggleDynamicColor() {
        dynamicColor = !dynamicColor;
    }

    public static void resetDynamicColor(PingHudSettings settings) {
        settings.dynamicColor = DEFAULTS.dynamicColor;
    }

    public String format(int pingMs) {
        if (pingMs < 0) {
            return "Ping --";
        }
        return "Ping " + pingMs + "ms";
    }

    public int resolveColor(int pingMs) {
        if (!dynamicColor || pingMs < 0) {
            return 0xFFE0E6F0;
        }
        if (pingMs <= 60) {
            return 0xFF55FF55;
        }
        if (pingMs <= 120) {
            return 0xFFFFFF55;
        }
        if (pingMs <= 200) {
            return 0xFFFFAA55;
        }
        return 0xFFFF5555;
    }

    private static PingHudSettings copy(PingHudSettings source) {
        PingHudSettings copy = new PingHudSettings();
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.textShadow = source.textShadow;
        copy.paddingX = source.paddingX;
        copy.paddingY = source.paddingY;
        copy.dynamicColor = source.dynamicColor;
        return copy;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
