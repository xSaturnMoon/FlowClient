package com.flowclient.modpanel;

import com.flowclient.ui.UiSprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class ModSettingsIconButton extends AbstractWidget {
    public static final int SIZE = 12;

    private final Runnable onPress;

    public ModSettingsIconButton(int x, int y, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.literal("Settings"));
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.isHovered()) {
            graphics.fill(this.getX(), this.getY(), this.getX() + SIZE, this.getY() + SIZE, ModPanelTheme.BG_HOVER);
        }

        UiSprites.drawIcon(
                graphics,
                UiSprites.IconType.GEAR,
                this.getX(),
                this.getY(),
                SIZE,
                ModPanelTheme.TEXT
        );
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
