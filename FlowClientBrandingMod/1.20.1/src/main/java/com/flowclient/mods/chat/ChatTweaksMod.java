package com.flowclient.mods.chat;

public final class ChatTweaksMod {
    private static boolean enabled;

    private ChatTweaksMod() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        setEnabled(value, true);
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (!enabled) {
            ChatTweaksController.onDisabled();
        }
        if (persist) {
            com.flowclient.mods.FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }
}
