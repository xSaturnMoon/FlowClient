package com.flowclient.mods.battery;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class BatteryHudSettings {
    private static final BatteryHudSettings DEFAULTS = new BatteryHudSettings();

    private boolean showSystemBattery = false;
    private boolean showConnectedDevices = true;
    private boolean showProgressBar = true;
    private boolean showDeviceNames = true;
    private boolean showBackground = true;
    private int backgroundOpacity = 70;
    private boolean textShadow = true;
    private int paddingX = 6;
    private int paddingY = 3;
    private int lowThreshold = 20;
    private int midThreshold = 50;

    private static BatteryHudSettings current = copy(DEFAULTS);

    private BatteryHudSettings() {
    }

    public static BatteryHudSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("batteryHudSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "batteryHudSettings");
        BatteryHudSettings settings = copy(DEFAULTS);
        settings.showSystemBattery = GsonHelper.getAsBoolean(json, "showSystemBattery", DEFAULTS.showSystemBattery);
        settings.showConnectedDevices = GsonHelper.getAsBoolean(json, "showConnectedDevices", DEFAULTS.showConnectedDevices);
        settings.showProgressBar = GsonHelper.getAsBoolean(json, "showProgressBar", DEFAULTS.showProgressBar);
        settings.showDeviceNames = GsonHelper.getAsBoolean(json, "showDeviceNames", DEFAULTS.showDeviceNames);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.textShadow = GsonHelper.getAsBoolean(json, "textShadow", DEFAULTS.textShadow);
        settings.paddingX = clamp(GsonHelper.getAsInt(json, "paddingX", DEFAULTS.paddingX), 0, 16);
        settings.paddingY = clamp(GsonHelper.getAsInt(json, "paddingY", DEFAULTS.paddingY), 0, 12);
        settings.lowThreshold = clamp(GsonHelper.getAsInt(json, "lowThreshold", DEFAULTS.lowThreshold), 5, 40);
        settings.midThreshold = clamp(GsonHelper.getAsInt(json, "midThreshold", DEFAULTS.midThreshold), 20, 80);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("showSystemBattery", current.showSystemBattery);
        json.addProperty("showConnectedDevices", current.showConnectedDevices);
        json.addProperty("showProgressBar", current.showProgressBar);
        json.addProperty("showDeviceNames", current.showDeviceNames);
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("textShadow", current.textShadow);
        json.addProperty("paddingX", current.paddingX);
        json.addProperty("paddingY", current.paddingY);
        json.addProperty("lowThreshold", current.lowThreshold);
        json.addProperty("midThreshold", current.midThreshold);
        root.add("batteryHudSettings", json);
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

    public boolean showSystemBattery() {
        return showSystemBattery;
    }

    public boolean showConnectedDevices() {
        return showConnectedDevices;
    }

    public boolean showProgressBar() {
        return showProgressBar;
    }

    public boolean showDeviceNames() {
        return showDeviceNames;
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

    public int paddingX() {
        return paddingX;
    }

    public int paddingY() {
        return paddingY;
    }

    public int lowThreshold() {
        return lowThreshold;
    }

    public int midThreshold() {
        return midThreshold;
    }

    public void toggleShowSystemBattery() {
        showSystemBattery = !showSystemBattery;
    }

    public void toggleShowConnectedDevices() {
        showConnectedDevices = !showConnectedDevices;
    }

    public void toggleShowProgressBar() {
        showProgressBar = !showProgressBar;
    }

    public void toggleShowDeviceNames() {
        showDeviceNames = !showDeviceNames;
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

    public void cyclePaddingX() {
        paddingX = cycle(paddingX, 0, 16, 1);
    }

    public void cyclePaddingY() {
        paddingY = cycle(paddingY, 0, 12, 1);
    }

    public void cycleLowThreshold() {
        lowThreshold = cycle(lowThreshold, 5, 40, 5);
    }

    public void cycleMidThreshold() {
        midThreshold = cycle(midThreshold, 20, 80, 5);
    }

    public void resetShowSystemBattery() {
        showSystemBattery = DEFAULTS.showSystemBattery;
    }

    public void resetShowConnectedDevices() {
        showConnectedDevices = DEFAULTS.showConnectedDevices;
    }

    public void resetShowProgressBar() {
        showProgressBar = DEFAULTS.showProgressBar;
    }

    public void resetShowDeviceNames() {
        showDeviceNames = DEFAULTS.showDeviceNames;
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

    public void resetPaddingX() {
        paddingX = DEFAULTS.paddingX;
    }

    public void resetPaddingY() {
        paddingY = DEFAULTS.paddingY;
    }

    public void resetLowThreshold() {
        lowThreshold = DEFAULTS.lowThreshold;
    }

    public void resetMidThreshold() {
        midThreshold = DEFAULTS.midThreshold;
    }

    private static int cycle(int value, int min, int max, int step) {
        int next = value + step;
        return next > max ? min : next;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static BatteryHudSettings copy(BatteryHudSettings source) {
        BatteryHudSettings copy = new BatteryHudSettings();
        copy.showSystemBattery = source.showSystemBattery;
        copy.showConnectedDevices = source.showConnectedDevices;
        copy.showProgressBar = source.showProgressBar;
        copy.showDeviceNames = source.showDeviceNames;
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.textShadow = source.textShadow;
        copy.paddingX = source.paddingX;
        copy.paddingY = source.paddingY;
        copy.lowThreshold = source.lowThreshold;
        copy.midThreshold = source.midThreshold;
        return copy;
    }
}
