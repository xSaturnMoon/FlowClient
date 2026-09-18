package com.flowclient.mods.render;

public final class CrosshairMod {
    private static boolean enabled;

    private CrosshairMod() {}

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
