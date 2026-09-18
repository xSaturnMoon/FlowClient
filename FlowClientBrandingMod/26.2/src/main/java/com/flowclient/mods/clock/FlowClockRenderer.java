package com.flowclient.mods.clock;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import java.time.LocalDateTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class FlowClockRenderer {
    private FlowClockRenderer() {
    }

    public static int getWidth(Font font) {
        return getWidth(font, FlowClockSettings.get());
    }

    public static int getHeight(Font font) {
        return getHeight(font, FlowClockSettings.get());
    }

    public static int getWidth(Font font, FlowClockSettings settings) {
        return font.width(settings.formatNow()) + settings.paddingX() * 2;
    }

    public static int getHeight(Font font, FlowClockSettings settings) {
        return font.lineHeight + settings.paddingY() * 2;
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font, boolean preview) {
        if (!preview && !FlowClockMod.isEnabled()) {
            return;
        }

        FlowClockSettings settings = FlowClockSettings.get();
        String text = preview ? settings.formatTime(LocalDateTime.of(2026, 7, 27, 14, 30, 45)) : settings.formatNow();
        int width = font.width(text) + settings.paddingX() * 2;
        int height = font.lineHeight + settings.paddingY() * 2;
        int x = HudLayoutManager.get().resolveX(HudElement.FLOW_CLOCK, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.FLOW_CLOCK, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.FLOW_CLOCK);
        int color = settings.resolveColor();

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> {
            if (settings.showBackground()) {
                int alpha = (int) (settings.backgroundOpacity() / 100.0f * 255.0f) << 24;
                graphics.fill(x, y, x + width, y + height, alpha | 0x08080C);
            }
            graphics.text(font, text, settings.paddingX() + x, settings.paddingY() + y, color, settings.textShadow());
        });
    }
}
