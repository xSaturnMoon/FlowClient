package com.flowclient.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.flowclient.compat.FlowArgb;

public final class UiSprites {
    public static final ResourceLocation ICON_SINGLEPLAYER = new ResourceLocation(
            "flowclient", "textures/gui/icon_singleplayer"
    );

    public static void blitIcon(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
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

    public static void drawIcon(GuiGraphics graphics, IconType type, int x, int y, int size, int color) {
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

    private static void drawSingleplayer(GuiGraphics graphics, int x, int y, int size, int color) {
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

    private static void drawMultiplayer(GuiGraphics graphics, int x, int y, int size, int color) {
        drawSingleplayer(graphics, x - 2, y, size - 2, color);
        drawSingleplayer(graphics, x + 3, y + 1, size - 2, color);
    }

    private static void drawGlobe(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 2, y, x + size - 2, y + size, color);
        graphics.fill(x, y + 2, x + size, y + size - 2, color);
        int bg = FlowArgb.color(220, 20, 23, 31);
        graphics.fill(x + 1, y + size / 2 - 1, x + size - 1, y + size / 2 + 1, bg);
    }

    private static void drawStoreCart(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 2, x + 4, y + 4, color);
        graphics.fill(x + 3, y + 4, x + size - 1, y + size - 4, color);
        int bg = FlowArgb.color(220, 20, 83, 45);
        graphics.fill(x + 4, y + 5, x + size - 2, y + size - 6, bg);
        graphics.fill(x + 4, y + size - 3, x + 6, y + size - 1, color);
        graphics.fill(x + size - 4, y + size - 3, x + size - 2, y + size - 1, color);
    }

    private static void drawCoin(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y, x + size - 1, y + size, color);
        graphics.fill(x, y + 1, x + size, y + size - 1, color);
        int inner = FlowArgb.color(255, 254, 240, 138);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, inner);
    }

    private static void drawShirt(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 2, y, x + size - 2, y + size - 1, color);
        graphics.fill(x, y + 1, x + size, y + 5, color);
    }

    private static void drawClose(GuiGraphics graphics, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            graphics.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            graphics.fill(x + size - 1 - i, y + i, x + size - i, y + i + 1, color);
        }
    }

    private static void drawHome(GuiGraphics graphics, int x, int y, int size, int color) {
        int mid = x + size / 2;
        for (int i = 0; i < size / 2; i++) {
            graphics.fill(mid - i, y + i, mid + i + 1, y + i + 1, color);
        }
        graphics.fill(x + 2, y + size / 2, x + size - 2, y + size - 1, color);
    }

    private static void drawFriends(GuiGraphics graphics, int x, int y, int size, int color) {
        drawMultiplayer(graphics, x, y, size, color);
    }

    private static void drawGear(GuiGraphics graphics, int x, int y, int size, int color) {
        drawSettingsCog(graphics, x, y, size, color);
    }

    public static void drawSettingsCog(GuiGraphics graphics, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int outer = Math.max(3, size / 2 - 1);
        int inner = Math.max(2, outer - 2);

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(22.5 + i * 45.0);
            int toothX = cx + (int) Math.round(Math.cos(angle) * (outer - 1));
            int toothY = cy + (int) Math.round(Math.sin(angle) * (outer - 1));
            graphics.fill(toothX - 1, toothY - 1, toothX + 2, toothY + 2, color);
        }

        graphics.fill(cx - outer, cy - outer, cx + outer + 1, cy + outer + 1, color);
        int hole = FlowArgb.color(255, 40, 40, 40);
        graphics.fill(cx - inner, cy - inner, cx + inner + 1, cy + inner + 1, hole);
    }

    private static void drawLanguage(GuiGraphics graphics, int x, int y, int size, int color) {
        drawGlobe(graphics, x, y, size, color);
    }

    private static void drawAccessibility(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + size / 2 - 1, y + 1, x + size / 2 + 2, y + 4, color);
        graphics.fill(x + 1, y + 4, x + size - 1, y + 6, color);
        graphics.fill(x + size / 2 - 1, y + 6, x + size / 2 + 2, y + size - 1, color);
    }

    private static void drawMenu(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 2, x + size - 1, y + 4, color);
        graphics.fill(x + 1, y + size / 2 - 1, x + size - 1, y + size / 2 + 1, color);
        graphics.fill(x + 1, y + size - 4, x + size - 1, y + size - 2, color);
    }

    private static void drawGift(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, color);
        int ribbon = FlowArgb.color(255, 239, 68, 68);
        graphics.fill(x + size / 2 - 1, y + 1, x + size / 2 + 1, y + size - 1, ribbon);
        graphics.fill(x + 1, y + size / 2 - 1, x + size - 1, y + size / 2 + 1, ribbon);
    }

    private static void drawHelp(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 1, x + size - 1, y + 3, color);
        graphics.fill(x + size - 3, y + 3, x + size - 1, y + 6, color);
        graphics.fill(x + size / 2 - 1, y + 6, x + size / 2 + 1, y + 8, color);
        graphics.fill(x + size / 2 - 1, y + 10, x + size / 2 + 1, y + 12, color);
    }

    private static void drawLink(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 1, y + 1, x + size - 3, y + 3, color);
        graphics.fill(x + 1, y + 1, x + 3, y + size - 3, color);
        graphics.fill(x + 3, y + size - 5, x + size - 1, y + size - 3, color);
        graphics.fill(x + size - 3, y + 3, x + size - 1, y + size - 3, color);
    }
}
