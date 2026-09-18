package com.flowclient.modpanel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class ModPanelButton extends AbstractWidget {
    private final Runnable onPress;
    private final boolean alignLeft;

    public ModPanelButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        this(x, y, width, height, message, onPress, true);
    }

    public ModPanelButton(int x, int y, int width, int height, Component message, Runnable onPress, boolean alignLeft) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.alignLeft = alignLeft;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int fill = this.isHovered() ? ModPanelTheme.BG_HOVER : ModPanelTheme.BG_INPUT;
        ModPanelTheme.fillBordered(graphics, this.getX(), this.getY(), this.width, this.height, fill);

        Font font = Minecraft.getInstance().font;
        int textX = this.alignLeft
                ? this.getX() + 6
                : this.getX() + (this.width - font.width(this.getMessage())) / 2;
        int textY = this.getY() + (this.height - font.lineHeight) / 2;
        com.flowclient.compat.FlowGfx.text(graphics, font, this.getMessage(), textX, textY, ModPanelTheme.TEXT);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        playDownSound(Minecraft.getInstance().getSoundManager());
        this.onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        narration.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
    }
}
