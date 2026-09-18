package com.flowclient.mods.schematics.ui;

import com.flowclient.compat.FlowGfx;

import com.flowclient.modpanel.ModPanelScreen;
import com.flowclient.mods.schematics.PlacementManager;
import com.flowclient.mods.schematics.SchematicEntry;
import com.flowclient.mods.schematics.SchematicImporter;
import com.flowclient.mods.schematics.SchematicRepository;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SchematicBrowserScreen extends ModPanelScreen {
    private static final Component TITLE = Component.literal("AllSchematics");
    private static final int ARROW_WIDTH = 24;
    private static final int COLUMN_GAP = 8;
    private static final int ROW_GAP = 4;

    private EditBox searchBox;
    private String searchQuery = "";
    private List<SchematicEntry> entries = List.of();
    private int scrollOffset;

    public SchematicBrowserScreen(Screen parent) {
        super(TITLE, parent);
    }

    @Override
    protected void init() {
        this.reloadEntries();
        this.rebuildWidgets();
    }

    @Override
    protected void initPanel() {
        int frameX = this.frameX();
        int frameY = this.frameY();
        int frameWidth = this.frameWidth();
        int topY = frameY + 8;
        int addWidth = 116;
        int searchWidth = frameWidth - addWidth - ARROW_WIDTH - (COLUMN_GAP * 2) - 16;
        int addX = frameX + 8;
        int arrowX = addX + addWidth + COLUMN_GAP;
        int searchX = arrowX + ARROW_WIDTH + COLUMN_GAP;

        this.addRenderableWidget(Button.builder(Component.literal("Add Schematic"), button -> {
            if (SchematicImporter.importFromDialog()) {
                this.reloadEntries();
                this.rebuildWidgets();
            }
        }).bounds(addX, topY, addWidth, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            if (!PlacementManager.hasPlacement()) {
                if (this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(
                            Component.literal("Load a schematic first to open placement settings.")
                    );
                }
                return;
            }
            this.minecraft.setScreen(new SchematicPlacementScreen(this));
        }).bounds(arrowX, topY, ARROW_WIDTH, BUTTON_HEIGHT).build());

        this.searchBox = new EditBox(this.font, searchX, topY, searchWidth, BUTTON_HEIGHT, Component.literal("Search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setHint(Component.literal("Search schematics..."));
        this.searchBox.setResponder(value -> {
            this.searchQuery = value;
            this.scrollOffset = 0;
            this.reloadEntries();
            this.rebuildWidgets();
        });
        this.addRenderableWidget(this.searchBox);

        int listTop = topY + BUTTON_HEIGHT + 8;
        int listHeight = frameY + this.frameHeight() - listTop - 8;
        int columnWidth = (frameWidth - 16 - COLUMN_GAP) / 2;
        int rowHeight = BUTTON_HEIGHT + ROW_GAP;
        int visibleRows = Math.max(1, listHeight / rowHeight);
        int maxScroll = Math.max(0, ((entries.size() + 1) / 2) - visibleRows);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));

        int startIndex = this.scrollOffset * 2;
        for (int i = startIndex; i < entries.size(); i++) {
            int row = (i - startIndex) / 2;
            if (row >= visibleRows) {
                break;
            }
            int column = i % 2;
            int x = frameX + 8 + column * (columnWidth + COLUMN_GAP);
            int y = listTop + row * rowHeight;
            SchematicEntry entry = entries.get(i);
            this.addRenderableWidget(Button.builder(
                    Component.literal(entry.getDisplayName()),
                    button -> this.minecraft.setScreen(new SchematicActionScreen(this, entry))
            ).bounds(x, y, columnWidth, BUTTON_HEIGHT).build());
        }

        if (entries.isEmpty()) {
            // no entries - hint text rendered in extractRenderState
        }

        if (this.parent != null) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Back"),
                    button -> this.minecraft.setScreen(this.parent)
            ).bounds(this.centerX(BUTTON_WIDTH), this.height - 32, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (this.isHoveringFrame(mouseX, mouseY)) {
            this.scrollOffset = (int) Math.max(0, this.scrollOffset - (int) scrollDelta);
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.drawContentFrame(graphics, this.frameX(), this.frameY(), this.frameWidth(), this.frameHeight());
        if (this.entries.isEmpty()) {
            FlowGfx.text(graphics, 
                    this.font,
                    Component.literal("No schematics found. Add files to " + SchematicRepository.getRoot()),
                    this.frameX() + 12,
                    this.frameY() + 64,
                    0xFFAAAAAA,
                    false
            );
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void reloadEntries() {
        try {
            this.entries = new ArrayList<>(SchematicRepository.search(this.searchQuery));
        } catch (IOException ex) {
            this.entries = List.of();
        }
    }

    private boolean isHoveringFrame(double mouseX, double mouseY) {
        return mouseX >= this.frameX()
                && mouseX <= this.frameX() + this.frameWidth()
                && mouseY >= this.frameY()
                && mouseY <= this.frameY() + this.frameHeight();
    }

    private int frameWidth() {
        return Math.min(this.width - 40, 420);
    }

    private int frameHeight() {
        return Math.min(this.height - 60, this.height - 80);
    }

    private int frameX() {
        return this.centerX(this.frameWidth());
    }

    private int frameY() {
        return 24;
    }
}
