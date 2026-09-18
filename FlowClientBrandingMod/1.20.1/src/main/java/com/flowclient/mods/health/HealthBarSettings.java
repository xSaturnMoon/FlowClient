package com.flowclient.mods.health;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class HealthBarSettings {
    private static final HealthBarSettings DEFAULTS = new HealthBarSettings();

    private HealthBarStyle style = HealthBarStyle.CLASSIC;
    private HealthBarTarget target = HealthBarTarget.ALL;
    private boolean hideFullHealth = true;
    private boolean showNumeric = false;
    private boolean showAbsorption = true;
    private boolean showOnSelf = false;
    private int maxDistance = 48;
    private int barWidth = 48;
    private int barHeight = 5;
    private float yOffset = 0.35F;

    private static HealthBarSettings current = copy(DEFAULTS);

    private HealthBarSettings() {
    }

    public static HealthBarSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("healthBarSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "healthBarSettings");
        HealthBarSettings settings = copy(DEFAULTS);
        settings.style = parseEnum(GsonHelper.getAsString(json, "style", DEFAULTS.style.name()), HealthBarStyle.class, DEFAULTS.style);
        settings.target = parseEnum(GsonHelper.getAsString(json, "target", DEFAULTS.target.name()), HealthBarTarget.class, DEFAULTS.target);
        settings.hideFullHealth = GsonHelper.getAsBoolean(json, "hideFullHealth", DEFAULTS.hideFullHealth);
        settings.showNumeric = GsonHelper.getAsBoolean(json, "showNumeric", DEFAULTS.showNumeric);
        settings.showAbsorption = GsonHelper.getAsBoolean(json, "showAbsorption", DEFAULTS.showAbsorption);
        settings.showOnSelf = GsonHelper.getAsBoolean(json, "showOnSelf", DEFAULTS.showOnSelf);
        settings.maxDistance = clamp(GsonHelper.getAsInt(json, "maxDistance", DEFAULTS.maxDistance), 8, 128);
        settings.barWidth = clamp(GsonHelper.getAsInt(json, "barWidth", DEFAULTS.barWidth), 24, 96);
        settings.barHeight = clamp(GsonHelper.getAsInt(json, "barHeight", DEFAULTS.barHeight), 3, 12);
        settings.yOffset = clamp(GsonHelper.getAsFloat(json, "yOffset", DEFAULTS.yOffset), 0.0F, 2.0F);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("style", current.style.name());
        json.addProperty("target", current.target.name());
        json.addProperty("hideFullHealth", current.hideFullHealth);
        json.addProperty("showNumeric", current.showNumeric);
        json.addProperty("showAbsorption", current.showAbsorption);
        json.addProperty("showOnSelf", current.showOnSelf);
        json.addProperty("maxDistance", current.maxDistance);
        json.addProperty("barWidth", current.barWidth);
        json.addProperty("barHeight", current.barHeight);
        json.addProperty("yOffset", current.yOffset);
        root.add("healthBarSettings", json);
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

    public HealthBarStyle style() {
        return style;
    }

    public void cycleStyle() {
        HealthBarStyle[] values = HealthBarStyle.values();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == style) {
                index = i;
                break;
            }
        }
        style = values[(index + 1) % values.length];
    }

    public HealthBarTarget target() {
        return target;
    }

    public void cycleTarget() {
        HealthBarTarget[] values = HealthBarTarget.values();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) {
                index = i;
                break;
            }
        }
        target = values[(index + 1) % values.length];
    }

    public boolean hideFullHealth() {
        return hideFullHealth;
    }

    public void toggleHideFullHealth() {
        hideFullHealth = !hideFullHealth;
    }

    public boolean showNumeric() {
        return showNumeric;
    }

    public void toggleShowNumeric() {
        showNumeric = !showNumeric;
    }

    public boolean showAbsorption() {
        return showAbsorption;
    }

    public void toggleShowAbsorption() {
        showAbsorption = !showAbsorption;
    }

    public boolean showOnSelf() {
        return showOnSelf;
    }

    public void toggleShowOnSelf() {
        showOnSelf = !showOnSelf;
    }

    public int maxDistance() {
        return maxDistance;
    }

    public void cycleMaxDistance() {
        int[] options = {16, 24, 32, 48, 64, 96};
        maxDistance = nextOption(maxDistance, options, 48);
    }

    public int barWidth() {
        return barWidth;
    }

    public void cycleBarWidth() {
        int[] options = {32, 40, 48, 56, 64, 72};
        barWidth = nextOption(barWidth, options, 48);
    }

    public int barHeight() {
        return barHeight;
    }

    public void cycleBarHeight() {
        int[] options = {3, 4, 5, 6, 8};
        barHeight = nextOption(barHeight, options, 5);
    }

    public float yOffset() {
        return yOffset;
    }

    public void cycleYOffset() {
        float[] options = {0.15F, 0.35F, 0.55F, 0.75F, 1.0F};
        yOffset = nextFloatOption(yOffset, options, 0.35F);
    }

    public void resetStyle() {
        style = DEFAULTS.style;
    }

    public void resetTarget() {
        target = DEFAULTS.target;
    }

    public void resetHideFullHealth() {
        hideFullHealth = DEFAULTS.hideFullHealth;
    }

    public void resetShowNumeric() {
        showNumeric = DEFAULTS.showNumeric;
    }

    public void resetShowAbsorption() {
        showAbsorption = DEFAULTS.showAbsorption;
    }

    public void resetShowOnSelf() {
        showOnSelf = DEFAULTS.showOnSelf;
    }

    public void resetMaxDistance() {
        maxDistance = DEFAULTS.maxDistance;
    }

    public void resetBarWidth() {
        barWidth = DEFAULTS.barWidth;
    }

    public void resetBarHeight() {
        barHeight = DEFAULTS.barHeight;
    }

    public void resetYOffset() {
        yOffset = DEFAULTS.yOffset;
    }

    private static int nextOption(int currentValue, int[] options, int fallback) {
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == currentValue) {
                index = i;
                break;
            }
        }
        return options[(index + 1) % options.length];
    }

    private static float nextFloatOption(float currentValue, float[] options, float fallback) {
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (Float.compare(options[i], currentValue) == 0) {
                index = i;
                break;
            }
        }
        return options[(index + 1) % options.length];
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static HealthBarSettings copy(HealthBarSettings source) {
        HealthBarSettings copy = new HealthBarSettings();
        copy.style = source.style;
        copy.target = source.target;
        copy.hideFullHealth = source.hideFullHealth;
        copy.showNumeric = source.showNumeric;
        copy.showAbsorption = source.showAbsorption;
        copy.showOnSelf = source.showOnSelf;
        copy.maxDistance = source.maxDistance;
        copy.barWidth = source.barWidth;
        copy.barHeight = source.barHeight;
        copy.yOffset = source.yOffset;
        return copy;
    }
}
