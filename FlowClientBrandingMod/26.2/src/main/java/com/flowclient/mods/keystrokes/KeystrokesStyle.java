package com.flowclient.mods.keystrokes;

public enum KeystrokesStyle {
    MODERN("Modern"),
    COMPACT("Compact"),
    MINIMAL("Minimal");

    private final String label;

    KeystrokesStyle(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public KeystrokesStyle next() {
        KeystrokesStyle[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
