package com.flowclient.mods.ping;

import com.flowclient.compat.FlowGfx;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import com.flowclient.compat.FlowArgb;

public final class PingHudRenderer {
    private PingHudRenderer() {
    }

    public static int getWidth(Font font) {
        return getWidth(font, PingHudSettings.get(), "Ping 42ms");
    }

    public static int getHeight(Font font) {
        return getHeight(font, PingHudSettings.get());
    }

    public static int getWidth(Font font, PingHudSettings settings, String text) {
        return font.width(text) + settings.paddingX() * 2;
    }

    public static int getHeight(Font font, PingHudSettings settings) {
        return font.lineHeight + settings.paddingY() * 2;
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font, boolean preview) {
        if (!preview && !PingHudMod.isEnabled()) {
            return;
        }

        PingHudSettings settings = PingHudSettings.get();
        int ping = preview ? 42 : resolvePing(mc);
        String text = settings.format(ping);
        int width = getWidth(font, settings, text);
        int height = getHeight(font, settings);
        int x = HudLayoutManager.get().resolveX(HudElement.PING_HUD, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.PING_HUD, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.PING_HUD);
        int color = settings.resolveColor(ping);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> {
            if (settings.showBackground()) {
                int alpha = (settings.backgroundOpacity() * 255) / 100;
                graphics.fill(x, y, x + width, y + height, FlowArgb.color(alpha, 8, 8, 12));
            }
            FlowGfx.text(graphics, 
                    font,
                    text,
                    x + settings.paddingX(),
                    y + settings.paddingY(),
                    color,
                    settings.textShadow()
            );
        });
    }

    private static int resolvePing(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            return -1;
        }

        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info != null ? info.getLatency() : -1;
    }
}
