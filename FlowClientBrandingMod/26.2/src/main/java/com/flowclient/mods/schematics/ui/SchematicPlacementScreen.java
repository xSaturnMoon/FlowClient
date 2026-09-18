package com.flowclient.mods.schematics.ui;

import com.flowclient.modpanel.ModPanelScreen;
import com.flowclient.mods.schematics.AutoBuilder;
import com.flowclient.mods.schematics.PlacementManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public final class SchematicPlacementScreen extends ModPanelScreen {
    private static final Component TITLE = Component.literal("Schematic Placement");
    private static final int ROW_HEIGHT = 22;

    private int scrollOffset;

    public SchematicPlacementScreen(Screen parent) {
        super(TITLE, parent);
    }

    @Override
    protected void init() {
        if (!PlacementManager.hasPlacement()) {
            this.minecraft.gui.setScreen(parent);
            return;
        }
        this.rebuildWidgets();
    }

    @Override
    protected void initPanel() {
        int frameX = this.frameX();
        int frameY = this.frameY();
        int frameWidth = this.frameWidth();
        int y = frameY + 8 - this.scrollOffset;
        int rowWidth = frameWidth - 16;

        y = this.addRow(frameX, y, rowWidth, "Move X -", () -> PlacementManager.nudge(-1, 0, 0));
        y = this.addRow(frameX, y, rowWidth, "Move X +", () -> PlacementManager.nudge(1, 0, 0));
        y = this.addRow(frameX, y, rowWidth, "Move Y -", () -> PlacementManager.nudge(0, -1, 0));
        y = this.addRow(frameX, y, rowWidth, "Move Y +", () -> PlacementManager.nudge(0, 1, 0));
        y = this.addRow(frameX, y, rowWidth, "Move Z -", () -> PlacementManager.nudge(0, 0, -1));
        y = this.addRow(frameX, y, rowWidth, "Move Z +", () -> PlacementManager.nudge(0, 0, 1));

        y = this.addRow(frameX, y, rowWidth, "Rotate Clockwise", () ->
                PlacementManager.getTransform().rotateClockwise());
        y = this.addRow(frameX, y, rowWidth, "Rotate Counter-Clockwise", () ->
                PlacementManager.getTransform().rotateCounterClockwise());
        y = this.addRow(frameX, y, rowWidth, "Toggle Mirror", () ->
                PlacementManager.getTransform().toggleMirror());

        y = this.addRow(frameX, y, rowWidth, "Set Anchor To Player", () -> {
            if (this.minecraft.player != null) {
                PlacementManager.setAnchor(this.minecraft.player.blockPosition());
            }
        });

        y = this.addRow(frameX, y, rowWidth, "Clear Placement", () -> {
            PlacementManager.clear();
            this.minecraft.gui.setScreen(null);
        });

        if (this.minecraft.player != null && this.minecraft.player.isCreative()) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("AutoBuild"),
                    button -> AutoBuilder.build(this.minecraft)
            ).bounds(frameX + 8, frameY + this.frameHeight() - BUTTON_HEIGHT - 40, rowWidth, BUTTON_HEIGHT).build());
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> {
                    if (this.parent != null) {
                        this.minecraft.gui.setScreen(this.parent);
                    } else {
                        this.minecraft.gui.setScreen(null);
                    }
                }
        ).bounds(this.centerX(BUTTON_WIDTH), this.height - 32, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private int addRow(int frameX, int y, int rowWidth, String label, Runnable action) {
        if (y + ROW_HEIGHT >= this.frameY() && y <= this.frameY() + this.frameHeight()) {
            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    button -> {
                        action.run();
                        this.rebuildWidgets();
                    }
            ).bounds(frameX + 8, y, rowWidth, ROW_HEIGHT).build());
        }
        return y + ROW_HEIGHT + 2;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isHoveringFrame(mouseX, mouseY)) {
            this.scrollOffset = (int) Math.max(0, this.scrollOffset - scrollY * 16);
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.drawContentFrame(graphics, this.frameX(), this.frameY(), this.frameWidth(), this.frameHeight());

        BlockPos anchor = PlacementManager.getAnchor();
        var transform = PlacementManager.getTransform();
        int infoY = this.frameY() + 8;
        graphics.text(this.font, Component.literal("Anchor: " + anchor.toShortString()), this.frameX() + 8, infoY, 0xFFCCCCCC, false);
        graphics.text(this.font, Component.literal("Offset: "
                + transform.getOffsetX() + ", "
                + transform.getOffsetY() + ", "
                + transform.getOffsetZ()), this.frameX() + 8, infoY + 12, 0xFFAAAAAA, false);
        graphics.text(this.font, Component.literal("Rotation: " + labelRotation(transform.getRotation())
                + "  Mirror: " + labelMirror(transform.getMirror())), this.frameX() + 8, infoY + 24, 0xFFAAAAAA, false);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static String labelRotation(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> "90°";
            case CLOCKWISE_180 -> "180°";
            case COUNTERCLOCKWISE_90 -> "270°";
            default -> "0°";
        };
    }

    private static String labelMirror(Mirror mirror) {
        return mirror == Mirror.NONE ? "Off" : "On";
    }

    private boolean isHoveringFrame(double mouseX, double mouseY) {
        return mouseX >= this.frameX()
                && mouseX <= this.frameX() + this.frameWidth()
                && mouseY >= this.frameY()
                && mouseY <= this.frameY() + this.frameHeight();
    }

    private int frameWidth() {
        return Math.min(this.width - 40, 360);
    }

    private int frameHeight() {
        return Math.min(this.height - 80, 300);
    }

    private int frameX() {
        return this.centerX(this.frameWidth());
    }

    private int frameY() {
        return 40;
    }
}
