package com.flowclient.mods.render;

import com.flowclient.mods.FlowModConfig;

public class MotionBlurMod {
    private static boolean enabled = false;
    private static float blurAmount = 0.5f;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (persist) {
            FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled, true);
    }

    public static float getBlurAmount() {
        return blurAmount;
    }

    public static void setBlurAmount(float amount) {
        setBlurAmount(amount, true);
    }

    public static void setBlurAmount(float amount, boolean persist) {
        blurAmount = amount;
        if (persist) {
            FlowModConfig.save();
        }
    }
}
