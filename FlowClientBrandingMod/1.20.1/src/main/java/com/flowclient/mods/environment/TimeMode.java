package com.flowclient.mods.environment;

public enum TimeMode {
    PRESET("Preset"),
    REAL_TIME("Real Time"),
    CUSTOM("Custom");

    private final String label;

    TimeMode(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public TimeMode next() {
        TimeMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
