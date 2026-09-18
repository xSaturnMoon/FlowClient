package com.flowclient.mods.render;

public enum ScoreboardAnchor {
    RIGHT("Right Edge"),
    LEFT("Left Edge"),
    CUSTOM("Custom");

    private final String label;

    ScoreboardAnchor(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public ScoreboardAnchor next() {
        ScoreboardAnchor[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
