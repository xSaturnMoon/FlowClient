package com.flowclient.mods.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class DamageIndicatorRenderer {
    private DamageIndicatorRenderer() {
    }

    public static void render(Minecraft client, GuiGraphicsExtractor graphics, Font font) {
        if (!DamageIndicatorMod.isEnabled() || client.level == null || client.gameRenderer == null) {
            return;
        }

        DamageIndicatorTracker.advancePopups();
        DamageIndicatorSettings settings = DamageIndicatorSettings.get();
        long now = System.currentTimeMillis();

        for (DamageIndicatorTracker.DamagePopup popup : DamageIndicatorTracker.popups()) {
            Entity entity = client.level.getEntity(popup.entityId());
            Vec3 anchor = entity != null
                    ? entity.position().add(0.0D, entity.getBbHeight() + 0.35D, 0.0D)
                    : popup.worldPosition();
            Vec3 projected = client.gameRenderer.projectPointToScreen(anchor.add(0.0D, popup.rise(), 0.0D));
            if (projected.z <= 0.0D || projected.z >= 1.0D) {
                continue;
            }

            int screenWidth = client.getWindow().getGuiScaledWidth();
            int screenHeight = client.getWindow().getGuiScaledHeight();
            int x = (int) ((projected.x + 1.0D) * 0.5D * screenWidth);
            int y = (int) ((1.0D - projected.y) * 0.5D * screenHeight);

            float life = (now - popup.spawnTimeMs()) / (float) settings.durationMs();
            int alpha = (int) ((1.0F - life) * 255.0F) << 24;
            int color = (alpha & 0xFF000000) | (settings.textColor() & 0x00FFFFFF);
            if (popup.healing()) {
                color = (alpha & 0xFF000000) | 0x0055FF55;
            }

            String text = popup.healing()
                    ? "+" + formatAmount(popup.amount())
                    : formatAmount(popup.amount());
            x -= font.width(text) / 2;
            graphics.text(font, Component.literal(text), x, y, color, true);
        }
    }

    private static String formatAmount(float amount) {
        if (amount >= 10.0F) {
            return String.valueOf(Math.round(amount));
        }
        return String.format("%.1f", amount);
    }
}
