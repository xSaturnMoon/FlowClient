package com.flowclient.mods.fps;

public enum FpsDisplayFormat {
    FPS_ONLY("144"),
    FPS_SUFFIX("144 FPS"),
    FPS_PREFIX("FPS 144"),
    FRAME_TIME("6.9 ms"),
    FPS_AND_MS("144 FPS (6.9 ms)");

    private final String label;

    FpsDisplayFormat(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public FpsDisplayFormat next() {
        FpsDisplayFormat[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
