package com.flowclient.mods.environment;

public enum WeatherMode {
    PRESET("Preset"),
    CUSTOM("Custom");

    private final String label;

    WeatherMode(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public WeatherMode next() {
        WeatherMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
