package com.flowclient.modpanel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ModPanelScreen extends Screen {
    protected static final int BUTTON_WIDTH = 200;
    protected static final int BUTTON_HEIGHT = Button.DEFAULT_HEIGHT;

    protected final Screen parent;

    protected ModPanelScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected int centerX(int width) {
        return (this.width - width) / 2;
    }

    protected Button vanillaButton(Component label, int x, int y, int width, Button.OnPress onPress) {
        return Button.builder(label, onPress).bounds(x, y, width, BUTTON_HEIGHT).build();
    }

    protected void addBackButton() {
        this.addRenderableWidget(vanillaButton(
                Component.literal("Back"),
                this.centerX(BUTTON_WIDTH),
                this.height - 32,
                BUTTON_WIDTH,
                button -> this.minecraft.setScreen(this.parent)
        ));
    }

    protected void drawContentFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xC0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF4A4A4A);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF1A1A1A);
        graphics.fill(x, y, x + 1, y + height, 0xFF4A4A4A);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF1A1A1A);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.extractMenuBackground(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
