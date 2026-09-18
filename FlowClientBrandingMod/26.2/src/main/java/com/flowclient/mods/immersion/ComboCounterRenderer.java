package com.flowclient.mods.immersion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class ComboCounterRenderer {
    private ComboCounterRenderer() {
    }

    public static void render(Minecraft client, GuiGraphicsExtractor graphics, Font font) {
        if (!ComboCounterMod.isEnabled() || client.player == null) {
            return;
        }

        int combo = ComboCounterTracker.combo();
        if (combo < 2) {
            return;
        }

        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = client.getWindow().getGuiScaledHeight() / 2 + 18;
        float pulse = ComboCounterTracker.pulse();
        int scale = combo >= 10 ? 2 : combo >= 5 ? 1 : 0;
        String text = combo + "x COMBO";
        int color = (0xFF << 24)
                | (combo >= 10 ? 0xFF5555 : combo >= 5 ? 0xFFAA55 : 0xFFFFFF);
        int glow = ((int) (pulse * 120.0F) << 24) | (color & 0x00FFFFFF);

        graphics.pose().pushMatrix();
        float s = 1.0F + pulse * 0.25F + scale * 0.15F;
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(s, s);
        graphics.pose().translate(-centerX, -centerY);

        int x = centerX - font.width(text) / 2;
        graphics.text(font, Component.literal(text), x + 1, centerY + 1, glow, true);
        graphics.text(font, Component.literal(text), x, centerY, color, true);
        graphics.pose().popMatrix();
    }
}
