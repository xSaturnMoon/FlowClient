package com.flowclient.mods.schematics.ui;

import com.flowclient.modpanel.ModPanelScreen;
import com.flowclient.mods.schematics.PlacementManager;
import com.flowclient.mods.schematics.SchematicEntry;
import com.flowclient.mods.schematics.SchematicRepository;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public final class SchematicActionScreen extends ModPanelScreen {
    private final SchematicEntry entry;

    public SchematicActionScreen(Screen parent, SchematicEntry entry) {
        super(Component.literal(entry.getDisplayName()), parent);
        this.entry = entry;
    }

    @Override
    protected void init() {
        this.initPanel();
    }

    @Override
    protected void initPanel() {
        int bw = 200;
        int x = this.centerX(bw);
        int y = this.frameY() + 24;

        this.addRenderableWidget(Button.builder(Component.literal("Load"), button -> {
            try {
                PlacementManager.load(this.entry);
                if (this.minecraft.player != null) {
                    int blocks = PlacementManager.getSchematic().blockCount();
                    this.minecraft.player.sendSystemMessage(
                            Component.literal("Loaded schematic with " + blocks + " blocks.")
                    );
                }
                this.minecraft.gui.setScreen(null);
            } catch (IOException ex) {
                if (this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(
                            Component.literal("Failed to load schematic: " + ex.getMessage())
                    );
                }
                this.minecraft.gui.setScreen(new SchematicBrowserScreen(this.parent));
            }
        }).bounds(x, y, bw, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + 6;

        this.addRenderableWidget(Button.builder(Component.literal("Rename"), button ->
                this.minecraft.gui.setScreen(new SchematicRenameScreen(this, this.entry)))
                .bounds(x, y, bw, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + 6;

        this.addRenderableWidget(Button.builder(Component.literal("Delete"), button -> {
            try {
                SchematicRepository.delete(this.entry);
                this.minecraft.gui.setScreen(new SchematicBrowserScreen(this.parent));
            } catch (IOException ignored) {
                this.minecraft.gui.setScreen(new SchematicBrowserScreen(this.parent));
            }
        }).bounds(x, y, bw, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> this.minecraft.gui.setScreen(new SchematicBrowserScreen(this.parent))
        ).bounds(this.centerX(BUTTON_WIDTH), this.height - 32, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.drawContentFrame(graphics, this.frameX(), this.frameY(), this.frameWidth(), this.frameHeight());
        graphics.text(this.font, Component.literal(this.entry.getDisplayName()), this.frameX() + 12, this.frameY() + 10, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.literal(this.entry.getSizeLabel()), this.frameX() + 12, this.frameY() + 22, 0xFFAAAAAA, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private int frameWidth() {
        return Math.min(this.width - 40, 280);
    }

    private int frameHeight() {
        return 180;
    }

    private int frameX() {
        return this.centerX(this.frameWidth());
    }

    private int frameY() {
        return 48;
    }
}
