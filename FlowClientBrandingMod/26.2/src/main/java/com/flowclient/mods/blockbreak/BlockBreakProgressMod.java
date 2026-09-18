package com.flowclient.mods.blockbreak;

public final class BlockBreakProgressMod {
    private static boolean enabled;

    private BlockBreakProgressMod() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (persist) {
            com.flowclient.mods.FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled, true);
    }
}
