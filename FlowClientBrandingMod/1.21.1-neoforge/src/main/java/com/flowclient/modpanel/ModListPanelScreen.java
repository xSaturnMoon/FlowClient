package com.flowclient.modpanel;

import com.flowclient.mods.FreelookMod;
import com.flowclient.mods.ModifyF3Mod;
import com.flowclient.mods.NametagMod;
import com.flowclient.mods.ZoomifyMod;
import com.flowclient.mods.schematics.AllSchematicsMod;
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
        this.initPanel();
    }

    @Override
    protected void initPanel() {
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

        this.addRenderableWidget(vanillaButton(
                this.zoomifyLabel(),
                x,
                y + (BUTTON_HEIGHT + 4) * 2,
                BUTTON_WIDTH,
                button -> {
                    ZoomifyMod.toggle();
                    this.rebuildWidgets();
                }
        ));

        this.addRenderableWidget(vanillaButton(
                Component.literal("Zoomify Settings"),
                x,
                y + (BUTTON_HEIGHT + 4) * 3,
                BUTTON_WIDTH,
                button -> this.minecraft.setScreen(new ZoomifySettingsScreen(this))
        ));

        this.addRenderableWidget(vanillaButton(
                this.freelookLabel(),
                x,
                y + (BUTTON_HEIGHT + 4) * 4,
                BUTTON_WIDTH,
                button -> {
                    FreelookMod.toggle();
                    this.rebuildWidgets();
                }
        ));

        this.addRenderableWidget(vanillaButton(
                this.allSchematicsLabel(),
                x,
                y + (BUTTON_HEIGHT + 4) * 5,
                BUTTON_WIDTH,
                button -> {
                    AllSchematicsMod.toggle();
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

    private Component zoomifyLabel() {
        return Component.literal("Zoomify: " + (ZoomifyMod.isEnabled() ? "ON" : "OFF"));
    }

    private Component freelookLabel() {
        return Component.literal("Freelook: " + (FreelookMod.isEnabled() ? "ON" : "OFF"));
    }

    private Component allSchematicsLabel() {
        return Component.literal("AllSchematics: " + (AllSchematicsMod.isEnabled() ? "ON" : "OFF"));
    }

    private int frameWidth() {
        return Math.min(this.width - 40, 320);
    }

    private int frameHeight() {
        return Math.min(this.height - 80, 360);
    }

    private int frameX() {
        return this.centerX(this.frameWidth());
    }

    private int frameY() {
        return 32;
    }
}
