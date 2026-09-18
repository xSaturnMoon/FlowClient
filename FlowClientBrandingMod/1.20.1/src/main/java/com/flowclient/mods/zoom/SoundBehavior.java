package com.flowclient.mods.zoom;

public enum SoundBehavior {
    MATCH_OVERLAY("Match Overlay"),
    ALWAYS("Always"),
    NEVER("Never");

    private final String label;

    SoundBehavior(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public SoundBehavior next() {
        SoundBehavior[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
