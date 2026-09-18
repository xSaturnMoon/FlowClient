package com.flowclient.modpanel;

import net.minecraft.network.chat.Component;

public final class ModPanelMenuScreen extends ModPanelScreen {
    private static final Component TITLE = Component.literal("FlowClient");

    public ModPanelMenuScreen() {
        super(TITLE, null);
    }

    @Override
    protected void initPanel() {
        int x = this.centerX(BUTTON_WIDTH);
        int y = this.height / 2 - BUTTON_HEIGHT - 2;

        this.addRenderableWidget(new ModPanelButton(
                x,
                y,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Component.literal("Mods"),
                () -> this.minecraft.gui.setScreen(new ModListPanelScreen(this)),
                false
        ));

        this.addRenderableWidget(new ModPanelButton(
                x,
                y + BUTTON_HEIGHT + 4,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Component.literal("Position"),
                () -> this.minecraft.gui.setScreen(new ModPositionPanelScreen(this)),
                false
        ));
    }
}
