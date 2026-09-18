package com.flowclient.mods.serveraddress;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class ServerAddressHudSettings {
    private static final ServerAddressHudSettings DEFAULTS = new ServerAddressHudSettings();

    private ServerAddressDisplayMode displayMode = ServerAddressDisplayMode.SERVER_NAME;
    private boolean hideSingleplayer = true;
    private boolean showBackground = true;
    private int backgroundOpacity = 70;
    private boolean textShadow = true;
    private int paddingX = 6;
    private int paddingY = 3;

    private static ServerAddressHudSettings current = copy(DEFAULTS);

    private ServerAddressHudSettings() {
    }

    public static ServerAddressHudSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("serverAddressHudSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "serverAddressHudSettings");
        ServerAddressHudSettings settings = copy(DEFAULTS);
        settings.displayMode = parseEnum(
                GsonHelper.getAsString(json, "displayMode", DEFAULTS.displayMode.name()),
                ServerAddressDisplayMode.class,
                DEFAULTS.displayMode
        );
        settings.hideSingleplayer = GsonHelper.getAsBoolean(json, "hideSingleplayer", DEFAULTS.hideSingleplayer);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.textShadow = GsonHelper.getAsBoolean(json, "textShadow", DEFAULTS.textShadow);
        settings.paddingX = clamp(GsonHelper.getAsInt(json, "paddingX", DEFAULTS.paddingX), 0, 16);
        settings.paddingY = clamp(GsonHelper.getAsInt(json, "paddingY", DEFAULTS.paddingY), 0, 12);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("displayMode", current.displayMode.name());
        json.addProperty("hideSingleplayer", current.hideSingleplayer);
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("textShadow", current.textShadow);
        json.addProperty("paddingX", current.paddingX);
        json.addProperty("paddingY", current.paddingY);
        root.add("serverAddressHudSettings", json);
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

    public ServerAddressDisplayMode displayMode() {
        return displayMode;
    }

    public boolean hideSingleplayer() {
        return hideSingleplayer;
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

    public void cycleDisplayMode() {
        displayMode = displayMode.next();
    }

    public void toggleHideSingleplayer() {
        hideSingleplayer = !hideSingleplayer;
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

    public void resetDisplayMode() {
        displayMode = DEFAULTS.displayMode;
    }

    public void resetHideSingleplayer() {
        hideSingleplayer = DEFAULTS.hideSingleplayer;
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

    private static int cycle(int value, int min, int max, int step) {
        int next = value + step;
        return next > max ? min : next;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static ServerAddressHudSettings copy(ServerAddressHudSettings source) {
        ServerAddressHudSettings copy = new ServerAddressHudSettings();
        copy.displayMode = source.displayMode;
        copy.hideSingleplayer = source.hideSingleplayer;
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.textShadow = source.textShadow;
        copy.paddingX = source.paddingX;
        copy.paddingY = source.paddingY;
        return copy;
    }
}
