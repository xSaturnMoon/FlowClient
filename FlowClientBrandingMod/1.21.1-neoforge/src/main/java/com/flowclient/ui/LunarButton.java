package com.flowclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import com.flowclient.util.FlowArgb;

public final class LunarButton extends AbstractButton {
    public enum Style { DEFAULT, STORE, QUEST_ACTION }

    private final Runnable onPress;
    private final Style style;
    private final String badgeText;

    public LunarButton(int x, int y, int w, int h, Component message, Style style, String badgeText, Runnable onPress) {
        super(x, y, w, h, message);
        this.style = style;
        this.badgeText = badgeText;
        this.onPress = onPress;
    }

    public LunarButton(int x, int y, int w, int h, Component message, Runnable onPress) {
        this(x, y, w, h, message, Style.DEFAULT, null, onPress);
    }

    // Kept for quest card compatibility
    public LunarButton(int x, int y, int w, int h, Component message, UiSprites.IconType ignored, Style style, String badge, Runnable onPress) {
        this(x, y, w, h, message, style, badge, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        boolean hov = isHoveredOrFocused();
        Font font = Minecraft.getInstance().font;

        int bgColor, textColor, borderColor = 0;
        int radius = 8;

        if (style == Style.STORE) {
            bgColor = hov ? FlowArgb.color(255, 22, 101, 52) : FlowArgb.color(220, 16, 64, 36);
            textColor = FlowArgb.color(255, 74, 222, 128);
            borderColor = FlowArgb.color(255, 34, 197, 94);
        } else if (style == Style.QUEST_ACTION) {
            bgColor = hov ? FlowArgb.color(255, 163, 230, 53) : FlowArgb.color(255, 132, 204, 22);
            textColor = FlowArgb.color(255, 15, 23, 42);
            radius = 6;
        } else {
            bgColor = hov ? FlowArgb.color(255, 45, 52, 70) : FlowArgb.color(220, 22, 26, 36);
            textColor = hov ? FlowArgb.color(255, 255, 255, 255) : FlowArgb.color(255, 200, 208, 220);
        }

        UiColors.fillRounded(g, getX(), getY(), width, height, radius, bgColor);

        // Left accent line for default buttons
        if (style == Style.DEFAULT) {
            int accent = hov ? FlowArgb.color(255, 99, 163, 255) : FlowArgb.color(180, 66, 120, 200);
            g.fill(getX(), getY() + 4, getX() + 2, getY() + height - 4, accent);
        }

        if (borderColor != 0) {
            UiColors.drawRoundedOutline(g, getX(), getY(), width, height, radius, 1, borderColor);
        }

        Component label = this.getMessage();
        int textW = font.width(label);

        // Badge text
        int badgeW = 0;
        if (badgeText != null) {
            badgeW = font.width(badgeText) + 10;
        }

        int totalW = textW + (badgeW > 0 ? 6 + badgeW : 0);
        int textX = getX() + (width - totalW) / 2;
        int textY = getY() + (height - font.lineHeight) / 2 + 1;
        g.drawString(font, label, textX, textY, textColor);

        if (badgeText != null) {
            int bx = textX + textW + 6;
            int by = getY() + (height - 13) / 2;
            UiColors.fillRounded(g, bx, by, badgeW, 13, 4, FlowArgb.color(180, 40, 50, 70));
            g.drawString(font, badgeText, bx + 5, by + (13 - font.lineHeight) / 2 + 1, FlowArgb.color(255, 148, 163, 184));
        }
    }

    @Override public void onPress() { if (onPress != null) onPress.run(); }
    @Override public void updateWidgetNarration(NarrationElementOutput o) { defaultButtonNarrationText(o); }
}
