package com.flowclient.ui;

import com.flowclient.compat.FlowGfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class FlowSidebarButton extends AbstractButton {
    public static final int HEIGHT = 40;

    private final Runnable onPress;

    public FlowSidebarButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        UiColors.fillRounded(
                graphics,
                this.getX(),
                this.getY(),
                this.width,
                this.height,
                hovered ? UiColors.SIDEBAR_HOVER : UiColors.SIDEBAR_IDLE
        );

        int textColor = hovered ? UiColors.SIDEBAR_TEXT_HOVER : UiColors.SIDEBAR_TEXT;
        int iconSize = 16;
        int iconX = this.getX() + 14;
        int iconY = this.getY() + (this.height - iconSize) / 2;
        UiSprites.blitIcon(graphics, UiSprites.ICON_SINGLEPLAYER, iconX, iconY, iconSize);

        Font font = Minecraft.getInstance().font;
        Component label = UiFonts.text(this.getMessage());
        int textY = this.getY() + (this.height - font.lineHeight) / 2;
        FlowGfx.text(graphics, font, label, iconX + iconSize + 10, textY, textColor);
    }

    @Override
    public void onPress() {
        this.onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
