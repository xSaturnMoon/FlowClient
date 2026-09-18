package com.flowclient.mods.advancements;

import com.flowclient.mods.FlowModConfig;

public final class AdvancementsUnlockMod {
    private static boolean enabled;

    private AdvancementsUnlockMod() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        setEnabled(value, true);
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (persist) {
            FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }
}
