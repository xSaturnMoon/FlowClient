package com.flowclient.mods.chat;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class ChatTweaksSettings {
    private static final ChatTweaksSettings DEFAULTS = new ChatTweaksSettings();

    private boolean timestamps;
    private boolean hideBackground;
    private int backgroundOpacity = 50;
    private int chatScalePercent = 100;

    private static ChatTweaksSettings current = copy(DEFAULTS);

    private ChatTweaksSettings() {
    }

    public static ChatTweaksSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("chatTweaksSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "chatTweaksSettings");
        ChatTweaksSettings settings = copy(DEFAULTS);
        settings.timestamps = GsonHelper.getAsBoolean(json, "timestamps", DEFAULTS.timestamps);
        settings.hideBackground = GsonHelper.getAsBoolean(json, "hideBackground", DEFAULTS.hideBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.chatScalePercent = clamp(GsonHelper.getAsInt(json, "chatScalePercent", DEFAULTS.chatScalePercent), 50, 200);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamps", current.timestamps);
        json.addProperty("hideBackground", current.hideBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("chatScalePercent", current.chatScalePercent);
        root.add("chatTweaksSettings", json);
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

    public boolean timestamps() {
        return timestamps;
    }

    public boolean hideBackground() {
        return hideBackground;
    }

    public int backgroundOpacity() {
        return backgroundOpacity;
    }

    public int chatScalePercent() {
        return chatScalePercent;
    }

    public void setTimestamps(boolean timestamps) {
        this.timestamps = timestamps;
    }

    public void setHideBackground(boolean hideBackground) {
        this.hideBackground = hideBackground;
    }

    public void setBackgroundOpacity(int backgroundOpacity) {
        this.backgroundOpacity = clamp(backgroundOpacity, 0, 100);
    }

    public void cycleBackgroundOpacity() {
        this.backgroundOpacity = (this.backgroundOpacity + 10) % 110;
        if (this.backgroundOpacity < 10) {
            this.backgroundOpacity = 10;
        }
    }

    public void cycleChatScale() {
        int[] options = {75, 100, 125, 150};
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == this.chatScalePercent) {
                index = i;
                break;
            }
        }
        this.chatScalePercent = options[(index + 1) % options.length];
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ChatTweaksSettings copy(ChatTweaksSettings source) {
        ChatTweaksSettings copy = new ChatTweaksSettings();
        copy.timestamps = source.timestamps;
        copy.hideBackground = source.hideBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.chatScalePercent = source.chatScalePercent;
        return copy;
    }
}
