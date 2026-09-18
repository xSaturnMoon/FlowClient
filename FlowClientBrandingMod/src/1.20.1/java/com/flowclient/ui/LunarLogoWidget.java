package com.flowclient.ui;

import net.minecraft.client.gui.GuiGraphics;
import com.flowclient.util.FlowArgb;

/**
 * Draws the FlowClient / Lunar-style crescent moon emblem.
 * Uses filled rectangles in a carefully crafted pattern to simulate a round moon shape.
 */
public final class LunarLogoWidget {
    private LunarLogoWidget() {}

    /**
     * Draw the moon emblem centered at (centerX, topY).
     * @param centerX horizontal center of the emblem
     * @param topY    top of the emblem
     * @param size    diameter (~40px looks good)
     */
    public static void drawLogo(GuiGraphics g, int centerX, int topY, int size) {
        int left = centerX - size / 2;

        // ── Background container (rounded square, semi-dark) ──────────────────
        int bgDark = FlowArgb.color(200, 12, 14, 22);
        int glow   = FlowArgb.color(60, 99, 163, 255);
        UiColors.fillRounded(g, left - 2, topY - 2, size + 4, size + 4, 14, glow);
        UiColors.fillRounded(g, left,     topY,     size,     size,     12, bgDark);

        // ── Moon: draw white circle, then punch a hole offset to create crescent ──
        int white = FlowArgb.color(255, 240, 245, 255);
        int hole  = bgDark;   // same color as bg = invisible = crescent effect

        // We approximate circles with stacked horizontal rectangles
        // Outer (white) circle – radius ≈ size*0.38
        int cx = left + size / 2;
        int cy = topY + size / 2;
        int r  = Math.max(6, (int)(size * 0.38));
        fillCircle(g, cx, cy, r, white);

        // Inner (punch-out) circle offset to create crescent shape
        // Offset: slightly right and up so the crescent opens to the right
        int offX = (int)(r * 0.45);
        int offY = (int)(r * -0.2);
        int r2   = (int)(r * 0.78);
        fillCircle(g, cx + offX, cy + offY, r2, hole);

        // ── Stars (small squares at specific positions inside emblem) ──────────
        int starColor = FlowArgb.color(255, 200, 215, 255);
        // Star 1 – top right of moon
        int s1x = cx + r - 4, s1y = topY + 5;
        g.fill(s1x, s1y, s1x + 2, s1y + 2, starColor);
        // Star 2 – smaller, slightly lower
        int s2x = s1x + 5, s2y = s1y + 4;
        g.fill(s2x, s2y, s2x + 1, s2y + 1, starColor);
        // Star 3 – bottom of emblem
        int s3x = cx + r - 1, s3y = cy + 3;
        g.fill(s3x, s3y, s3x + 2, s3y + 2, starColor);
    }

    /** Fill a circle approximated by stacked horizontal rectangles. */
    private static void fillCircle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int halfW = (int) Math.sqrt((double)(r * r - dy * dy));
            if (halfW <= 0) continue;
            g.fill(cx - halfW, cy + dy, cx + halfW, cy + dy + 1, color);
        }
    }
}
