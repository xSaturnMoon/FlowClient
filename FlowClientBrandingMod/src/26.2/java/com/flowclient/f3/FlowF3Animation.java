package com.flowclient.f3;

import com.flowclient.mods.ModifyF3Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class FlowF3Animation {
    public static final int SLIDE_DISTANCE = 56;
    private static final long OPEN_DURATION_MS = 280L;
    private static final long CLOSE_DURATION_MS = 140L;

    private static float linearProgress;
    private static long lastFrameMs;

    private FlowF3Animation() {
    }

    public static void frame(Minecraft client) {
        if (!ModifyF3Mod.isEnabled()) {
            linearProgress = 0f;
            lastFrameMs = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (lastFrameMs == 0L) {
            lastFrameMs = now;
            return;
        }

        boolean opening = client.getDebugOverlay().showDebugScreen();
        long duration = opening ? OPEN_DURATION_MS : CLOSE_DURATION_MS;
        float delta = Math.min((now - lastFrameMs) / (float) duration, 0.25f);
        lastFrameMs = now;

        if (opening) {
            linearProgress = Mth.clamp(linearProgress + delta, 0f, 1f);
        } else {
            linearProgress = Mth.clamp(linearProgress - delta, 0f, 1f);
        }
    }

    public static boolean shouldRender() {
        return linearProgress > 0f;
    }

    public static float easedProgress() {
        return easeOut(linearProgress);
    }

    private static float easeOut(float progress) {
        float inverse = 1f - progress;
        return 1f - inverse * inverse * inverse;
    }
}
