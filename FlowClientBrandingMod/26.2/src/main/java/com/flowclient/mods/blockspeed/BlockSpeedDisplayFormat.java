package com.flowclient.mods.blockspeed;

public enum BlockSpeedDisplayFormat {
    BPS_ONLY("5.6"),
    BPS_SUFFIX("5.6 b/s"),
    BPS_PREFIX("BPS 5.6"),
    BLOCKS_ONLY("6 blocks/s");

    private final String example;

    BlockSpeedDisplayFormat(String example) {
        this.example = example;
    }

    public String example() {
        return example;
    }
}
