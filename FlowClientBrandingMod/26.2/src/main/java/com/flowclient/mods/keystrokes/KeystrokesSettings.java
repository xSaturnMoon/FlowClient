package com.flowclient.mods.keystrokes;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class KeystrokesSettings {
    private static final KeystrokesSettings DEFAULTS = new KeystrokesSettings();

    private KeystrokesStyle style = KeystrokesStyle.MODERN;
    private KeystrokesTheme theme = KeystrokesTheme.FLOW;
    private boolean showMovementKeys = true;
    private boolean showMouseButtons = true;
    private boolean showSpace = true;
    private CpsDisplayMode cpsDisplay = CpsDisplayMode.BELOW_MOUSE;
    private int keyWidth = 26;
    private int keyHeight = 20;
    private int keyGap = 3;
    private int cornerRadius = 4;
    private boolean showBorders = true;
    private boolean textShadow = true;
    private boolean pressedGlow = true;
    private boolean showPanelBackground = true;
    private int panelPadding = 6;

    private static KeystrokesSettings current = copy(DEFAULTS);

    private KeystrokesSettings() {
    }

    public static KeystrokesSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("keystrokesSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "keystrokesSettings");
        KeystrokesSettings settings = copy(DEFAULTS);
        settings.style = parseEnum(GsonHelper.getAsString(json, "style", DEFAULTS.style.name()), KeystrokesStyle.class, DEFAULTS.style);
        settings.theme = parseEnum(GsonHelper.getAsString(json, "theme", DEFAULTS.theme.name()), KeystrokesTheme.class, DEFAULTS.theme);
        settings.showMovementKeys = GsonHelper.getAsBoolean(json, "showMovementKeys", DEFAULTS.showMovementKeys);
        settings.showMouseButtons = GsonHelper.getAsBoolean(json, "showMouseButtons", DEFAULTS.showMouseButtons);
        settings.showSpace = GsonHelper.getAsBoolean(json, "showSpace", DEFAULTS.showSpace);
        settings.cpsDisplay = parseEnum(
                GsonHelper.getAsString(json, "cpsDisplay", DEFAULTS.cpsDisplay.name()),
                CpsDisplayMode.class,
                DEFAULTS.cpsDisplay
        );
        settings.keyWidth = clamp(GsonHelper.getAsInt(json, "keyWidth", DEFAULTS.keyWidth), 18, 40);
        settings.keyHeight = clamp(GsonHelper.getAsInt(json, "keyHeight", DEFAULTS.keyHeight), 14, 30);
        settings.keyGap = clamp(GsonHelper.getAsInt(json, "keyGap", DEFAULTS.keyGap), 1, 8);
        settings.cornerRadius = clamp(GsonHelper.getAsInt(json, "cornerRadius", DEFAULTS.cornerRadius), 0, 8);
        settings.showBorders = GsonHelper.getAsBoolean(json, "showBorders", DEFAULTS.showBorders);
        settings.textShadow = GsonHelper.getAsBoolean(json, "textShadow", DEFAULTS.textShadow);
        settings.pressedGlow = GsonHelper.getAsBoolean(json, "pressedGlow", DEFAULTS.pressedGlow);
        settings.showPanelBackground = GsonHelper.getAsBoolean(json, "showPanelBackground", DEFAULTS.showPanelBackground);
        settings.panelPadding = clamp(GsonHelper.getAsInt(json, "panelPadding", DEFAULTS.panelPadding), 0, 12);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("style", current.style.name());
        json.addProperty("theme", current.theme.name());
        json.addProperty("showMovementKeys", current.showMovementKeys);
        json.addProperty("showMouseButtons", current.showMouseButtons);
        json.addProperty("showSpace", current.showSpace);
        json.addProperty("cpsDisplay", current.cpsDisplay.name());
        json.addProperty("keyWidth", current.keyWidth);
        json.addProperty("keyHeight", current.keyHeight);
        json.addProperty("keyGap", current.keyGap);
        json.addProperty("cornerRadius", current.cornerRadius);
        json.addProperty("showBorders", current.showBorders);
        json.addProperty("textShadow", current.textShadow);
        json.addProperty("pressedGlow", current.pressedGlow);
        json.addProperty("showPanelBackground", current.showPanelBackground);
        json.addProperty("panelPadding", current.panelPadding);
        root.add("keystrokesSettings", json);
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

    public void resetField(String fieldId) {
        switch (fieldId) {
            case "style" -> this.style = DEFAULTS.style;
            case "theme" -> this.theme = DEFAULTS.theme;
            case "showMovementKeys" -> this.showMovementKeys = DEFAULTS.showMovementKeys;
            case "showMouseButtons" -> this.showMouseButtons = DEFAULTS.showMouseButtons;
            case "showSpace" -> this.showSpace = DEFAULTS.showSpace;
            case "cpsDisplay" -> this.cpsDisplay = DEFAULTS.cpsDisplay;
            case "keyWidth" -> this.keyWidth = DEFAULTS.keyWidth;
            case "keyHeight" -> this.keyHeight = DEFAULTS.keyHeight;
            case "keyGap" -> this.keyGap = DEFAULTS.keyGap;
            case "cornerRadius" -> this.cornerRadius = DEFAULTS.cornerRadius;
            case "showBorders" -> this.showBorders = DEFAULTS.showBorders;
            case "textShadow" -> this.textShadow = DEFAULTS.textShadow;
            case "pressedGlow" -> this.pressedGlow = DEFAULTS.pressedGlow;
            case "showPanelBackground" -> this.showPanelBackground = DEFAULTS.showPanelBackground;
            case "panelPadding" -> this.panelPadding = DEFAULTS.panelPadding;
            default -> {
            }
        }
    }

    public KeystrokesStyle style() { return this.style; }
    public KeystrokesTheme theme() { return this.theme; }
    public boolean showMovementKeys() { return this.showMovementKeys; }
    public boolean showMouseButtons() { return this.showMouseButtons; }
    public boolean showSpace() { return this.showSpace; }
    public CpsDisplayMode cpsDisplay() { return this.cpsDisplay; }
    public int keyWidth() { return this.keyWidth; }
    public int keyHeight() { return this.keyHeight; }
    public int keyGap() { return this.keyGap; }
    public int cornerRadius() { return this.cornerRadius; }
    public boolean showBorders() { return this.showBorders; }
    public boolean textShadow() { return this.textShadow; }
    public boolean pressedGlow() { return this.pressedGlow; }
    public boolean showPanelBackground() { return this.showPanelBackground; }
    public int panelPadding() { return this.panelPadding; }

    public void setStyle(KeystrokesStyle style) { this.style = style; }
    public void setTheme(KeystrokesTheme theme) { this.theme = theme; }
    public void setShowMovementKeys(boolean showMovementKeys) { this.showMovementKeys = showMovementKeys; }
    public void setShowMouseButtons(boolean showMouseButtons) { this.showMouseButtons = showMouseButtons; }
    public void setShowSpace(boolean showSpace) { this.showSpace = showSpace; }
    public void setCpsDisplay(CpsDisplayMode cpsDisplay) { this.cpsDisplay = cpsDisplay; }
    public void setKeyWidth(int keyWidth) { this.keyWidth = clamp(keyWidth, 18, 40); }
    public void setKeyHeight(int keyHeight) { this.keyHeight = clamp(keyHeight, 14, 30); }
    public void setKeyGap(int keyGap) { this.keyGap = clamp(keyGap, 1, 8); }
    public void setCornerRadius(int cornerRadius) { this.cornerRadius = clamp(cornerRadius, 0, 8); }
    public void setShowBorders(boolean showBorders) { this.showBorders = showBorders; }
    public void setTextShadow(boolean textShadow) { this.textShadow = textShadow; }
    public void setPressedGlow(boolean pressedGlow) { this.pressedGlow = pressedGlow; }
    public void setShowPanelBackground(boolean showPanelBackground) { this.showPanelBackground = showPanelBackground; }
    public void setPanelPadding(int panelPadding) { this.panelPadding = clamp(panelPadding, 0, 12); }

    public void applyStyleDefaults() {
        switch (this.style) {
            case COMPACT -> {
                this.keyWidth = 22;
                this.keyHeight = 16;
                this.keyGap = 2;
                this.cornerRadius = 3;
                this.panelPadding = 4;
            }
            case MINIMAL -> {
                this.keyWidth = 24;
                this.keyHeight = 18;
                this.keyGap = 3;
                this.cornerRadius = 2;
                this.showPanelBackground = false;
                this.showBorders = true;
                this.pressedGlow = false;
            }
            case MODERN -> {
                this.keyWidth = 26;
                this.keyHeight = 20;
                this.keyGap = 3;
                this.cornerRadius = 4;
                this.panelPadding = 6;
                this.showPanelBackground = true;
                this.showBorders = true;
                this.pressedGlow = true;
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static KeystrokesSettings copy(KeystrokesSettings source) {
        KeystrokesSettings copy = new KeystrokesSettings();
        copy.style = source.style;
        copy.theme = source.theme;
        copy.showMovementKeys = source.showMovementKeys;
        copy.showMouseButtons = source.showMouseButtons;
        copy.showSpace = source.showSpace;
        copy.cpsDisplay = source.cpsDisplay;
        copy.keyWidth = source.keyWidth;
        copy.keyHeight = source.keyHeight;
        copy.keyGap = source.keyGap;
        copy.cornerRadius = source.cornerRadius;
        copy.showBorders = source.showBorders;
        copy.textShadow = source.textShadow;
        copy.pressedGlow = source.pressedGlow;
        copy.showPanelBackground = source.showPanelBackground;
        copy.panelPadding = source.panelPadding;
        return copy;
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
