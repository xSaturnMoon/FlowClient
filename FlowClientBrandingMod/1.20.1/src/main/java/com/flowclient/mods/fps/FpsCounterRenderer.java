package com.flowclient.mods.fps;

import com.flowclient.compat.FlowGfx;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class FpsCounterRenderer {
    private FpsCounterRenderer() {
    }

    public static int getWidth(Font font) {
        return getWidth(font, FpsCounterSettings.get(), 999);
    }

    public static int getHeight(Font font) {
        return getHeight(font, FpsCounterSettings.get());
    }

    public static int getWidth(Font font, FpsCounterSettings settings, int fps) {
        return font.width(buildText(settings, fps)) + settings.paddingX() * 2;
    }

    public static int getHeight(Font font, FpsCounterSettings settings) {
        return font.lineHeight + settings.paddingY() * 2;
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font, boolean preview) {
        if (!preview && !FpsCounterMod.isEnabled()) {
            return;
        }

        FpsCounterSettings settings = FpsCounterSettings.get();
        int rawFps = preview ? 144 : mc.getFps();
        int fps = settings.smoothing() && !preview ? FpsSmoother.smooth(rawFps) : rawFps;

        int width = getWidth(font, settings, fps);
        int height = getHeight(font, settings);
        int x = HudLayoutManager.get().resolveX(HudElement.FPS_COUNTER, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.FPS_COUNTER, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.FPS_COUNTER);
        String text = buildText(settings, fps);
        int color = resolveColor(settings, fps, preview);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> {
            if (settings.showBackground()) {
                int alpha = (int) (settings.backgroundOpacity() / 100.0f * 255.0f) << 24;
                graphics.fill(x, y, x + width, y + height, alpha | 0x08080C);
            }

            if (settings.showOutline()) {
                graphics.fill(x, y, x + width, y + 1, 0xFF4A9EE0);
                graphics.fill(x, y + height - 1, x + width, y + height, 0xFF1E2A40);
                graphics.fill(x, y, x + 1, y + height, 0xFF4A9EE0);
                graphics.fill(x + width - 1, y, x + width, y + height, 0xFF1E2A40);
            }

            if (settings.boldNumbers()) {
                FlowGfx.text(graphics, font, text, settings.paddingX() + x + 1, settings.paddingY() + y, color, false);
            }

            FlowGfx.text(graphics, font, text, settings.paddingX() + x, settings.paddingY() + y, color, settings.textShadow());
        });
    }

    static String buildText(FpsCounterSettings settings, int fps) {
        float ms = fps > 0 ? 1000.0f / fps : 0.0f;
        String msText = String.format("%.1f ms", ms);
        return switch (settings.format()) {
            case FPS_ONLY -> Integer.toString(fps);
            case FPS_SUFFIX -> fps + " FPS";
            case FPS_PREFIX -> "FPS " + fps;
            case FRAME_TIME -> msText;
            case FPS_AND_MS -> fps + " FPS (" + msText + ")";
        };
    }

    private static int resolveColor(FpsCounterSettings settings, int fps, boolean preview) {
        return switch (settings.colorMode()) {
            case FIXED_WHITE -> 0xFFFFFFFF;
            case FIXED_GREEN -> 0xFF55FF55;
            case ACCENT -> 0xFF4A9EE0;
            case RAINBOW -> rainbowColor(preview ? 144 : (int) (System.currentTimeMillis() / 50L % 360L));
            case DYNAMIC -> dynamicColor(settings, fps);
        };
    }

    private static int dynamicColor(FpsCounterSettings settings, int fps) {
        if (fps >= settings.highThreshold()) {
            return 0xFF55FF55;
        }
        if (fps >= settings.midThreshold()) {
            return 0xFFFFFF55;
        }
        if (fps >= settings.lowThreshold()) {
            return 0xFFFFAA55;
        }
        return 0xFFFF5555;
    }

    private static int rainbowColor(int hue) {
        float h = hue / 360.0f;
        int rgb = java.awt.Color.HSBtoRGB(h, 0.85f, 1.0f) & 0xFFFFFF;
        return 0xFF000000 | rgb;
    }
}
