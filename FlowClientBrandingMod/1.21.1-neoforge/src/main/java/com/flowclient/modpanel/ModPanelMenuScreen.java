package com.flowclient.modpanel;

import net.minecraft.network.chat.Component;

public final class ModPanelMenuScreen extends ModPanelScreen {
    private static final Component TITLE = Component.literal("FlowClient");

    public ModPanelMenuScreen() {
        super(TITLE, null);
    }

    @Override
    protected void init() {
        int x = this.centerX(BUTTON_WIDTH);
        int y = this.height / 2 - BUTTON_HEIGHT - 2;

        this.addRenderableWidget(vanillaButton(
                Component.literal("Mods"),
                x,
                y,
                BUTTON_WIDTH,
                button -> this.minecraft.setScreen(new ModListPanelScreen(this))
        ));

        this.addRenderableWidget(vanillaButton(
                Component.literal("Position"),
                x,
                y + BUTTON_HEIGHT + 4,
                BUTTON_WIDTH,
                button -> this.minecraft.setScreen(new ModPositionPanelScreen(this))
        ));
    }
}
