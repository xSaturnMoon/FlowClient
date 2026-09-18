package com.flowclient.mods.render;

public enum ScoreboardColor {
    WHITE("White", 0xFFFFFFFF),
    BLACK("Black", 0xFF000000),
    RED("Red", 0xFFFF5555),
    GREEN("Green", 0xFF55FF55),
    BLUE("Blue", 0xFF5555FF),
    YELLOW("Yellow", 0xFFFFFF55),
    AQUA("Aqua", 0xFF55FFFF),
    GOLD("Gold", 0xFFFFAA00),
    PINK("Pink", 0xFFFF55FF),
    ACCENT("Flow Accent", 0xFF4A9EE0);

    private final String label;
    private final int argb;

    ScoreboardColor(String label, int argb) {
        this.label = label;
        this.argb = argb;
    }

    public String label() {
        return this.label;
    }

    public int argb() {
        return this.argb;
    }

    public ScoreboardColor next() {
        ScoreboardColor[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
