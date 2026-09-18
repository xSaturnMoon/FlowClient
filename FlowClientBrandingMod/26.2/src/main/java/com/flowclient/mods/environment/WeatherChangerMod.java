package com.flowclient.mods.environment;

public final class WeatherChangerMod {
    private static boolean enabled;

    private WeatherChangerMod() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        setEnabled(value, true);
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (persist) {
            com.flowclient.mods.FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static float getRainLevel() {
        return WeatherChangerSettings.get().resolveRainLevel();
    }

    public static float getThunderLevel() {
        return WeatherChangerSettings.get().resolveThunderLevel();
    }
}
