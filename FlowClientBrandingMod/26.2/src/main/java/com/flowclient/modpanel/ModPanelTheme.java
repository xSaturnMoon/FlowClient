package com.flowclient.modpanel;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ModPanelTheme {
    public static final int BG = 0xFF1A1A1A;
    public static final int BG_HOVER = 0xFF242424;
    public static final int BG_INPUT = 0xFF141414;
    public static final int BORDER = 0xFF000000;
    public static final int BORDER_MUTED = 0xFF3A3A3A;
    public static final int ACCENT_BLUE = 0xFF3B7FD4;
    public static final int ACCENT_GREEN = 0xFF4CAF50;
    public static final int TRACK_OFF = 0xFF3A3A3A;
    public static final int TEXT = 0xFFC8C8C8;
    public static final int TEXT_MUTED = 0xFF909090;
    public static final int TEXT_DIM = 0xFF707070;
    public static final int SEPARATOR = 0xFF2E2E2E;
    public static final int KNOB = 0xFFFFFFFF;

    private ModPanelTheme() {
    }

    public static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static void fillBordered(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill, int border) {
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        drawBorder(graphics, x, y, width, height, border);
    }

    public static void fillBordered(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill) {
        fillBordered(graphics, x, y, width, height, fill, BORDER);
    }
}
