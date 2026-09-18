package com.flowclient.mods.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudRenderHelper {
    private HudRenderHelper() {
    }

    public static void withLayout(
            GuiGraphicsExtractor graphics,
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
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(clamped, clamped);
        pose.translate(-x, -y);
        draw.run();
        pose.popMatrix();
    }

    public static int scaledSize(int size, float scale) {
        return Math.max(1, Math.round(size * HudScale.clamp(scale)));
    }
}
