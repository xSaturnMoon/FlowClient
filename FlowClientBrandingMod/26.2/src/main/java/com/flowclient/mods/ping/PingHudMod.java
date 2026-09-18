package com.flowclient.mods.ping;

import com.flowclient.mods.FlowModConfig;

public final class PingHudMod {
    private static boolean enabled = false;

    private PingHudMod() {
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
