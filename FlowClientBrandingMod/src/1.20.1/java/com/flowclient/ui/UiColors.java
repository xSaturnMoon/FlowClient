package com.flowclient.ui;

import net.minecraft.client.gui.GuiGraphics;
import com.flowclient.util.FlowArgb;

public final class UiColors {
    // Dark glass card surfaces
    public static final int CARD_BG = FlowArgb.color(220, 15, 17, 23);         // #0F1117 translucent
    public static final int CARD_BG_HOVER = FlowArgb.color(240, 26, 30, 42);   // #1A1E2A
    public static final int CARD_BORDER = FlowArgb.color(80, 255, 255, 255);

    // Sidebar compatibility constants
    public static final int SIDEBAR_IDLE = FlowArgb.color(210, 22, 22, 24);
    public static final int SIDEBAR_HOVER = FlowArgb.color(255, 34, 34, 36);
    public static final int SIDEBAR_TEXT = FlowArgb.color(255, 200, 200, 204);
    public static final int SIDEBAR_TEXT_HOVER = FlowArgb.color(255, 255, 255, 255);

    // Main action buttons
    public static final int BTN_IDLE = FlowArgb.color(225, 20, 23, 31);         // #14171F
    public static final int BTN_HOVER = FlowArgb.color(255, 38, 43, 56);        // #262B38
    public static final int BTN_TEXT = FlowArgb.color(255, 240, 242, 245);
    public static final int BTN_TEXT_MUTED = FlowArgb.color(255, 148, 163, 184);

    // Store button (green accent)
    public static final int STORE_BG = FlowArgb.color(220, 20, 83, 45);        // #14532D
    public static final int STORE_HOVER = FlowArgb.color(255, 22, 101, 52);    // #166534
    public static final int STORE_BORDER = FlowArgb.color(255, 34, 197, 94);   // #22C55E
    public static final int STORE_TEXT = FlowArgb.color(255, 74, 222, 128);    // #4ADE80

    // Quest button (lime accent)
    public static final int QUEST_BTN_BG = FlowArgb.color(255, 132, 204, 22);  // #84CC16
    public static final int QUEST_BTN_HOVER = FlowArgb.color(255, 163, 230, 53);// #A3E635
    public static final int QUEST_BTN_TEXT = FlowArgb.color(255, 15, 23, 42);  // #0F172A

    // Accent colors
    public static final int GOLD_COIN = FlowArgb.color(255, 245, 158, 11);     // #F59E0B
    public static final int ORANGE_BADGE = FlowArgb.color(255, 234, 88, 12);    // #EA580C
    public static final int TEXT_MUTED = FlowArgb.color(255, 148, 163, 184);
    public static final int TEXT_FOOTER = FlowArgb.color(180, 148, 163, 184);

    private UiColors() {
    }

    public static void fillRounded(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        fillRounded(graphics, x, y, width, height, 6, color);
    }

    public static void fillRounded(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        radius = Math.min(radius, Math.min(width, height) / 2);
        if (radius <= 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }

        graphics.fill(x + radius, y, x + width - radius, y + height, color);
        graphics.fill(x, y + radius, x + width, y + height - radius, color);

        for (int r = 1; r < radius; r++) {
            int cornerIndent = radius - (int) Math.sqrt(radius * radius - (radius - r) * (radius - r));
            if (cornerIndent > 0) {
                graphics.fill(x + r - 1, y, x + r, y + cornerIndent, color);
                graphics.fill(x + width - r, y, x + width - r + 1, y + cornerIndent, color);
                graphics.fill(x + r - 1, y + height - cornerIndent, x + r, y + height, color);
                graphics.fill(x + width - r, y + height - cornerIndent, x + width - r + 1, y + height, color);
            }
        }
    }

    public static void drawRoundedOutline(GuiGraphics graphics, int x, int y, int width, int height, int radius, int borderThickness, int borderColor) {
        graphics.fill(x + radius, y, x + width - radius, y + borderThickness, borderColor);
        graphics.fill(x + radius, y + height - borderThickness, x + width - radius, y + height, borderColor);
        graphics.fill(x, y + radius, x + borderThickness, y + height - radius, borderColor);
        graphics.fill(x + width - borderThickness, y + radius, x + width, y + height - radius, borderColor);
    }
}
