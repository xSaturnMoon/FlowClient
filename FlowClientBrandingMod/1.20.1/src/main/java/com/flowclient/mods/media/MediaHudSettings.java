package com.flowclient.mods.media;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class MediaHudSettings {
    private static final MediaHudSettings DEFAULTS = new MediaHudSettings();

    private boolean showArtist = true;
    private boolean showProgress = true;
    private boolean hideWhenIdle = true;
    private boolean showBackground = true;
    private int backgroundOpacity = 75;
    private boolean textShadow = true;
    private int paddingX = 8;
    private int paddingY = 6;
    private int maxTextWidth = 180;

    private static MediaHudSettings current = copy(DEFAULTS);

    private MediaHudSettings() {
    }

    public static MediaHudSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("mediaHudSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "mediaHudSettings");
        MediaHudSettings settings = copy(DEFAULTS);
        settings.showArtist = GsonHelper.getAsBoolean(json, "showArtist", DEFAULTS.showArtist);
        settings.showProgress = GsonHelper.getAsBoolean(json, "showProgress", DEFAULTS.showProgress);
        settings.hideWhenIdle = GsonHelper.getAsBoolean(json, "hideWhenIdle", DEFAULTS.hideWhenIdle);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.textShadow = GsonHelper.getAsBoolean(json, "textShadow", DEFAULTS.textShadow);
        settings.paddingX = clamp(GsonHelper.getAsInt(json, "paddingX", DEFAULTS.paddingX), 0, 20);
        settings.paddingY = clamp(GsonHelper.getAsInt(json, "paddingY", DEFAULTS.paddingY), 0, 16);
        settings.maxTextWidth = clamp(GsonHelper.getAsInt(json, "maxTextWidth", DEFAULTS.maxTextWidth), 80, 320);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("showArtist", current.showArtist);
        json.addProperty("showProgress", current.showProgress);
        json.addProperty("hideWhenIdle", current.hideWhenIdle);
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("textShadow", current.textShadow);
        json.addProperty("paddingX", current.paddingX);
        json.addProperty("paddingY", current.paddingY);
        json.addProperty("maxTextWidth", current.maxTextWidth);
        root.add("mediaHudSettings", json);
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

    public boolean showArtist() {
        return showArtist;
    }

    public boolean showProgress() {
        return showProgress;
    }

    public boolean hideWhenIdle() {
        return hideWhenIdle;
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

    public int maxTextWidth() {
        return maxTextWidth;
    }

    public void toggleShowArtist() {
        showArtist = !showArtist;
    }

    public void toggleShowProgress() {
        showProgress = !showProgress;
    }

    public void toggleHideWhenIdle() {
        hideWhenIdle = !hideWhenIdle;
    }

    public void toggleShowBackground() {
        showBackground = !showBackground;
    }

    public void cycleBackgroundOpacity() {
        backgroundOpacity = backgroundOpacity >= 100 ? 30 : backgroundOpacity + 10;
    }

    public void toggleTextShadow() {
        textShadow = !textShadow;
    }

    public void setPaddingX(int value) {
        paddingX = clamp(value, 0, 20);
    }

    public void setPaddingY(int value) {
        paddingY = clamp(value, 0, 16);
    }

    public void setMaxTextWidth(int value) {
        maxTextWidth = clamp(value, 80, 320);
    }

    public void resetShowArtist() {
        showArtist = DEFAULTS.showArtist;
    }

    public void resetShowProgress() {
        showProgress = DEFAULTS.showProgress;
    }

    public void resetHideWhenIdle() {
        hideWhenIdle = DEFAULTS.hideWhenIdle;
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

    public void resetMaxTextWidth() {
        maxTextWidth = DEFAULTS.maxTextWidth;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static MediaHudSettings copy(MediaHudSettings source) {
        MediaHudSettings copy = new MediaHudSettings();
        copy.showArtist = source.showArtist;
        copy.showProgress = source.showProgress;
        copy.hideWhenIdle = source.hideWhenIdle;
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.textShadow = source.textShadow;
        copy.paddingX = source.paddingX;
        copy.paddingY = source.paddingY;
        copy.maxTextWidth = source.maxTextWidth;
        return copy;
    }
}
