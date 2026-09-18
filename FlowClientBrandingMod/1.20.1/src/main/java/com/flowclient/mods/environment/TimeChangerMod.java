package com.flowclient.mods.environment;

public final class TimeChangerMod {
    private static boolean enabled;

    private TimeChangerMod() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

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

    public static long getTargetTime() {
        return TimeChangerLogic.resolveTime(TimeChangerSettings.get());
    }
}
