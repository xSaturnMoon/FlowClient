package com.flowclient.mods.hud;

import net.minecraft.client.gui.GuiGraphics;

public final class HudRenderHelper {
    private HudRenderHelper() {
    }

    public static void withLayout(
            GuiGraphics graphics,
            int x,
            int y,
            float scale,
            Runnable draw
    ) {
        float clamped = HudScale.clamp(scale);
        if (HudScale.isUnity(clamped)) {
            draw.run();
            return;
        }

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0D);
        pose.scale(clamped, clamped, 1.0F);
        pose.translate(-x, -y, 0.0D);
        draw.run();
        pose.popPose();
    }

    public static int scaledSize(int size, float scale) {
        return Math.max(1, Math.round(size * HudScale.clamp(scale)));
    }
}
