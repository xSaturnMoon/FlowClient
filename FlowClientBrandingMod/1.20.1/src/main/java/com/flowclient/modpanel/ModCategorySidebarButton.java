package com.flowclient.modpanel;

import com.flowclient.compat.FlowGfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class ModCategorySidebarButton extends AbstractWidget {
    private final Runnable onPress;
    private boolean selected;
    private boolean dimmed;

    public ModCategorySidebarButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setDimmed(boolean dimmed) {
        this.dimmed = dimmed;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        boolean hovered = this.isHovered() && !this.dimmed;

        int fill;
        if (this.selected && !this.dimmed) {
            fill = ModPanelTheme.ACCENT_BLUE;
        } else if (hovered) {
            fill = ModPanelTheme.BG_HOVER;
        } else {
            fill = ModPanelTheme.BG;
        }

        ModPanelTheme.fillBordered(graphics, x, y, this.width, this.height, fill);

        Font font = Minecraft.getInstance().font;
        int textColor = this.dimmed
                ? ModPanelTheme.TEXT_DIM
                : (this.selected ? ModPanelTheme.KNOB : ModPanelTheme.TEXT);
        int textX = x + 6;
        int textY = y + (this.height - font.lineHeight) / 2;
        FlowGfx.text(graphics, font, this.getMessage(), textX, textY, textColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!this.dimmed) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress.run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
    }
}
