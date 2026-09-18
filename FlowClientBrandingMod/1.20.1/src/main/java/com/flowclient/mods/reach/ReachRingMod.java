package com.flowclient.mods.reach;

import com.flowclient.mods.FlowModConfig;

public final class ReachRingMod {
    private static boolean enabled = false;

    private ReachRingMod() {
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
