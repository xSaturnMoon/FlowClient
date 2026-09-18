package com.flowclient.mods.environment;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class TimeChangerSettings {
    private static final TimeChangerSettings DEFAULTS = new TimeChangerSettings();

    private TimeMode mode = TimeMode.PRESET;
    private boolean lockCycle = true;
    private TimePreset preset = TimePreset.NOON;
    private long customTime = 6000L;
    private long lockedTime = 6000L;

    private static TimeChangerSettings current = copy(DEFAULTS);

    private TimeChangerSettings() {
    }

    public static TimeChangerSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("timeChangerSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "timeChangerSettings");
        TimeChangerSettings settings = copy(DEFAULTS);
        settings.mode = parseEnum(
                GsonHelper.getAsString(json, "mode", DEFAULTS.mode.name()),
                TimeMode.class,
                DEFAULTS.mode
        );
        settings.lockCycle = GsonHelper.getAsBoolean(json, "lockCycle", DEFAULTS.lockCycle);
        settings.preset = parseEnum(
                GsonHelper.getAsString(json, "preset", DEFAULTS.preset.name()),
                TimePreset.class,
                DEFAULTS.preset
        );
        settings.customTime = GsonHelper.getAsLong(json, "customTime", DEFAULTS.customTime);
        settings.lockedTime = GsonHelper.getAsLong(json, "lockedTime", DEFAULTS.lockedTime);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("mode", current.mode.name());
        json.addProperty("lockCycle", current.lockCycle);
        json.addProperty("preset", current.preset.name());
        json.addProperty("customTime", current.customTime);
        json.addProperty("lockedTime", current.lockedTime);
        root.add("timeChangerSettings", json);
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
            case "lockCycle" -> this.lockCycle = DEFAULTS.lockCycle;
            case "preset" -> this.preset = DEFAULTS.preset;
            case "customTime" -> this.customTime = DEFAULTS.customTime;
            case "lockedTime" -> this.lockedTime = DEFAULTS.lockedTime;
            default -> {
            }
        }
    }

    public TimeMode mode() {
        return this.mode;
    }

    public boolean lockCycle() {
        return this.lockCycle;
    }

    public TimePreset preset() {
        return this.preset;
    }

    public long customTime() {
        return this.customTime;
    }

    public long lockedTime() {
        return this.lockedTime;
    }

    public void setMode(TimeMode mode) {
        this.mode = mode;
        if (this.lockCycle && mode == TimeMode.REAL_TIME) {
            this.lockedTime = TimeChangerLogic.computeRealTimeTicks();
        }
    }

    public void setLockCycle(boolean lockCycle) {
        this.lockCycle = lockCycle;
        if (lockCycle && this.mode == TimeMode.REAL_TIME) {
            this.lockedTime = TimeChangerLogic.computeRealTimeTicks();
        }
    }

    public void setPreset(TimePreset preset) {
        this.preset = preset;
    }

    public void setCustomTime(long customTime) {
        this.customTime = Math.floorMod(customTime, 24000L);
    }

    public void setLockedTime(long lockedTime) {
        this.lockedTime = Math.floorMod(lockedTime, 24000L);
    }

    private static TimeChangerSettings copy(TimeChangerSettings source) {
        TimeChangerSettings copy = new TimeChangerSettings();
        copy.mode = source.mode;
        copy.lockCycle = source.lockCycle;
        copy.preset = source.preset;
        copy.customTime = source.customTime;
        copy.lockedTime = source.lockedTime;
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
