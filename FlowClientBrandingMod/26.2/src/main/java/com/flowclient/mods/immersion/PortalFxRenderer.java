package com.flowclient.mods.immersion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class PortalFxRenderer {
    private PortalFxRenderer() {
    }

    public static void render(Minecraft client, GuiGraphicsExtractor graphics, Font font) {
        if (!PortalFxMod.isEnabled() || client.player == null) {
            return;
        }

        float intensity = PortalFxController.intensity();
        if (intensity <= 0.0F) {
            return;
        }

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        int alpha = (int) (intensity * 170.0F) << 24;

        int tint = switch (PortalFxController.activeEffect()) {
            case NETHER -> alpha | 0x00AA2200;
            case END -> alpha | 0x006B1F8A;
            case OVERWORLD -> alpha | 0x00C8E8FF;
            default -> 0;
        };

        if (tint != 0) {
            graphics.fill(0, 0, width, height, tint);
            int edge = (int) (intensity * 90.0F) << 24;
            graphics.fill(0, 0, width, 18, edge | 0x00000000);
            graphics.fill(0, height - 18, width, height, edge | 0x00000000);
            graphics.fill(0, 0, 18, height, edge | 0x00000000);
            graphics.fill(width - 18, 0, width, height, edge | 0x00000000);
        }

        if (PortalFxController.showTitle()) {
            String title = switch (PortalFxController.activeEffect()) {
                case NETHER -> "NETHER";
                case END -> "THE END";
                case OVERWORLD -> "OVERWORLD";
                default -> "";
            };
            if (!title.isEmpty()) {
                int textAlpha = (int) (Math.min(1.0F, intensity + 0.25F) * 255.0F) << 24;
                graphics.centeredText(font, Component.literal(title), width / 2, height / 2 - 24, textAlpha | 0x00FFFFFF);
            }
        }
    }
}
