package com.flowclient.mods.keystrokes;

import com.flowclient.compat.FlowGfx;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

final class KeystrokesDraw {
    private KeystrokesDraw() {
    }

    static void fillRounded(GuiGraphics graphics, int x, int y, int w, int h, int radius, int color) {
        if (radius <= 0) {
            graphics.fill(x, y, x + w, y + h, color);
            return;
        }

        int r = Math.min(radius, Math.min(w, h) / 2);
        graphics.fill(x + r, y, x + w - r, y + h, color);
        graphics.fill(x, y + r, x + w, y + h - r, color);
        graphics.fill(x, y, x + r, y + r, color);
        graphics.fill(x + w - r, y, x + w, y + r, color);
        graphics.fill(x, y + h - r, x + r, y + h, color);
        graphics.fill(x + w - r, y + h - r, x + w, y + h, color);
    }

    static void drawKey(
            GuiGraphics graphics,
            Font font,
            KeystrokesSettings settings,
            String label,
            String subLabel,
            int x,
            int y,
            int w,
            int h,
            boolean pressed
    ) {
        KeystrokesTheme theme = settings.theme();
        int radius = settings.style() == KeystrokesStyle.MINIMAL ? Math.min(2, settings.cornerRadius()) : settings.cornerRadius();

        if (pressed && settings.pressedGlow()) {
            fillRounded(graphics, x - 1, y - 1, w + 2, h + 2, radius + 1, theme.glow());
        }

        int background = pressed ? theme.pressedBackground() : theme.idleBackground();
        if (settings.style() == KeystrokesStyle.MINIMAL && !pressed) {
            background = (background & 0x00FFFFFF) | 0x55000000;
        }

        fillRounded(graphics, x, y, w, h, radius, background);

        if (settings.showBorders()) {
            int border = pressed ? theme.pressedBackground() : theme.border();
            graphics.fill(x + radius, y, x + w - radius, y + 1, border);
            graphics.fill(x + radius, y + h - 1, x + w - radius, y + h, border);
            graphics.fill(x, y + radius, x + 1, y + h - radius, border);
            graphics.fill(x + w - 1, y + radius, x + w, y + h - radius, border);
        }

        int textColor = pressed ? theme.pressedText() : theme.idleText();
        int mainY = y + (h - font.lineHeight) / 2 - (subLabel == null ? 0 : 3);
        drawText(graphics, font, label, x, mainY, w, textColor, settings.textShadow());

        if (subLabel != null) {
            int subY = mainY + font.lineHeight + 1;
            drawText(graphics, font, subLabel, x, subY, w, (textColor & 0x00FFFFFF) | 0xAA000000, settings.textShadow());
        }
    }

    static void drawText(
            GuiGraphics graphics,
            Font font,
            String text,
            int x,
            int y,
            int width,
            int color,
            boolean shadow
    ) {
        int tx = x + (width - font.width(text)) / 2;
        FlowGfx.text(graphics, font, text, tx, y, color, shadow);
    }
}
