package com.flowclient.mods.quiet;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class FlowQuietSettings {
    private static final FlowQuietSettings DEFAULTS = new FlowQuietSettings();

    private boolean unfocusedEnabled = true;
    private boolean afkEnabled = true;
    private boolean muteAudio = true;
    private int backgroundFps = 20;
    private int preservedMaxFps = 120;
    private int afkTimeoutSeconds = 60;

    private static FlowQuietSettings current = copy(DEFAULTS);

    private FlowQuietSettings() {
    }

    public static FlowQuietSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("flowQuietSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "flowQuietSettings");
        FlowQuietSettings settings = copy(DEFAULTS);
        settings.unfocusedEnabled = GsonHelper.getAsBoolean(json, "unfocusedEnabled", DEFAULTS.unfocusedEnabled);
        settings.afkEnabled = GsonHelper.getAsBoolean(json, "afkEnabled", DEFAULTS.afkEnabled);
        settings.muteAudio = GsonHelper.getAsBoolean(json, "muteAudio", DEFAULTS.muteAudio);
        settings.backgroundFps = clampFps(GsonHelper.getAsInt(json, "backgroundFps", DEFAULTS.backgroundFps));
        settings.preservedMaxFps = clampFps(GsonHelper.getAsInt(json, "preservedMaxFps", DEFAULTS.preservedMaxFps));
        settings.afkTimeoutSeconds = clampAfk(GsonHelper.getAsInt(json, "afkTimeoutSeconds", DEFAULTS.afkTimeoutSeconds));
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("unfocusedEnabled", current.unfocusedEnabled);
        json.addProperty("afkEnabled", current.afkEnabled);
        json.addProperty("muteAudio", current.muteAudio);
        json.addProperty("backgroundFps", current.backgroundFps);
        json.addProperty("preservedMaxFps", current.preservedMaxFps);
        json.addProperty("afkTimeoutSeconds", current.afkTimeoutSeconds);
        root.add("flowQuietSettings", json);
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

    public boolean isUnfocusedEnabled() {
        return unfocusedEnabled;
    }

    public void setUnfocusedEnabled(boolean unfocusedEnabled) {
        this.unfocusedEnabled = unfocusedEnabled;
    }

    public boolean isAfkEnabled() {
        return afkEnabled;
    }

    public void setAfkEnabled(boolean afkEnabled) {
        this.afkEnabled = afkEnabled;
    }

    public boolean isMuteAudio() {
        return muteAudio;
    }

    public void setMuteAudio(boolean muteAudio) {
        this.muteAudio = muteAudio;
    }

    public int getBackgroundFps() {
        return backgroundFps;
    }

    public void setBackgroundFps(int backgroundFps) {
        this.backgroundFps = clampFps(backgroundFps);
    }

    public int getPreservedMaxFps() {
        return preservedMaxFps;
    }

    public void rememberMaxFps(int maxFps) {
        if (maxFps > this.backgroundFps) {
            this.preservedMaxFps = clampFps(maxFps);
        }
    }

    public void cycleBackgroundFps() {
        int[] options = {10, 15, 20, 30, 60};
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == backgroundFps) {
                index = i;
                break;
            }
        }
        backgroundFps = options[(index + 1) % options.length];
    }

    public int getAfkTimeoutSeconds() {
        return afkTimeoutSeconds;
    }

    public void setAfkTimeoutSeconds(int afkTimeoutSeconds) {
        this.afkTimeoutSeconds = clampAfk(afkTimeoutSeconds);
    }

    public void cycleAfkTimeoutSeconds() {
        int[] options = {10, 30, 60, 120, 180, 300};
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == afkTimeoutSeconds) {
                index = i;
                break;
            }
        }
        afkTimeoutSeconds = options[(index + 1) % options.length];
    }

    private static int clampFps(int fps) {
        return Math.max(5, Math.min(260, fps));
    }

    private static int clampAfk(int seconds) {
        return Math.max(10, Math.min(900, seconds));
    }

    private static FlowQuietSettings copy(FlowQuietSettings source) {
        FlowQuietSettings copy = new FlowQuietSettings();
        copy.unfocusedEnabled = source.unfocusedEnabled;
        copy.afkEnabled = source.afkEnabled;
        copy.muteAudio = source.muteAudio;
        copy.backgroundFps = source.backgroundFps;
        copy.preservedMaxFps = source.preservedMaxFps;
        copy.afkTimeoutSeconds = source.afkTimeoutSeconds;
        return copy;
    }
}
