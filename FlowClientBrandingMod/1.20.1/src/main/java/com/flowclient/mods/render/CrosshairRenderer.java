package com.flowclient.mods.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class CrosshairRenderer {
    private CrosshairRenderer() {
    }

    public static void render(Minecraft mc, GuiGraphics graphics) {
        CrosshairSettings settings = CrosshairSettings.get();
        // Vanilla F3 visibility check omitted on 1.20.1 (Options debug key name differs).

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;
        boolean entityTarget = mc.crosshairPickEntity != null;
        int color = settings.resolveColorArgb(entityTarget);
        int outline = settings.resolveOutlineColorArgb();

        switch (settings.style()) {
            case CROSS -> drawCross(graphics, cx, cy, settings.size(), settings.thickness(), 0, color, outline, settings);
            case GAP_CROSS -> drawCross(graphics, cx, cy, settings.size(), settings.thickness(), settings.gap(), color, outline, settings);
            case DOT -> drawDot(graphics, cx, cy, Math.max(1, settings.dotSize()), color, outline, settings);
            case CIRCLE -> drawCircle(graphics, cx, cy, settings.size(), settings.thickness(), color, outline, settings);
            case T_SHAPE -> drawTShape(graphics, cx, cy, settings.size(), settings.thickness(), color, outline, settings);
            case SQUARE -> drawSquare(graphics, cx, cy, settings.size(), settings.thickness(), color, outline, settings);
            case DIAMOND -> drawDiamond(graphics, cx, cy, settings.size(), settings.thickness(), color, outline, settings);
        }

        if (settings.dot() && settings.style() != CrosshairStyle.DOT) {
            drawDot(graphics, cx, cy, settings.dotSize(), color, outline, settings);
        }
    }

    private static void drawCross(
            GuiGraphics graphics,
            int cx,
            int cy,
            int size,
            int thick,
            int gap,
            int color,
            int outline,
            CrosshairSettings settings
    ) {
        if (settings.outline()) {
            hBar(graphics, cx, cy, size, thick + 2, gap, outline);
            vBar(graphics, cx, cy, size, thick + 2, gap, outline);
        }
        hBar(graphics, cx, cy, size, thick, gap, color);
        vBar(graphics, cx, cy, size, thick, gap, color);
    }

    private static void drawTShape(
            GuiGraphics graphics,
            int cx,
            int cy,
            int size,
            int thick,
            int color,
            int outline,
            CrosshairSettings settings
    ) {
        if (settings.outline()) {
            graphics.fill(cx - size, cy - thick - 1, cx + size + 1, cy + thick + 2, outline);
            graphics.fill(cx - thick - 1, cy, cx + thick + 2, cy + size + 1, outline);
        }
        graphics.fill(cx - size, cy - thick, cx + size + 1, cy + thick + 1, color);
        graphics.fill(cx - thick, cy, cx + thick + 1, cy + size + 1, color);
    }

    private static void drawSquare(
            GuiGraphics graphics,
            int cx,
            int cy,
            int size,
            int thick,
            int color,
            int outline,
            CrosshairSettings settings
    ) {
        if (settings.outline()) {
            graphics.fill(cx - size - 1, cy - size - 1, cx + size + 2, cy + size + 2, outline);
        }
        graphics.fill(cx - size, cy - size, cx + size + 1, cy - size + thick, color);
        graphics.fill(cx - size, cy + size - thick + 1, cx + size + 1, cy + size + 1, color);
        graphics.fill(cx - size, cy - size, cx - size + thick, cy + size + 1, color);
        graphics.fill(cx + size - thick + 1, cy - size, cx + size + 1, cy + size + 1, color);
    }

    private static void drawDiamond(
            GuiGraphics graphics,
            int cx,
            int cy,
            int size,
            int thick,
            int color,
            int outline,
            CrosshairSettings settings
    ) {
        for (int i = -size; i <= size; i++) {
            int width = thick;
            if (settings.outline()) {
                graphics.fill(cx + i - 1, cy - Math.abs(i) - 1, cx + i + width + 1, cy - Math.abs(i) + width + 1, outline);
                graphics.fill(cx + i - 1, cy + Math.abs(i) - width, cx + i + width + 1, cy + Math.abs(i) + 1, outline);
            }
            graphics.fill(cx + i, cy - Math.abs(i), cx + i + width, cy - Math.abs(i) + width, color);
            graphics.fill(cx + i, cy + Math.abs(i) - width + 1, cx + i + width, cy + Math.abs(i) + 1, color);
        }
    }

    private static void drawDot(
            GuiGraphics graphics,
            int cx,
            int cy,
            int radius,
            int color,
            int outline,
            CrosshairSettings settings
    ) {
        if (settings.outline()) {
            graphics.fill(cx - radius - 1, cy - radius - 1, cx + radius + 2, cy + radius + 2, outline);
        }
        graphics.fill(cx - radius, cy - radius, cx + radius + 1, cy + radius + 1, color);
    }

    private static void drawCircle(
            GuiGraphics graphics,
            int cx,
            int cy,
            int radius,
            int thick,
            int color,
            int outline,
            CrosshairSettings settings
    ) {
        int outer = radius + thick;
        if (settings.outline()) {
            drawRing(graphics, cx, cy, outer + 1, thick + 1, outline);
        }
        drawRing(graphics, cx, cy, outer, thick, color);
    }

    private static void drawRing(GuiGraphics graphics, int cx, int cy, int outer, int thick, int color) {
        graphics.fill(cx - outer, cy - outer, cx + outer + 1, cy - outer + thick, color);
        graphics.fill(cx - outer, cy + outer - thick + 1, cx + outer + 1, cy + outer + 1, color);
        graphics.fill(cx - outer, cy - outer, cx - outer + thick, cy + outer + 1, color);
        graphics.fill(cx + outer - thick + 1, cy - outer, cx + outer + 1, cy + outer + 1, color);
    }

    private static void hBar(GuiGraphics graphics, int cx, int cy, int size, int thick, int gap, int color) {
        graphics.fill(cx - size, cy - thick, cx - gap, cy + thick + 1, color);
        graphics.fill(cx + gap + 1, cy - thick, cx + size + 1, cy + thick + 1, color);
    }

    private static void vBar(GuiGraphics graphics, int cx, int cy, int size, int thick, int gap, int color) {
        graphics.fill(cx - thick, cy - size, cx + thick + 1, cy - gap, color);
        graphics.fill(cx - thick, cy + gap + 1, cx + thick + 1, cy + size + 1, color);
    }
}
