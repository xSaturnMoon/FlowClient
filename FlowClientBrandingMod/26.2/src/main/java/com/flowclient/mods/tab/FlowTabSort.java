package com.flowclient.mods.tab;

public enum FlowTabSort {
    VANILLA("Server Order"),
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    PING_LOW("Ping Low-High"),
    PING_HIGH("Ping High-Low");

    private final String label;

    FlowTabSort(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public FlowTabSort next() {
        FlowTabSort[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
