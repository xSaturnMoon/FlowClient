package com.flowclient.mods.blockspeed;

public final class BlockSpeedMod {
    private static boolean enabled;

    private BlockSpeedMod() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        setEnabled(value, true);
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (!enabled) {
            BlockSpeedTracker.reset();
        }
        if (persist) {
            com.flowclient.mods.FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }
}
