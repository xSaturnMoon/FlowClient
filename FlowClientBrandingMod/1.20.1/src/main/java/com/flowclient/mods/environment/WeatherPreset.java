package com.flowclient.mods.environment;

public enum WeatherPreset {
    CLEAR("Clear", 0.0f, 0.0f),
    RAIN("Rain", 1.0f, 0.0f),
    THUNDER("Thunder", 1.0f, 1.0f),
    SNOW("Snow", 1.0f, 0.0f);

    private final String label;
    private final float rain;
    private final float thunder;

    WeatherPreset(String label, float rain, float thunder) {
        this.label = label;
        this.rain = rain;
        this.thunder = thunder;
    }

    public String label() {
        return this.label;
    }

    public float rain() {
        return this.rain;
    }

    public float thunder() {
        return this.thunder;
    }

    public WeatherPreset next() {
        WeatherPreset[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
