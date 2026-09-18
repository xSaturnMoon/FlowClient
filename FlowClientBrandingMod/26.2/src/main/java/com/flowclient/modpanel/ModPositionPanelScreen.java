package com.flowclient.modpanel;

import com.flowclient.mods.hud.HudBounds;
import com.flowclient.mods.hud.HudEdgeGuides;
import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayout;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.render.ScoreboardAnchor;
import com.flowclient.mods.render.ScoreboardSettings;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ModPositionPanelScreen extends ModPanelScreen {
    private static final Component TITLE = Component.literal("Position");

    private HudElement dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public ModPositionPanelScreen(Screen parent) {
        super(TITLE, parent);
    }

    @Override
    protected void initPanel() {
        this.addBackButton();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        List<HudElement> enabled = HudElement.enabledElements();
        for (HudElement element : enabled) {
            element.renderPreview(this.minecraft, graphics, this.font);
        }

        for (HudElement element : enabled) {
            HudBounds.Bounds bounds = HudBounds.get(element, this.minecraft, true);
            boolean hovered = bounds.contains(mouseX, mouseY);
            boolean selected = element == this.dragging;
            int color = selected ? 0xFF4A9EE0 : (hovered ? 0xCCFFFFFF : 0x88FFFFFF);

            graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + 1, color);
            graphics.fill(bounds.x(), bounds.y() + bounds.height() - 1, bounds.x() + bounds.width(), bounds.y() + bounds.height(), color);
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + 1, bounds.y() + bounds.height(), color);
            graphics.fill(bounds.x() + bounds.width() - 1, bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), color);

            graphics.text(
                    this.font,
                    element.getDisplayName(),
                    bounds.x() + 4,
                    bounds.y() - 10,
                    0xFFFFFFFF,
                    true
            );
        }

        if (this.dragging != null) {
            this.drawEdgeGuides(graphics, HudBounds.get(this.dragging, this.minecraft, true));
        }

        String hint = enabled.isEmpty()
                ? "No active HUD mods. Enable mods from the mod list first."
                : "Drag HUD elements to reposition them.";
        graphics.text(
                this.font,
                hint,
                this.centerX(this.font.width(hint)),
                12,
                0xFFDDDDDD,
                false
        );

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawEdgeGuides(GuiGraphicsExtractor graphics, HudBounds.Bounds bounds) {
        HudEdgeGuides.EdgeDistances distances = HudEdgeGuides.distances(bounds, this.width, this.height);
        int guideColor = 0xAA4A9EE0;
        int centerX = bounds.x() + bounds.width() / 2;
        int centerY = bounds.y() + bounds.height() / 2;

        this.drawDashedLine(graphics, bounds.x(), centerY, 0, centerY, guideColor);
        this.drawDashedLine(graphics, bounds.x() + bounds.width(), centerY, this.width, centerY, guideColor);
        this.drawDashedLine(graphics, centerX, bounds.y(), centerX, 0, guideColor);
        this.drawDashedLine(graphics, centerX, bounds.y() + bounds.height(), centerX, this.height, guideColor);

        this.drawEdgeLabel(graphics, "L " + distances.left() + "px", bounds.x() / 2 - 16, centerY - 4, distances.left());
        this.drawEdgeLabel(graphics, "R " + distances.right() + "px", bounds.x() + bounds.width() + (this.width - bounds.x() - bounds.width()) / 2 - 16, centerY - 4, distances.right());
        this.drawEdgeLabel(graphics, "T " + distances.top() + "px", centerX - 16, bounds.y() / 2 - 4, distances.top());
        this.drawEdgeLabel(graphics, "B " + distances.bottom() + "px", centerX - 16, bounds.y() + bounds.height() + (this.height - bounds.y() - bounds.height()) / 2 - 4, distances.bottom());

        String nearestText = distances.nearest() + "px from " + distances.nearestEdgeName();
        int labelWidth = this.font.width(nearestText);
        int labelX = bounds.x() + (bounds.width() - labelWidth) / 2;
        int labelY = bounds.y() + bounds.height() + 6;
        graphics.fill(labelX - 4, labelY - 2, labelX + labelWidth + 4, labelY + 10, 0xCC101018);
        graphics.text(this.font, nearestText, labelX, labelY, 0xFF4A9EE0, false);
    }

    private void drawEdgeLabel(GuiGraphicsExtractor graphics, String text, int x, int y, int distance) {
        int color = distance == HudEdgeGuides.distances(
                HudBounds.get(this.dragging, this.minecraft, true),
                this.width,
                this.height
        ).nearest() ? 0xFF4A9EE0 : 0xFFCCCCCC;
        graphics.text(this.font, text, x, y, color, true);
    }

    private void drawDashedLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        if (x1 == x2) {
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            for (int y = minY; y < maxY; y += 4) {
                graphics.fill(x1, y, x1 + 1, Math.min(y + 2, maxY), color);
            }
            return;
        }

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        for (int x = minX; x < maxX; x += 4) {
            graphics.fill(x, y1, Math.min(x + 2, maxX), y1 + 1, color);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (HudElement element : HudElement.enabledElements()) {
                HudBounds.Bounds bounds = HudBounds.get(element, this.minecraft, true);
                if (bounds.contains(event.x(), event.y())) {
                    this.dragging = element;
                    this.dragOffsetX = (int) event.x() - bounds.x();
                    this.dragOffsetY = (int) event.y() - bounds.y();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && this.dragging != null) {
            int newY = (int) event.y() - this.dragOffsetY;
            if (this.dragging == HudElement.SCOREBOARD && ScoreboardSettings.get().anchor() != ScoreboardAnchor.CUSTOM) {
                HudLayoutManager.get().setPositionQuiet(this.dragging, HudLayout.UNSET, newY);
            } else {
                HudLayoutManager.get().setPositionQuiet(
                        this.dragging,
                        (int) event.x() - this.dragOffsetX,
                        newY
                );
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.dragging != null) {
            HudElement released = this.dragging;
            this.dragging = null;
            HudLayoutManager.get().flushPosition(released);
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
    }
}
