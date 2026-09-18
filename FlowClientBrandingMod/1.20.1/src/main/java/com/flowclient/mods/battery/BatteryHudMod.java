package com.flowclient.mods.battery;

public final class BatteryHudMod {
    private static boolean enabled;

    private BatteryHudMod() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (!enabled) {
            BatteryPoller.get().clear();
        } else {
            BatteryPoller.get().requestPoll();
        }
        if (persist) {
            com.flowclient.mods.FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled, true);
    }
}
