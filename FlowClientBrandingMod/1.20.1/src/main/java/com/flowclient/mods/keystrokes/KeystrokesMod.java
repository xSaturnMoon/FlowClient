package com.flowclient.mods.keystrokes;

import net.minecraft.client.Minecraft;

public final class KeystrokesMod {
    private static boolean enabled;

    private KeystrokesMod() {}

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean value) {
        setEnabled(value, true);
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (persist) {
            com.flowclient.mods.FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }
}

