package com.flowclient.modpanel;

import com.flowclient.ui.UiSprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ModSettingsResetButton extends AbstractWidget {
    public static final int SIZE = 20;

    private final Runnable onPress;

    public ModSettingsResetButton(int x, int y, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.literal("Reset"));
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int fill = this.isHovered() ? ModPanelTheme.BG_HOVER : ModPanelTheme.BG_INPUT;
        ModPanelTheme.fillBordered(graphics, this.getX(), this.getY(), this.width, this.height, fill);

        int iconSize = 12;
        int iconX = this.getX() + (this.width - iconSize) / 2;
        int iconY = this.getY() + (this.height - iconSize) / 2;
        UiSprites.drawIcon(graphics, UiSprites.IconType.GEAR, iconX, iconY, iconSize, ModPanelTheme.TEXT);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        playButtonClickSound(Minecraft.getInstance().getSoundManager());
        this.onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        narration.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
    }
}
