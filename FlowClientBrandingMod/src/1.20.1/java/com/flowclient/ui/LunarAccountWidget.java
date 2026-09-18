package com.flowclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class LunarAccountWidget extends AbstractWidget {
    public LunarAccountWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();

        int bgColor = hovered ? UiColors.CARD_BG_HOVER : UiColors.CARD_BG;
        UiColors.fillRounded(graphics, this.getX(), this.getY(), this.width, this.height, 8, bgColor);
        UiColors.drawRoundedOutline(graphics, this.getX(), this.getY(), this.width, this.height, 8, 1, UiColors.CARD_BORDER);

        Minecraft mc = Minecraft.getInstance();
        String username = mc.getUser() != null ? mc.getUser().getName() : "Player";

        int avatarSize = 16;
        int avatarX = this.getX() + 6;
        int avatarY = this.getY() + (this.height - avatarSize) / 2;

        UiSprites.drawIcon(graphics, UiSprites.IconType.SINGLEPLAYER, avatarX, avatarY, avatarSize, UiColors.BTN_TEXT);

        Font font = mc.font;
        Component label = UiFonts.text(username);
        int textX = avatarX + avatarSize + 6;
        int textY = this.getY() + (this.height - font.lineHeight) / 2 + 1;
        graphics.drawString(font, label, textX, textY, UiColors.BTN_TEXT);

        int arrowX = this.getX() + this.width - 12;
        int arrowY = this.getY() + this.height / 2;
        graphics.fill(arrowX - 3, arrowY - 1, arrowX + 3, arrowY, UiColors.TEXT_MUTED);
        graphics.fill(arrowX - 2, arrowY, arrowX + 2, arrowY + 1, UiColors.TEXT_MUTED);
        graphics.fill(arrowX - 1, arrowY + 1, arrowX + 1, arrowY + 2, UiColors.TEXT_MUTED);
    }

    @Override
    protected void updateNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
