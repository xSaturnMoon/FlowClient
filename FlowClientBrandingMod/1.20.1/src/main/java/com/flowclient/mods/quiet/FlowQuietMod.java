package com.flowclient.mods.quiet;

public final class FlowQuietMod {
    private static boolean enabled = false;

    private FlowQuietMod() {
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
            FlowQuietController.forceDeactivate();
        }
        if (persist) {
            com.flowclient.mods.FlowModConfig.flushSave();
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }
}
