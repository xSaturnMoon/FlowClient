package com.flowclient.mods.render;

public enum CrosshairStyle {
    CROSS("Cross"),
    DOT("Dot"),
    CIRCLE("Circle"),
    GAP_CROSS("Gap Cross"),
    T_SHAPE("T-Shape"),
    SQUARE("Square"),
    DIAMOND("Diamond");

    private final String label;

    CrosshairStyle(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public CrosshairStyle next() {
        CrosshairStyle[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
