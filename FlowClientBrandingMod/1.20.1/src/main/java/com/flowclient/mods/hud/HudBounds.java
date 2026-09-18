package com.flowclient.mods.hud;

import net.minecraft.client.Minecraft;

public final class HudBounds {
    private HudBounds() {
    }

    public static Bounds get(HudElement element, Minecraft mc, boolean preview) {
        int width = element.getWidth(mc, mc.font, preview);
        int height = element.getHeight(mc, mc.font, preview);

        float scale = HudLayoutManager.get().resolveScale(element);
        int x = element == HudElement.SCOREBOARD
                ? com.flowclient.mods.render.ScoreboardRenderer.resolveX(mc, mc.font, width, height, preview)
                : HudLayoutManager.get().resolveX(element, width, height);
        int y = HudLayoutManager.get().resolveY(element, width, height);
        return new Bounds(
                x,
                y,
                HudRenderHelper.scaledSize(width, scale),
                HudRenderHelper.scaledSize(height, scale)
        );
    }

    public record Bounds(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }
}
