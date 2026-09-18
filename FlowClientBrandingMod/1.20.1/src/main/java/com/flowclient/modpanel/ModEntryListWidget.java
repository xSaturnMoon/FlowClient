package com.flowclient.modpanel;

import net.minecraft.client.gui.Font;

/**
 * Layout bounds for the scrollable mod list and the footer strip beneath it.
 */
public final class ModEntryListWidget {
    public static final int ROW_HEIGHT = 24;
    public static final int FOOTER_SEPARATOR = 1;
    public static final int FOOTER_GAP = 6;
    public static final int FOOTER_BOTTOM_PAD = 4;

    private final int x;
    private final int top;
    private final int width;
    private final int height;
    private final int footerSeparatorY;
    private final int footerTextY;

    private ModEntryListWidget(int x, int top, int width, int height, int footerSeparatorY, int footerTextY) {
        this.x = x;
        this.top = top;
        this.width = width;
        this.height = height;
        this.footerSeparatorY = footerSeparatorY;
        this.footerTextY = footerTextY;
    }

    public static ModEntryListWidget layout(
            Font font,
            int frameX,
            int frameY,
            int frameWidth,
            int frameHeight,
            int padding,
            int searchBlockHeight,
            int sidebarWidth,
            int columnGap
    ) {
        int innerX = frameX + padding;
        int innerY = frameY + padding;
        int innerWidth = frameWidth - padding * 2;
        int innerBottom = frameY + frameHeight - padding;

        int bodyTop = innerY + searchBlockHeight;
        int listX = innerX + sidebarWidth + columnGap;
        int listWidth = innerWidth - sidebarWidth - columnGap;

        int footerTextHeight = font.lineHeight;
        int footerAreaHeight = FOOTER_SEPARATOR + FOOTER_GAP + footerTextHeight + FOOTER_BOTTOM_PAD;
        int listBottom = innerBottom - footerAreaHeight;
        int listHeight = Math.max(ROW_HEIGHT, listBottom - bodyTop);

        int footerSeparatorY = listBottom;
        int footerTextY = footerSeparatorY + FOOTER_SEPARATOR + FOOTER_GAP;

        return new ModEntryListWidget(listX, bodyTop, listWidth, listHeight, footerSeparatorY, footerTextY);
    }

    public int x() {
        return this.x;
    }

    public int top() {
        return this.top;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int bottom() {
        return this.top + this.height;
    }

    public int footerSeparatorY() {
        return this.footerSeparatorY;
    }

    public int footerTextY() {
        return this.footerTextY;
    }

    public int maxScroll(int entryCount) {
        return Math.max(0, entryCount * ROW_HEIGHT - this.height);
    }

    public int rowY(int index, int scrollOffset) {
        return this.top + index * ROW_HEIGHT - scrollOffset;
    }

    public boolean isRowVisible(int rowY) {
        return rowY + ROW_HEIGHT > this.top && rowY < this.bottom();
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x
                && mouseX <= this.x + this.width
                && mouseY >= this.top
                && mouseY <= this.bottom();
    }
}
