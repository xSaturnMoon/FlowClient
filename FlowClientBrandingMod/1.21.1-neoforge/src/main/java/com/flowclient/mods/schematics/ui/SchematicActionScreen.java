package com.flowclient.mods.schematics.ui;

import com.flowclient.modpanel.ModPanelScreen;
import com.flowclient.mods.schematics.PlacementManager;
import com.flowclient.mods.schematics.SchematicEntry;
import com.flowclient.mods.schematics.SchematicRepository;
import net.minecraft.client.gui.GuiGraphics;
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

        this.addRenderableWidget(vanillaButton(Component.literal("Load"), x, y, bw, button -> {
            try {
                PlacementManager.load(this.entry);
                if (this.minecraft.player != null) {
                    int blocks = PlacementManager.getSchematic().blockCount();
                    this.minecraft.player.sendSystemMessage(
                            Component.literal("Loaded schematic with " + blocks + " blocks.")
                    );
                }
                this.minecraft.setScreen(null);
            } catch (IOException ex) {
                if (this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(
                            Component.literal("Failed to load schematic: " + ex.getMessage())
                    );
                }
                this.minecraft.setScreen(new SchematicBrowserScreen(this.parent));
            }
        }));
        y += BUTTON_HEIGHT + 6;

        this.addRenderableWidget(vanillaButton(Component.literal("Rename"), x, y, bw, button ->
                this.minecraft.setScreen(new SchematicRenameScreen(this, this.entry))));
        y += BUTTON_HEIGHT + 6;

        this.addRenderableWidget(vanillaButton(Component.literal("Delete"), x, y, bw, button -> {
            try {
                SchematicRepository.delete(this.entry);
                this.minecraft.setScreen(new SchematicBrowserScreen(this.parent));
            } catch (IOException ignored) {
                this.minecraft.setScreen(new SchematicBrowserScreen(this.parent));
            }
        }));

        this.addRenderableWidget(vanillaButton(
                Component.literal("Back"),
                this.centerX(BUTTON_WIDTH),
                this.height - 32,
                BUTTON_WIDTH,
                button -> this.minecraft.setScreen(new SchematicBrowserScreen(this.parent))
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.drawContentFrame(graphics, this.frameX(), this.frameY(), this.frameWidth(), this.frameHeight());
        graphics.drawString(this.font, this.entry.getDisplayName(), this.frameX() + 12, this.frameY() + 10, 0xFFFFFFFF, false);
        graphics.drawString(this.font, this.entry.getSizeLabel(), this.frameX() + 12, this.frameY() + 22, 0xFFAAAAAA, false);
        super.render(graphics, mouseX, mouseY, partialTick);
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
