package com.flowclient.mods.environment;

public final class WeatherChangerLogic {
    private WeatherChangerLogic() {
    }

    public static float resolveRain(WeatherChangerSettings settings) {
        return switch (settings.mode()) {
            case PRESET -> settings.preset().rain();
            case CUSTOM -> clamp(settings.customRain());
        };
    }

    public static float resolveThunder(WeatherChangerSettings settings) {
        return switch (settings.mode()) {
            case PRESET -> settings.preset().thunder();
            case CUSTOM -> clamp(settings.customThunder());
        };
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
