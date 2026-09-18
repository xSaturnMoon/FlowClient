package com.flowclient.mods.immersion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class ImmersionHudRenderer {
    private ImmersionHudRenderer() {
    }

    public static void render(Minecraft client, GuiGraphics graphics, Font font) {
        PortalFxRenderer.render(client, graphics, font);
        ComboCounterRenderer.render(client, graphics, font);
    }
}
