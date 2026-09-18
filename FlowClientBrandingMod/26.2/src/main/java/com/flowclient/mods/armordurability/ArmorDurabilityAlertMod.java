package com.flowclient.mods.armordurability;

public final class ArmorDurabilityAlertMod {
    private static boolean enabled;

    private ArmorDurabilityAlertMod() {
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
