package com.flowclient.mods.immersion;

import com.flowclient.mods.FlowModConfig;

public final class TrajectoryArcMod {
    private static boolean enabled = false;

    private TrajectoryArcMod() {
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
