package com.flowclient.mods.coordinates;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;

public final class CoordinatesHudSettings {
    private static final CoordinatesHudSettings DEFAULTS = new CoordinatesHudSettings();

    private boolean showFacing = true;
    private boolean showBackground = true;
    private int backgroundOpacity = 70;
    private boolean textShadow = true;
    private int paddingX = 6;
    private int paddingY = 3;

    private static CoordinatesHudSettings current = copy(DEFAULTS);

    private CoordinatesHudSettings() {
    }

    public static CoordinatesHudSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("coordinatesHudSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "coordinatesHudSettings");
        CoordinatesHudSettings settings = copy(DEFAULTS);
        settings.showFacing = GsonHelper.getAsBoolean(json, "showFacing", DEFAULTS.showFacing);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.textShadow = GsonHelper.getAsBoolean(json, "textShadow", DEFAULTS.textShadow);
        settings.paddingX = clamp(GsonHelper.getAsInt(json, "paddingX", DEFAULTS.paddingX), 0, 16);
        settings.paddingY = clamp(GsonHelper.getAsInt(json, "paddingY", DEFAULTS.paddingY), 0, 12);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("showFacing", current.showFacing);
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("textShadow", current.textShadow);
        json.addProperty("paddingX", current.paddingX);
        json.addProperty("paddingY", current.paddingY);
        root.add("coordinatesHudSettings", json);
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

    public boolean showFacing() {
        return showFacing;
    }

    public void toggleShowFacing() {
        showFacing = !showFacing;
    }

    public static void resetShowFacing(CoordinatesHudSettings settings) {
        settings.showFacing = DEFAULTS.showFacing;
    }

    public boolean showBackground() {
        return showBackground;
    }

    public void toggleShowBackground() {
        showBackground = !showBackground;
    }

    public static void resetShowBackground(CoordinatesHudSettings settings) {
        settings.showBackground = DEFAULTS.showBackground;
    }

    public int backgroundOpacity() {
        return backgroundOpacity;
    }

    public void cycleBackgroundOpacity() {
        backgroundOpacity = backgroundOpacity >= 100 ? 30 : backgroundOpacity + 10;
    }

    public static void resetBackgroundOpacity(CoordinatesHudSettings settings) {
        settings.backgroundOpacity = DEFAULTS.backgroundOpacity;
    }

    public boolean textShadow() {
        return textShadow;
    }

    public void toggleTextShadow() {
        textShadow = !textShadow;
    }

    public static void resetTextShadow(CoordinatesHudSettings settings) {
        settings.textShadow = DEFAULTS.textShadow;
    }

    public int paddingX() {
        return paddingX;
    }

    public void cyclePaddingX() {
        paddingX = paddingX >= 12 ? 0 : paddingX + 2;
    }

    public static void resetPaddingX(CoordinatesHudSettings settings) {
        settings.paddingX = DEFAULTS.paddingX;
    }

    public int paddingY() {
        return paddingY;
    }

    public void cyclePaddingY() {
        paddingY = paddingY >= 8 ? 0 : paddingY + 1;
    }

    public static void resetPaddingY(CoordinatesHudSettings settings) {
        settings.paddingY = DEFAULTS.paddingY;
    }

    public String format(int x, int y, int z, Direction facing) {
        String text = String.format("XYZ %d %d %d", x, y, z);
        if (showFacing) {
            text += " · " + facing.getName().toUpperCase();
        }
        return text;
    }

    private static CoordinatesHudSettings copy(CoordinatesHudSettings source) {
        CoordinatesHudSettings copy = new CoordinatesHudSettings();
        copy.showFacing = source.showFacing;
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.textShadow = source.textShadow;
        copy.paddingX = source.paddingX;
        copy.paddingY = source.paddingY;
        return copy;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
