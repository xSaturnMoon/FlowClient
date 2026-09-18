package com.flowclient.mods.serveraddress;

public final class ServerAddressHudMod {
    private static boolean enabled;

    private ServerAddressHudMod() {
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
