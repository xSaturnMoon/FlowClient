package com.flowclient.mods.armor;

public enum ArmorHudDurabilityFormat {
    REMAINING("Remaining"),
    PERCENT("Percent");

    private final String label;

    ArmorHudDurabilityFormat(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public ArmorHudDurabilityFormat next() {
        ArmorHudDurabilityFormat[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
