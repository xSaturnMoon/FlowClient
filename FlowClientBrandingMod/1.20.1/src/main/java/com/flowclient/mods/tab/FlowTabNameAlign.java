package com.flowclient.mods.tab;

public enum FlowTabNameAlign {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right");

    private final String label;

    FlowTabNameAlign(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public FlowTabNameAlign next() {
        FlowTabNameAlign[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
