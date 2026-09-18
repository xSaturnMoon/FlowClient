package com.flowclient.modpanel;

import com.flowclient.mods.ModifyF3Mod;
import com.flowclient.mods.NametagMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ModListPanelScreen extends ModPanelScreen {
    private static final Component TITLE = Component.literal("Mods");

    public ModListPanelScreen(Screen parent) {
        super(TITLE, parent);
    }

    @Override
    protected void init() {
        int x = this.centerX(BUTTON_WIDTH);
        int y = this.frameY() + 12;

        this.addRenderableWidget(vanillaButton(
                this.nametagLabel(),
                x,
                y,
                BUTTON_WIDTH,
                button -> {
                    NametagMod.toggle();
                    this.rebuildWidgets();
                }
        ));

        this.addRenderableWidget(vanillaButton(
                this.modifyF3Label(),
                x,
                y + BUTTON_HEIGHT + 4,
                BUTTON_WIDTH,
                button -> {
                    ModifyF3Mod.toggle();
                    this.rebuildWidgets();
                }
        ));

        this.addBackButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.drawContentFrame(
                graphics,
                this.frameX(),
                this.frameY(),
                this.frameWidth(),
                this.frameHeight()
        );
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component nametagLabel() {
        return Component.literal("Nametag: " + (NametagMod.isEnabled() ? "ON" : "OFF"));
    }

    private Component modifyF3Label() {
        return Component.literal("Modify F3: " + (ModifyF3Mod.isEnabled() ? "ON" : "OFF"));
    }

    private int frameWidth() {
        return Math.min(this.width - 40, 320);
    }

    private int frameHeight() {
        return Math.min(this.height - 80, 220);
    }

    private int frameX() {
        return this.centerX(this.frameWidth());
    }

    private int frameY() {
        return 32;
    }
}
