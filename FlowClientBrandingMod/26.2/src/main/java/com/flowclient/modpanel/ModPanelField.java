package com.flowclient.modpanel;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class ModPanelField extends EditBox {
    public static final int PAD_X = 6;
    public static final int PAD_Y = 4;

    private final int outerX;
    private final int outerY;
    private final int outerWidth;
    private final int outerHeight;

    public ModPanelField(Font font, int x, int y, int width, int height, Component hint) {
        super(
                font,
                x + PAD_X,
                y + PAD_Y,
                width - PAD_X * 2,
                height - PAD_Y * 2,
                hint
        );
        this.outerX = x;
        this.outerY = y;
        this.outerWidth = width;
        this.outerHeight = height;
        this.setBordered(false);
        this.setTextColor(ModPanelTheme.TEXT);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ModPanelTheme.fillBordered(
                graphics,
                this.outerX,
                this.outerY,
                this.outerWidth,
                this.outerHeight,
                ModPanelTheme.BG_INPUT
        );
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
