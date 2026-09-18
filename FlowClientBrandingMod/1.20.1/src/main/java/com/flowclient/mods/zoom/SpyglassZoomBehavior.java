package com.flowclient.mods.zoom;

public enum SpyglassZoomBehavior {
    COMBINE("Combine"),
    OVERRIDE("Override"),
    IGNORE("Ignore");

    private final String label;

    SpyglassZoomBehavior(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public SpyglassZoomBehavior next() {
        SpyglassZoomBehavior[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
