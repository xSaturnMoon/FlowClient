package com.flowclient.mods.blockbreak;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

public final class BlockBreakProgressRenderer {
    private static final int BAR_HEIGHT = 8;
    private static final int PADDING = 4;

    private BlockBreakProgressRenderer() {
    }

    public static int getWidth(Font font) {
        return getWidth(font, BlockBreakProgressSettings.get(), 0.65f);
    }

    public static int getHeight(Font font) {
        return getHeight(font, BlockBreakProgressSettings.get());
    }

    public static int getWidth(Font font, BlockBreakProgressSettings settings, float progress) {
        int width = settings.barWidth() + PADDING * 2;
        if (settings.showPercentage()) {
            width = Math.max(width, font.width(formatPercent(progress)) + PADDING * 2);
        }
        return width;
    }

    public static int getHeight(Font font, BlockBreakProgressSettings settings) {
        int height = BAR_HEIGHT + PADDING * 2;
        if (settings.showPercentage()) {
            height += font.lineHeight + 2;
        }
        return height;
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font, boolean preview) {
        if (!preview && !BlockBreakProgressMod.isEnabled()) {
            return;
        }

        float progress = preview ? 0.65f : BlockBreakProgressTracker.progress(mc);
        if (!preview && progress <= 0.0f) {
            return;
        }

        BlockBreakProgressSettings settings = BlockBreakProgressSettings.get();
        int width = getWidth(font, settings, progress);
        int height = getHeight(font, settings);
        int x = HudLayoutManager.get().resolveX(HudElement.BLOCK_BREAK_PROGRESS, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.BLOCK_BREAK_PROGRESS, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.BLOCK_BREAK_PROGRESS);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> draw(graphics, font, settings, x, y, width, height, progress));
    }

    private static void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            BlockBreakProgressSettings settings,
            int x,
            int y,
            int width,
            int height,
            float progress
    ) {
        if (settings.showBackground()) {
            int alpha = (settings.backgroundOpacity() * 255) / 100;
            graphics.fill(x, y, x + width, y + height, ARGB.color(alpha, 8, 8, 12));
        }

        int contentY = y + PADDING;
        if (settings.showPercentage()) {
            graphics.text(font, formatPercent(progress), x + PADDING, contentY, 0xFFFFFFFF, true);
            contentY += font.lineHeight + 2;
        }

        int barX = x + PADDING;
        int barY = contentY;
        int barWidth = settings.barWidth();
        graphics.fill(barX, barY, barX + barWidth, barY + BAR_HEIGHT, ARGB.color(255, 30, 30, 30));
        int fillWidth = Math.max(1, (int) (barWidth * progress));
        graphics.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, ARGB.color(255, 76, 175, 80));
        graphics.fill(barX, barY, barX + barWidth, barY + 1, ARGB.color(255, 0, 0, 0));
        graphics.fill(barX, barY + BAR_HEIGHT - 1, barX + barWidth, barY + BAR_HEIGHT, ARGB.color(255, 0, 0, 0));
        graphics.fill(barX, barY, barX + 1, barY + BAR_HEIGHT, ARGB.color(255, 0, 0, 0));
        graphics.fill(barX + barWidth - 1, barY, barX + barWidth, barY + BAR_HEIGHT, ARGB.color(255, 0, 0, 0));
    }

    private static String formatPercent(float progress) {
        return Math.round(progress * 100.0f) + "%";
    }
}
