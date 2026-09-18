package com.flowclient.mods.keystrokes;

public enum CpsDisplayMode {
    OFF("Off"),
    ON_BUTTONS("On Buttons"),
    BELOW_MOUSE("Below Mouse"),
    SEPARATE_ROW("Separate Row");

    private final String label;

    CpsDisplayMode(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public CpsDisplayMode next() {
        CpsDisplayMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
