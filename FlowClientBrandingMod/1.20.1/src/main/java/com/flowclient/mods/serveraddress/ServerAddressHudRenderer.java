package com.flowclient.mods.serveraddress;

import com.flowclient.compat.FlowGfx;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.flowclient.compat.FlowArgb;

public final class ServerAddressHudRenderer {
    private ServerAddressHudRenderer() {
    }

    public static int getWidth(Font font) {
        return getWidth(font, ServerAddressHudSettings.get(), "mc.example.net");
    }

    public static int getHeight(Font font) {
        return getHeight(font, ServerAddressHudSettings.get());
    }

    public static int getWidth(Font font, ServerAddressHudSettings settings, String text) {
        return font.width(text) + settings.paddingX() * 2;
    }

    public static int getHeight(Font font, ServerAddressHudSettings settings) {
        return font.lineHeight + settings.paddingY() * 2;
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font, boolean preview) {
        if (!preview && !ServerAddressHudMod.isEnabled()) {
            return;
        }

        ServerAddressHudSettings settings = ServerAddressHudSettings.get();
        if (!preview && settings.hideSingleplayer() && mc.isLocalServer()) {
            return;
        }

        String text = preview
                ? "mc.hypixel.net"
                : ServerAddressResolver.resolve(mc, settings.displayMode());
        int width = getWidth(font, settings, text);
        int height = getHeight(font, settings);
        int x = HudLayoutManager.get().resolveX(HudElement.SERVER_ADDRESS_HUD, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.SERVER_ADDRESS_HUD, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.SERVER_ADDRESS_HUD);

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
                    0xFFE0E6F0,
                    settings.textShadow()
            );
        });
    }
}
