package com.flowclient.mods.schematics;

public final class AllSchematicsMod {
    private static boolean enabled;

    private AllSchematicsMod() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (!enabled) {
            PlacementManager.clear();
        }
        if (persist) {
            com.flowclient.mods.FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled, true);
    }
}
