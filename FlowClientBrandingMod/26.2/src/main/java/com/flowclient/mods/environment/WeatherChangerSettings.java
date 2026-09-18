package com.flowclient.mods.environment;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class WeatherChangerSettings {
    private static final WeatherChangerSettings DEFAULTS = new WeatherChangerSettings();

    private WeatherMode mode = WeatherMode.PRESET;
    private boolean lockWeather = true;
    private WeatherPreset preset = WeatherPreset.CLEAR;
    private float customRain;
    private float customThunder;
    private float lockedRain;
    private float lockedThunder;

    private static WeatherChangerSettings current = copy(DEFAULTS);

    private WeatherChangerSettings() {
    }

    public static WeatherChangerSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("weatherChangerSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "weatherChangerSettings");
        WeatherChangerSettings settings = copy(DEFAULTS);
        settings.mode = parseEnum(
                GsonHelper.getAsString(json, "mode", DEFAULTS.mode.name()),
                WeatherMode.class,
                DEFAULTS.mode
        );
        settings.lockWeather = GsonHelper.getAsBoolean(json, "lockWeather", DEFAULTS.lockWeather);
        settings.preset = parseEnum(
                GsonHelper.getAsString(json, "preset", DEFAULTS.preset.name()),
                WeatherPreset.class,
                DEFAULTS.preset
        );
        settings.customRain = GsonHelper.getAsFloat(json, "customRain", DEFAULTS.customRain);
        settings.customThunder = GsonHelper.getAsFloat(json, "customThunder", DEFAULTS.customThunder);
        settings.lockedRain = GsonHelper.getAsFloat(json, "lockedRain", DEFAULTS.lockedRain);
        settings.lockedThunder = GsonHelper.getAsFloat(json, "lockedThunder", DEFAULTS.lockedThunder);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("mode", current.mode.name());
        json.addProperty("lockWeather", current.lockWeather);
        json.addProperty("preset", current.preset.name());
        json.addProperty("customRain", current.customRain);
        json.addProperty("customThunder", current.customThunder);
        json.addProperty("lockedRain", current.lockedRain);
        json.addProperty("lockedThunder", current.lockedThunder);
        root.add("weatherChangerSettings", json);
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
            case "mode" -> this.mode = DEFAULTS.mode;
            case "lockWeather" -> this.lockWeather = DEFAULTS.lockWeather;
            case "preset" -> this.preset = DEFAULTS.preset;
            case "customRain" -> this.customRain = DEFAULTS.customRain;
            case "customThunder" -> this.customThunder = DEFAULTS.customThunder;
            case "lockedRain" -> this.lockedRain = DEFAULTS.lockedRain;
            case "lockedThunder" -> this.lockedThunder = DEFAULTS.lockedThunder;
            default -> {
            }
        }
    }

    public WeatherMode mode() {
        return this.mode;
    }

    public boolean lockWeather() {
        return this.lockWeather;
    }

    public WeatherPreset preset() {
        return this.preset;
    }

    public float customRain() {
        return this.customRain;
    }

    public float customThunder() {
        return this.customThunder;
    }

    public float lockedRain() {
        return this.lockedRain;
    }

    public float lockedThunder() {
        return this.lockedThunder;
    }

    public void setMode(WeatherMode mode) {
        this.mode = mode;
    }

    public void setLockWeather(boolean lockWeather) {
        this.lockWeather = lockWeather;
        if (lockWeather) {
            this.lockedRain = WeatherChangerLogic.resolveRain(this);
            this.lockedThunder = WeatherChangerLogic.resolveThunder(this);
        }
    }

    public void setPreset(WeatherPreset preset) {
        this.preset = preset;
        if (this.lockWeather) {
            this.lockedRain = WeatherChangerLogic.resolveRain(this);
            this.lockedThunder = WeatherChangerLogic.resolveThunder(this);
        }
    }

    public void setCustomRain(float customRain) {
        this.customRain = Math.max(0.0f, Math.min(1.0f, customRain));
        if (this.lockWeather) {
            this.lockedRain = this.customRain;
        }
    }

    public void setCustomThunder(float customThunder) {
        this.customThunder = Math.max(0.0f, Math.min(1.0f, customThunder));
        if (this.lockWeather) {
            this.lockedThunder = this.customThunder;
        }
    }

    public float resolveRainLevel() {
        if (this.lockWeather) {
            return this.lockedRain;
        }
        return WeatherChangerLogic.resolveRain(this);
    }

    public float resolveThunderLevel() {
        if (this.lockWeather) {
            return this.lockedThunder;
        }
        return WeatherChangerLogic.resolveThunder(this);
    }

    private static WeatherChangerSettings copy(WeatherChangerSettings source) {
        WeatherChangerSettings copy = new WeatherChangerSettings();
        copy.mode = source.mode;
        copy.lockWeather = source.lockWeather;
        copy.preset = source.preset;
        copy.customRain = source.customRain;
        copy.customThunder = source.customThunder;
        copy.lockedRain = source.lockedRain;
        copy.lockedThunder = source.lockedThunder;
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
