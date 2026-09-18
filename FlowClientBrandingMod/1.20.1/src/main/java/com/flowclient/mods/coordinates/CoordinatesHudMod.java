package com.flowclient.mods.coordinates;

import com.flowclient.mods.FlowModConfig;

public final class CoordinatesHudMod {
    private static boolean enabled = false;

    private CoordinatesHudMod() {
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
