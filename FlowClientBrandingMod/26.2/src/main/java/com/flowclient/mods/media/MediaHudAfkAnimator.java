package com.flowclient.mods.media;

import com.flowclient.mods.quiet.FlowQuietController;
import com.flowclient.mods.quiet.FlowQuietMod;
import com.flowclient.mods.quiet.FlowQuietSettings;
import net.minecraft.client.Minecraft;

final class MediaHudAfkAnimator {
    private static final long TRANSITION_MS = 3000L;
    private static final float TARGET_SCREEN_FRACTION = 0.75F;

    private float blend;
    private long lastFrameMs = System.currentTimeMillis();

    void tick() {
        long now = System.currentTimeMillis();
        float deltaSeconds = Math.min(0.05F, (now - this.lastFrameMs) / 1000.0F);
        this.lastFrameMs = now;

        boolean targetAfk = shouldAnimateAfk();
        float step = deltaSeconds / (TRANSITION_MS / 1000.0F);
        if (targetAfk) {
            this.blend = Math.min(1.0F, this.blend + step);
        } else {
            this.blend = Math.max(0.0F, this.blend - step);
        }
    }

    float blend() {
        return easeInOutCubic(this.blend);
    }

    boolean isAnimating() {
        return this.blend > 0.001F;
    }

    float spinMultiplier() {
        if (!shouldAnimateAfk()) {
            return 1.0F;
        }

        // Peak spin only mid-transition; normal speed once the cinematic settles.
        float boost = (float) Math.sin(Math.PI * this.blend);
        return 1.0F + boost * 3.0F;
    }

    int targetDiscSize(Minecraft minecraft) {
        int viewport = Math.min(
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight()
        );
        return Math.max(MediaHudRenderer.baseDiscSize(), Math.round(viewport * TARGET_SCREEN_FRACTION));
    }

    private static boolean shouldAnimateAfk() {
        if (!FlowQuietMod.isEnabled() || !MediaHudMod.isEnabled()) {
            return false;
        }

        FlowQuietSettings settings = FlowQuietSettings.get();
        if (!settings.isAfkEnabled()) {
            return false;
        }

        return FlowQuietController.isAfkActive();
    }

    private static float easeInOutCubic(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        if (clamped < 0.5F) {
            return 4.0F * clamped * clamped * clamped;
        }
        float shifted = -2.0F * clamped + 2.0F;
        return 1.0F - shifted * shifted * shifted / 2.0F;
    }
}
