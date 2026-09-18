package com.flowclient.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public final class UiSprites {
    public static final Identifier ICON_SINGLEPLAYER = Identifier.fromNamespaceAndPath(
            "flowclient", "textures/gui/icon_singleplayer"
    );

    public static void blitIcon(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int size) {
        drawIcon(graphics, IconType.SINGLEPLAYER, x, y, size, UiColors.BTN_TEXT);
    }

    public enum IconType {
        SINGLEPLAYER,
        MULTIPLAYER,
        GLOBE,
        STORE_CART,
        COIN,
        SHIRT,
        CLOSE,
        HOME,
        FRIENDS,
        GEAR,
        LANGUAGE,
        ACCESSIBILITY,
        MENU,
        GIFT,
        HELP,
        LINK
    }

    private UiSprites() {
    }

    public static void drawIcon(GuiGraphicsExtractor graphics, IconType type, int x, int y, int size, int color) {
        switch (type) {
            case SINGLEPLAYER -> drawSingleplayer(graphics, x, y, size, color);
            case MULTIPLAYER -> drawMultiplayer(graphics, x, y, size, color);
            case GLOBE -> drawGlobe(graphics, x, y, size, color);
            case STORE_CART -> drawStoreCart(graphics, x, y, size, color);
            case COIN -> drawCoin(graphics, x, y, size, color);
            case SHIRT -> drawShirt(graphics, x, y, size, color);
            case CLOSE -> drawClose(graphics, x, y, size, color);
            case HOME -> drawHome(graphics, x, y, size, color);
            case FRIENDS -> drawFriends(graphics, x, y, size, color);
            case GEAR -> drawGear(graphics, x, y, size, color);
            case LANGUAGE -> drawLanguage(graphics, x, y, size, color);
            case ACCESSIBILITY -> drawAccessibility(graphics, x, y, size, color);
            case MENU -> drawMenu(graphics, x, y, size, color);
            case GIFT -> drawGift(graphics, x, y, size, color);
            case HELP -> drawHelp(graphics, x, y, size, color);
            case LINK -> drawLink(graphics, x, y, size, color);
        }
    }

    private static void drawSingleplayer(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        int headSize = Math.max(3, size / 3);
        int headX = x + (size - headSize) / 2;
        int headY = y + 1;
        graphics.fill(headX, headY, headX + headSize, headY + headSize, color);

        int bodyWidth = Math.max(5, size - 4);
        int bodyHeight = Math.max(3, size / 3 + 1);
        int bodyX = x + (size - bodyWidth) / 2;
        int bodyY = y + headSize + 2;
        graphics.fill(bodyX, bodyY, bodyX + bodyWidth, bodyY + bodyHeight, color);
    }

    private static void drawMultiplayer(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        drawSingleplayer(graphics, x - 2, y, size - 2, color);
        drawSingleplayer(graphics, x + 3, y + 1, size - 2, color);
    }

    private static void drawGlobe(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x + 2, y, x + size - 2, y + size, color);
        graphics.fill(x, y + 2, x + size, y + size - 2, color);
        int bg = ARGB.color(220, 20, 23, 31);
        graphics.fill(x + 1, y + size / 2 - 1, x + size - 1, y + size / 2 + 1, bg);
    }

    private static void drawStoreCart(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 2, x + 4, y + 4, color);
        graphics.fill(x + 3, y + 4, x + size - 1, y + size - 4, color);
        int bg = ARGB.color(220, 20, 83, 45);
        graphics.fill(x + 4, y + 5, x + size - 2, y + size - 6, bg);
        graphics.fill(x + 4, y + size - 3, x + 6, y + size - 1, color);
        graphics.fill(x + size - 4, y + size - 3, x + size - 2, y + size - 1, color);
    }

    private static void drawCoin(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y, x + size - 1, y + size, color);
        graphics.fill(x, y + 1, x + size, y + size - 1, color);
        int inner = ARGB.color(255, 254, 240, 138);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, inner);
    }

    private static void drawShirt(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x + 2, y, x + size - 2, y + size - 1, color);
        graphics.fill(x, y + 1, x + size, y + 5, color);
    }

    private static void drawClose(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            graphics.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            graphics.fill(x + size - 1 - i, y + i, x + size - i, y + i + 1, color);
        }
    }

    private static void drawHome(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        int mid = x + size / 2;
        for (int i = 0; i < size / 2; i++) {
            graphics.fill(mid - i, y + i, mid + i + 1, y + i + 1, color);
        }
        graphics.fill(x + 2, y + size / 2, x + size - 2, y + size - 1, color);
    }

    private static void drawFriends(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        drawMultiplayer(graphics, x, y, size, color);
    }

    private static void drawGear(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, color);
        int hole = ARGB.color(255, 20, 23, 31);
        graphics.fill(x + 3, y + 3, x + size - 3, y + size - 3, hole);
    }

    private static void drawLanguage(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        drawGlobe(graphics, x, y, size, color);
    }

    private static void drawAccessibility(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x + size / 2 - 1, y + 1, x + size / 2 + 2, y + 4, color);
        graphics.fill(x + 1, y + 4, x + size - 1, y + 6, color);
        graphics.fill(x + size / 2 - 1, y + 6, x + size / 2 + 2, y + size - 1, color);
    }

    private static void drawMenu(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 2, x + size - 1, y + 4, color);
        graphics.fill(x + 1, y + size / 2 - 1, x + size - 1, y + size / 2 + 1, color);
        graphics.fill(x + 1, y + size - 4, x + size - 1, y + size - 2, color);
    }

    private static void drawGift(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, color);
        int ribbon = ARGB.color(255, 239, 68, 68);
        graphics.fill(x + size / 2 - 1, y + 1, x + size / 2 + 1, y + size - 1, ribbon);
        graphics.fill(x + 1, y + size / 2 - 1, x + size - 1, y + size / 2 + 1, ribbon);
    }

    private static void drawHelp(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 1, x + size - 1, y + 3, color);
        graphics.fill(x + size - 3, y + 3, x + size - 1, y + 6, color);
        graphics.fill(x + size / 2 - 1, y + 6, x + size / 2 + 1, y + 8, color);
        graphics.fill(x + size / 2 - 1, y + 10, x + size / 2 + 1, y + 12, color);
    }

    private static void drawLink(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 1, x + size - 3, y + 3, color);
        graphics.fill(x + 1, y + 1, x + 3, y + size - 3, color);
        graphics.fill(x + 3, y + size - 5, x + size - 1, y + size - 3, color);
        graphics.fill(x + size - 3, y + 3, x + size - 1, y + size - 3, color);
    }
}
