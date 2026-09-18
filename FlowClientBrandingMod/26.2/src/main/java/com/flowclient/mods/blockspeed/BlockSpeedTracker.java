package com.flowclient.mods.blockspeed;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class BlockSpeedTracker {
    private static float smoothedValue;
    private static long lastSampleMs;

    private BlockSpeedTracker() {
    }

    public static void tick(Minecraft mc) {
        if (!BlockSpeedMod.isEnabled()) {
            return;
        }

        if (mc.player == null) {
            smoothedValue = 0.0f;
            lastSampleMs = 0L;
            return;
        }

        Vec3 motion = mc.player.getDeltaMovement();
        float current = horizontalBlocksPerSecond(motion);

        long now = System.currentTimeMillis();
        if (settingsSmoothing()) {
            if (lastSampleMs == 0L) {
                smoothedValue = current;
            } else {
                float alpha = Math.min(1.0f, (now - lastSampleMs) / 150.0f);
                smoothedValue += (current - smoothedValue) * alpha;
            }
        } else {
            smoothedValue = current;
        }
        lastSampleMs = now;
    }

    public static float getBlocksPerSecond() {
        return BlockSpeedMod.isEnabled() ? smoothedValue : 0.0f;
    }

    public static void reset() {
        smoothedValue = 0.0f;
        lastSampleMs = 0L;
    }

    private static float horizontalBlocksPerSecond(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        return (float) (horizontal * 20.0D);
    }

    private static boolean settingsSmoothing() {
        return BlockSpeedSettings.get().smoothing();
    }
}
