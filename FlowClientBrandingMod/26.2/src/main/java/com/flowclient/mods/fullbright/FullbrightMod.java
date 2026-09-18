package com.flowclient.mods.fullbright;

public final class FullbrightMod {
    private static boolean enabled;

    private FullbrightMod() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        setEnabled(value, true);
    }

    public static void setEnabled(boolean value, boolean persist) {
        if (enabled == value) {
            return;
        }
        enabled = value;
        FullbrightController.sync();
        if (persist) {
            com.flowclient.mods.FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }
}
