package com.flowclient.mods.blockspeed;

public enum BlockSpeedColorMode {
    DYNAMIC("Dynamic"),
    FIXED_WHITE("White"),
    FIXED_GREEN("Green"),
    ACCENT("Accent");

    private final String label;

    BlockSpeedColorMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
