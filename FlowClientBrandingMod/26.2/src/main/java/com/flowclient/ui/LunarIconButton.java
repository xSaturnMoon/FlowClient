package com.flowclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public final class LunarIconButton extends AbstractButton {
    // Single character labels for each icon type
    private static final String[] ICON_CHARS = {
        "\u2302", // HOME: ⌂
        "\u2665", // SHIRT: ♥ (cosmetics)
        "\u2764", // MESSAGE
        "\u0041", // FRIENDS: A (avatar)
        "\u2699", // GEAR: ⚙
        "\u0047", // LANGUAGE: G (globe)
        "\u0041", // ACCESSIBILITY
        "\u2261", // MENU: ≡
        "\u00D7", // CLOSE: ×
        "\u25CE", // COIN
        "\u0054", // SHIRT2
        "\u263D", // MOON
        "\u0048", // HOME2
        "\u2665", // FRIENDS2
        "\u2699", // GEAR2
        "\u25A0", // DEFAULT
    };

    private final UiSprites.IconType iconType;
    private final Runnable onPress;

    public LunarIconButton(int x, int y, int w, int h, UiSprites.IconType iconType, Runnable onPress) {
        super(x, y, w, h, Component.empty());
        this.iconType = iconType;
        this.onPress = onPress;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float pt) {
        boolean hov = isHoveredOrFocused();

        // Visible background - notably lighter than the dark pill background
        int bg = hov ? ARGB.color(255, 60, 70, 95) : ARGB.color(200, 35, 42, 58);
        int fg = hov ? ARGB.color(255, 255, 255, 255) : ARGB.color(220, 180, 195, 215);

        UiColors.fillRounded(g, getX(), getY(), width, height, 5, bg);

        Font font = Minecraft.getInstance().font;
        String label = getIconChar();
        int lw = font.width(label);
        int lx = getX() + (width - lw) / 2;
        int ly = getY() + (height - font.lineHeight) / 2 + 1;
        g.text(font, label, lx, ly, fg);
    }

    private String getIconChar() {
        return switch (iconType) {
            case HOME -> "\u2302";        // ⌂
            case SHIRT -> "C";
            case SINGLEPLAYER -> "S";
            case MULTIPLAYER -> "M";
            case FRIENDS -> "F";
            case GEAR -> "*";
            case LANGUAGE -> "L";
            case ACCESSIBILITY -> "A";
            case MENU -> "=";
            case CLOSE -> "\u00D7";       // ×
            case COIN -> "$";
            case GLOBE -> "D";
            case STORE_CART -> "B";
            default -> "?";
        };
    }

    @Override public void onPress(InputWithModifiers input) { if (onPress != null) onPress.run(); }
    @Override protected void updateWidgetNarration(NarrationElementOutput o) { defaultButtonNarrationText(o); }
}
