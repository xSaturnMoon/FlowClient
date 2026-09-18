package com.flowclient.mods.otherclient;

import com.flowclient.mods.FlowModConfig;

public class OtherClientMod {
    private static boolean enabled = false;

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        ClientDetector.clearCache();
        if (persist) FlowModConfig.save();
    }

    public static void toggle() { setEnabled(!enabled, true); }
}
