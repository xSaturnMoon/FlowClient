package com.flowclient.mods.blockspeed;

import com.flowclient.compat.FlowGfx;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class BlockSpeedRenderer {
    private BlockSpeedRenderer() {
    }

    public static int getWidth(Font font) {
        return getWidth(font, BlockSpeedSettings.get(), layoutSampleValue());
    }

    public static int getHeight(Font font) {
        return getHeight(font, BlockSpeedSettings.get());
    }

    public static int getWidth(Font font, BlockSpeedSettings settings, float blocksPerSecond) {
        return font.width(settings.formatValue(blocksPerSecond)) + settings.paddingX() * 2;
    }

    public static int getHeight(Font font, BlockSpeedSettings settings) {
        return font.lineHeight + settings.paddingY() * 2;
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font, boolean preview) {
        if (!preview && !BlockSpeedMod.isEnabled()) {
            return;
        }

        BlockSpeedSettings settings = BlockSpeedSettings.get();
        float blocksPerSecond = preview ? 5.6f : BlockSpeedTracker.getBlocksPerSecond();

        String text = settings.formatValue(blocksPerSecond);
        int width = font.width(text) + settings.paddingX() * 2;
        int height = font.lineHeight + settings.paddingY() * 2;
        int x = HudLayoutManager.get().resolveX(HudElement.BLOCK_SPEED, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.BLOCK_SPEED, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.BLOCK_SPEED);
        int color = resolveColor(settings, blocksPerSecond, preview);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> {
            if (settings.showBackground()) {
                int alpha = (int) (settings.backgroundOpacity() / 100.0f * 255.0f) << 24;
                graphics.fill(x, y, x + width, y + height, alpha | 0x08080C);
            }
            FlowGfx.text(graphics, font, text, settings.paddingX() + x, settings.paddingY() + y, color, settings.textShadow());
        });
    }

    private static float layoutSampleValue() {
        BlockSpeedSettings settings = BlockSpeedSettings.get();
        return switch (settings.format()) {
            case BPS_ONLY, BPS_SUFFIX, BPS_PREFIX -> 99.9f;
            case BLOCKS_ONLY -> 99f;
        };
    }

    private static int resolveColor(BlockSpeedSettings settings, float blocksPerSecond, boolean preview) {
        return switch (settings.colorMode()) {
            case FIXED_WHITE -> 0xFFFFFFFF;
            case FIXED_GREEN -> 0xFF55FF55;
            case ACCENT -> 0xFF4A9EE0;
            case DYNAMIC -> dynamicColor(preview ? 5.6f : blocksPerSecond);
        };
    }

    private static int dynamicColor(float blocksPerSecond) {
        if (blocksPerSecond >= 7.0f) {
            return 0xFF55FF55;
        }
        if (blocksPerSecond >= 5.2f) {
            return 0xFFFFFF55;
        }
        if (blocksPerSecond >= 3.5f) {
            return 0xFFFFAA55;
        }
        if (blocksPerSecond >= 0.05f) {
            return 0xFFB8BEC8;
        }
        return 0xFF6E7681;
    }
}
