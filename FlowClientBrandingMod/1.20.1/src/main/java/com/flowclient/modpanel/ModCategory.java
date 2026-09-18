package com.flowclient.modpanel;

import net.minecraft.network.chat.Component;

public enum ModCategory {
    VISUAL("Visual"),
    COMBAT("Combat"),
    HUD("HUD"),
    UTILITY("Utility"),
    WORLD("World"),
    MISC("Misc");

    private final String label;

    ModCategory(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public Component displayName() {
        return Component.literal(this.label);
    }
}
