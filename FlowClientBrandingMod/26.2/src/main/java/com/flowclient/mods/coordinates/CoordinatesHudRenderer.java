package com.flowclient.mods.coordinates;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;

public final class CoordinatesHudRenderer {
    private CoordinatesHudRenderer() {
    }

    public static int getWidth(Font font) {
        return getWidth(font, CoordinatesHudSettings.get(), "XYZ 123 64 -456 · NORTH");
    }

    public static int getHeight(Font font) {
        return getHeight(font, CoordinatesHudSettings.get());
    }

    public static int getWidth(Font font, CoordinatesHudSettings settings, String text) {
        return font.width(text) + settings.paddingX() * 2;
    }

    public static int getHeight(Font font, CoordinatesHudSettings settings) {
        return font.lineHeight + settings.paddingY() * 2;
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font, boolean preview) {
        if (!preview && !CoordinatesHudMod.isEnabled()) {
            return;
        }

        if (!preview && mc.player == null) {
            return;
        }

        CoordinatesHudSettings settings = CoordinatesHudSettings.get();
        String text = preview
                ? settings.format(123, 64, -456, net.minecraft.core.Direction.NORTH)
                : formatLive(mc, settings);
        int width = getWidth(font, settings, text);
        int height = getHeight(font, settings);
        int x = HudLayoutManager.get().resolveX(HudElement.COORDINATES_HUD, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.COORDINATES_HUD, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.COORDINATES_HUD);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> {
            if (settings.showBackground()) {
                int alpha = (settings.backgroundOpacity() * 255) / 100;
                graphics.fill(x, y, x + width, y + height, ARGB.color(alpha, 8, 8, 12));
            }
            graphics.text(
                    font,
                    text,
                    x + settings.paddingX(),
                    y + settings.paddingY(),
                    0xFFE0E6F0,
                    settings.textShadow()
            );
        });
    }

    private static String formatLive(Minecraft mc, CoordinatesHudSettings settings) {
        BlockPos pos = mc.player.blockPosition();
        return settings.format(pos.getX(), pos.getY(), pos.getZ(), mc.player.getDirection());
    }
}
