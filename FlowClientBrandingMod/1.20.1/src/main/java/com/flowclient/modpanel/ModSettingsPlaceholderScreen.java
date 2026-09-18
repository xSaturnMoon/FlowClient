package com.flowclient.modpanel;

import com.flowclient.compat.FlowGfx;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ModSettingsPlaceholderScreen extends ModPanelScreen {
    public ModSettingsPlaceholderScreen(Screen parent, String modName) {
        super(Component.literal(modName + " Settings"), parent, modName);
    }

    @Override
    protected void init() {
        this.initPanel();
    }

    @Override
    protected void initPanel() {
        this.addBackButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int frameX = this.frameX();
        int frameWidth = this.frameWidth();
        this.drawModDescription(graphics, frameX, frameWidth);
        this.drawContentFrame(graphics, frameX, this.frameY(), frameWidth, this.frameHeight());
        FlowGfx.text(graphics, 
                this.font,
                Component.literal("No configurable settings yet for this module."),
                frameX + 12,
                this.frameY() + 12,
                0xFF8E97A6,
                false
        );
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int frameWidth() {
        return Math.min(this.width - 40, 420);
    }

    private int frameHeight() {
        return 96;
    }

    private int frameX() {
        return this.centerX(this.frameWidth());
    }

    private int frameY() {
        return this.contentFrameY(24, this.frameWidth());
    }
}
