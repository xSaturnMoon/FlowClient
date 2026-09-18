package com.flowclient.mods.zoom;

public enum OverlayVisibility {
    HOLDING("Holding"),
    ALWAYS("Always"),
    NEVER("Never");

    private final String label;

    OverlayVisibility(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public OverlayVisibility next() {
        OverlayVisibility[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
