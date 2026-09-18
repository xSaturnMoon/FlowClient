package com.flowclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class LunarQuestCard extends AbstractWidget {
    private final LunarButton actionButton;

    public LunarQuestCard(int x, int y, int width, int height, Runnable onQuestPress) {
        super(x, y, width, height, Component.empty());
        this.actionButton = new LunarButton(
                x + 10,
                y + height - 28,
                width - 20,
                20,
                Component.literal("Complete Quest"),
                null,
                LunarButton.Style.QUEST_ACTION,
                null,
                onQuestPress
        );
    }

    public LunarButton getActionButton() {
        return this.actionButton;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiColors.fillRounded(graphics, this.getX(), this.getY(), this.width, this.height, 10, UiColors.CARD_BG);
        UiColors.drawRoundedOutline(graphics, this.getX(), this.getY(), this.width, this.height, 10, 2, UiColors.STORE_BORDER);

        int headX = this.getX() + 10;
        int headY = this.getY() + 10;
        UiSprites.drawIcon(graphics, UiSprites.IconType.GIFT, headX, headY, 14, UiColors.STORE_TEXT);

        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, UiFonts.text("Free Medal Quest"), headX + 18, headY + 2, UiColors.BTN_TEXT);
        UiSprites.drawIcon(graphics, UiSprites.IconType.HELP, this.getX() + this.width - 20, headY + 2, 10, UiColors.TEXT_MUTED);

        int slotW = 44;
        int slotH = 44;
        int slotY = headY + 20;
        int startX = this.getX() + 10;
        int gap = 8;

        for (int i = 0; i < 3; i++) {
            int sx = startX + i * (slotW + gap);
            UiColors.fillRounded(graphics, sx, slotY, slotW, slotH, 6, UiColors.CARD_BG_HOVER);

            if (i == 0) {
                UiSprites.drawIcon(graphics, UiSprites.IconType.SHIRT, sx + (slotW - 18) / 2, slotY + (slotH - 18) / 2, 18, UiColors.TEXT_MUTED);
            } else if (i == 1) {
                UiSprites.drawIcon(graphics, UiSprites.IconType.SINGLEPLAYER, sx + (slotW - 18) / 2, slotY + (slotH - 18) / 2, 18, UiColors.STORE_TEXT);
            } else {
                UiSprites.drawIcon(graphics, UiSprites.IconType.GEAR, sx + (slotW - 18) / 2, slotY + (slotH - 18) / 2, 18, UiColors.GOLD_COIN);
            }
        }

        int textY = slotY + slotH + 6;
        graphics.drawString(font, UiFonts.text("Earn 1 of 3 cosmetics for a limited time!"), this.getX() + 10, textY, UiColors.TEXT_MUTED);

        this.actionButton.renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
