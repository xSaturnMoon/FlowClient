package com.flowclient.mods.armor;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class ArmorHudSettings {
    private static final ArmorHudSettings DEFAULTS = new ArmorHudSettings();

    private boolean showBackground = true;
    private int backgroundOpacity = 70;
    private boolean textShadow = true;
    private boolean showOutline = false;
    private int paddingX = 4;
    private int paddingY = 3;
    private boolean showMainHand = true;
    private boolean showHelmet = true;
    private boolean showChestplate = true;
    private boolean showLeggings = true;
    private boolean showBoots = true;
    private boolean showDurability = true;
    private ArmorHudDurabilityFormat durabilityFormat = ArmorHudDurabilityFormat.REMAINING;

    private static ArmorHudSettings current = copy(DEFAULTS);

    private ArmorHudSettings() {
    }

    public static ArmorHudSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("armorHudSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "armorHudSettings");
        ArmorHudSettings settings = copy(DEFAULTS);
        settings.showBackground = GsonHelper.getAsBoolean(json, "showBackground", DEFAULTS.showBackground);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.textShadow = GsonHelper.getAsBoolean(json, "textShadow", DEFAULTS.textShadow);
        settings.showOutline = GsonHelper.getAsBoolean(json, "showOutline", DEFAULTS.showOutline);
        settings.paddingX = clamp(GsonHelper.getAsInt(json, "paddingX", DEFAULTS.paddingX), 0, 16);
        settings.paddingY = clamp(GsonHelper.getAsInt(json, "paddingY", DEFAULTS.paddingY), 0, 12);
        settings.showMainHand = GsonHelper.getAsBoolean(json, "showMainHand", DEFAULTS.showMainHand);
        settings.showHelmet = GsonHelper.getAsBoolean(json, "showHelmet", DEFAULTS.showHelmet);
        settings.showChestplate = GsonHelper.getAsBoolean(json, "showChestplate", DEFAULTS.showChestplate);
        settings.showLeggings = GsonHelper.getAsBoolean(json, "showLeggings", DEFAULTS.showLeggings);
        settings.showBoots = GsonHelper.getAsBoolean(json, "showBoots", DEFAULTS.showBoots);
        settings.showDurability = GsonHelper.getAsBoolean(json, "showDurability", DEFAULTS.showDurability);
        settings.durabilityFormat = parseEnum(
                GsonHelper.getAsString(json, "durabilityFormat", DEFAULTS.durabilityFormat.name()),
                ArmorHudDurabilityFormat.class,
                DEFAULTS.durabilityFormat
        );
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("showBackground", current.showBackground);
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("textShadow", current.textShadow);
        json.addProperty("showOutline", current.showOutline);
        json.addProperty("paddingX", current.paddingX);
        json.addProperty("paddingY", current.paddingY);
        json.addProperty("showMainHand", current.showMainHand);
        json.addProperty("showHelmet", current.showHelmet);
        json.addProperty("showChestplate", current.showChestplate);
        json.addProperty("showLeggings", current.showLeggings);
        json.addProperty("showBoots", current.showBoots);
        json.addProperty("showDurability", current.showDurability);
        json.addProperty("durabilityFormat", current.durabilityFormat.name());
        root.add("armorHudSettings", json);
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
            case "showBackground" -> this.showBackground = DEFAULTS.showBackground;
            case "backgroundOpacity" -> this.backgroundOpacity = DEFAULTS.backgroundOpacity;
            case "textShadow" -> this.textShadow = DEFAULTS.textShadow;
            case "showOutline" -> this.showOutline = DEFAULTS.showOutline;
            case "paddingX" -> this.paddingX = DEFAULTS.paddingX;
            case "paddingY" -> this.paddingY = DEFAULTS.paddingY;
            case "showMainHand" -> this.showMainHand = DEFAULTS.showMainHand;
            case "showHelmet" -> this.showHelmet = DEFAULTS.showHelmet;
            case "showChestplate" -> this.showChestplate = DEFAULTS.showChestplate;
            case "showLeggings" -> this.showLeggings = DEFAULTS.showLeggings;
            case "showBoots" -> this.showBoots = DEFAULTS.showBoots;
            case "showDurability" -> this.showDurability = DEFAULTS.showDurability;
            case "durabilityFormat" -> this.durabilityFormat = DEFAULTS.durabilityFormat;
            default -> {
            }
        }
    }

    public boolean showBackground() { return this.showBackground; }
    public int backgroundOpacity() { return this.backgroundOpacity; }
    public boolean textShadow() { return this.textShadow; }
    public boolean showOutline() { return this.showOutline; }
    public int paddingX() { return this.paddingX; }
    public int paddingY() { return this.paddingY; }
    public boolean showMainHand() { return this.showMainHand; }
    public boolean showHelmet() { return this.showHelmet; }
    public boolean showChestplate() { return this.showChestplate; }
    public boolean showLeggings() { return this.showLeggings; }
    public boolean showBoots() { return this.showBoots; }
    public boolean showDurability() { return this.showDurability; }
    public ArmorHudDurabilityFormat durabilityFormat() { return this.durabilityFormat; }

    public void setShowBackground(boolean showBackground) { this.showBackground = showBackground; }
    public void setBackgroundOpacity(int backgroundOpacity) { this.backgroundOpacity = clamp(backgroundOpacity, 0, 100); }
    public void setTextShadow(boolean textShadow) { this.textShadow = textShadow; }
    public void setShowOutline(boolean showOutline) { this.showOutline = showOutline; }
    public void setPaddingX(int paddingX) { this.paddingX = clamp(paddingX, 0, 16); }
    public void setPaddingY(int paddingY) { this.paddingY = clamp(paddingY, 0, 12); }
    public void setShowMainHand(boolean showMainHand) { this.showMainHand = showMainHand; }
    public void setShowHelmet(boolean showHelmet) { this.showHelmet = showHelmet; }
    public void setShowChestplate(boolean showChestplate) { this.showChestplate = showChestplate; }
    public void setShowLeggings(boolean showLeggings) { this.showLeggings = showLeggings; }
    public void setShowBoots(boolean showBoots) { this.showBoots = showBoots; }
    public void setShowDurability(boolean showDurability) { this.showDurability = showDurability; }
    public void setDurabilityFormat(ArmorHudDurabilityFormat durabilityFormat) { this.durabilityFormat = durabilityFormat; }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ArmorHudSettings copy(ArmorHudSettings source) {
        ArmorHudSettings copy = new ArmorHudSettings();
        copy.showBackground = source.showBackground;
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.textShadow = source.textShadow;
        copy.showOutline = source.showOutline;
        copy.paddingX = source.paddingX;
        copy.paddingY = source.paddingY;
        copy.showMainHand = source.showMainHand;
        copy.showHelmet = source.showHelmet;
        copy.showChestplate = source.showChestplate;
        copy.showLeggings = source.showLeggings;
        copy.showBoots = source.showBoots;
        copy.showDurability = source.showDurability;
        copy.durabilityFormat = source.durabilityFormat;
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
