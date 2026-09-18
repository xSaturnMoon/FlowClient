package com.flowclient.mods.schematics.ui;

import com.flowclient.modpanel.ModPanelScreen;
import com.flowclient.mods.schematics.SchematicEntry;
import com.flowclient.mods.schematics.SchematicRepository;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public final class SchematicRenameScreen extends ModPanelScreen {
    private final SchematicEntry entry;
    private EditBox nameBox;

    public SchematicRenameScreen(Screen parent, SchematicEntry entry) {
        super(Component.literal("Rename Schematic"), parent);
        this.entry = entry;
    }

    @Override
    protected void init() {
        this.initPanel();
    }

    @Override
    protected void initPanel() {
        int bw = 220;
        int x = this.centerX(bw);
        int y = this.frameY() + 28;

        this.nameBox = new EditBox(this.font, x, y, bw, BUTTON_HEIGHT, Component.literal("Name"));
        this.nameBox.setMaxLength(64);
        this.nameBox.setValue(this.entry.getDisplayName());
        this.addRenderableWidget(this.nameBox);
        y += BUTTON_HEIGHT + 10;

        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
            try {
                SchematicRepository.rename(this.entry, this.nameBox.getValue());
                this.minecraft.setScreen(new SchematicBrowserScreen(this.parent));
            } catch (IOException ignored) {
                this.minecraft.setScreen(new SchematicActionScreen(this.parent, this.entry));
            }
        }).bounds(x, y, bw, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                button -> this.minecraft.setScreen(new SchematicActionScreen(this.parent, this.entry))
        ).bounds(this.centerX(BUTTON_WIDTH), this.height - 32, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.drawContentFrame(graphics, this.frameX(), this.frameY(), this.frameWidth(), this.frameHeight());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int frameWidth() {
        return Math.min(this.width - 40, 260);
    }

    private int frameHeight() {
        return 140;
    }

    private int frameX() {
        return this.centerX(this.frameWidth());
    }

    private int frameY() {
        return 56;
    }
}
