package com.flowclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import com.flowclient.util.FlowArgb;

public final class LunarBannerCard extends AbstractWidget {
    public LunarBannerCard(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bgTop = FlowArgb.color(240, 20, 80, 100);
        UiColors.fillRounded(graphics, this.getX(), this.getY(), this.width, this.height, 8, bgTop);

        Font font = Minecraft.getInstance().font;
        int textX = this.getX() + 10;
        int textY = this.getY() + 15;
        graphics.drawString(font, UiFonts.text("30% OFF"), textX, textY, FlowArgb.color(255, 239, 68, 68));
        graphics.drawString(font, UiFonts.text("VAULT SALE"), textX, textY + 12, FlowArgb.color(255, 255, 255, 255));

        int dotW = 20;
        int dotH = 3;
        int gap = 4;
        int totalDotsW = 4 * dotW + 3 * gap;
        int startDotX = this.getX() + (this.width - totalDotsW) / 2;
        int dotY = this.getY() + this.height - 8;

        for (int i = 0; i < 4; i++) {
            int dx = startDotX + i * (dotW + gap);
            int dotColor = (i == 0) ? FlowArgb.color(255, 255, 255, 255) : FlowArgb.color(120, 255, 255, 255);
            graphics.fill(dx, dotY, dx + dotW, dotY + dotH, dotColor);
        }
    }

    @Override
    protected void updateNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
