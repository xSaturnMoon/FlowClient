package com.flowclient.mods.render;

public enum BlockOverlayStyle {
    CLASSIC("Classic"),
    GLOW("Glow"),
    NEON("Neon"),
    CORNERS("Corners"),
    BOLD("Bold");

    private final String label;

    BlockOverlayStyle(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public BlockOverlayStyle next() {
        BlockOverlayStyle[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
