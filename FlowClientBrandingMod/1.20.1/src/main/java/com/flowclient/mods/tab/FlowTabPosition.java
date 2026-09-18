package com.flowclient.mods.tab;

public enum FlowTabPosition {
    TOP_CENTER("Top Center"),
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right"),
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_RIGHT("Bottom Right"),
    CENTER("Center");

    private final String label;

    FlowTabPosition(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public FlowTabPosition next() {
        FlowTabPosition[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
