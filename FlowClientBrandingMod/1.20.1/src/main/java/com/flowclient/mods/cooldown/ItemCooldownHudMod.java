package com.flowclient.mods.cooldown;

public final class ItemCooldownHudMod {
    private static boolean enabled;

    private ItemCooldownHudMod() {
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
