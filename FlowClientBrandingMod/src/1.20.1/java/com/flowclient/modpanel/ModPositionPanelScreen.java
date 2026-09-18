package com.flowclient.modpanel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ModPositionPanelScreen extends ModPanelScreen {
    private static final Component TITLE = Component.literal("Position");

    public ModPositionPanelScreen(Screen parent) {
        super(TITLE, parent);
    }

    @Override
    protected void init() {
        this.addBackButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int frameWidth = Math.min(this.width - 40, 320);
        int frameHeight = Math.min(this.height - 80, 220);
        int frameX = this.centerX(frameWidth);
        int frameY = 32;
        this.drawContentFrame(graphics, frameX, frameY, frameWidth, frameHeight);
    }
}
