package com.flowclient.mods.health;

public enum HealthBarStyle {
    CLASSIC("Classic"),
    GRADIENT("Gradient"),
    COMPACT("Compact"),
    SEGMENTS("Segments"),
    OUTLINED("Outlined");

    private final String label;

    HealthBarStyle(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
