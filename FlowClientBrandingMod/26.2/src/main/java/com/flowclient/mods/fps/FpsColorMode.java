package com.flowclient.mods.fps;

public enum FpsColorMode {
    DYNAMIC("Dynamic"),
    FIXED_WHITE("Fixed White"),
    FIXED_GREEN("Fixed Green"),
    ACCENT("Flow Accent"),
    RAINBOW("Rainbow");

    private final String label;

    FpsColorMode(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public FpsColorMode next() {
        FpsColorMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
