package com.flowclient.mods.health;

import com.flowclient.compat.FlowGfx;
import com.flowclient.compat.WorldProjection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HealthBarRenderer {
    private HealthBarRenderer() {
    }

    public static void render(Minecraft client, GuiGraphics graphics, Font font) {
        if (!HealthBarMod.isEnabled() || client.level == null || client.gameRenderer == null || client.player == null) {
            return;
        }

        HealthBarSettings settings = HealthBarSettings.get();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        double maxDistanceSq = settings.maxDistance() * (double) settings.maxDistance();

        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }

            if (!shouldRender(client, living, settings, maxDistanceSq)) {
                continue;
            }

            Vec3 anchor = living.position().add(0.0D, living.getBbHeight() + settings.yOffset(), 0.0D);
            Vec3 projected = WorldProjection.projectPointToScreen(client, anchor);
            if (projected.z <= 0.0D || projected.z >= 1.0D) {
                continue;
            }

            int screenX = (int) ((projected.x + 1.0D) * 0.5D * screenWidth);
            int screenY = (int) ((1.0D - projected.y) * 0.5D * screenHeight);
            float distanceScale = scaleForDistance(client.player.distanceTo(living), settings.maxDistance());
            drawEntityBar(graphics, font, settings, living, screenX, screenY, distanceScale);
        }
    }

    public static void renderPreview(GuiGraphics graphics, Font font, int centerX, int centerY) {
        HealthBarSettings settings = HealthBarSettings.get();
        drawBar(graphics, font, settings, centerX, centerY, 1.0F, 0.62F, 6.0F, 20.0F);
    }

    private static boolean shouldRender(
            Minecraft client,
            LivingEntity living,
            HealthBarSettings settings,
            double maxDistanceSq
    ) {
        if (!living.isAlive() || living.isInvisible()) {
            return false;
        }

        if (living == client.player && !settings.showOnSelf()) {
            return false;
        }

        if (client.player.distanceToSqr(living) > maxDistanceSq) {
            return false;
        }

        if (!matchesTarget(living, settings.target())) {
            return false;
        }

        if (settings.hideFullHealth()
                && living.getHealth() >= living.getMaxHealth()
                && living.getAbsorptionAmount() <= 0.0F) {
            return false;
        }

        return true;
    }

    private static boolean matchesTarget(LivingEntity living, HealthBarTarget target) {
        return switch (target) {
            case ALL -> true;
            case PLAYERS -> living instanceof Player;
            case ANIMALS -> living instanceof Animal;
            case MONSTERS -> living instanceof Monster;
        };
    }

    private static float scaleForDistance(float distance, int maxDistance) {
        float ratio = Math.min(1.0F, distance / Math.max(1, maxDistance));
        return 1.0F - ratio * 0.25F;
    }

    private static void drawEntityBar(
            GuiGraphics graphics,
            Font font,
            HealthBarSettings settings,
            LivingEntity living,
            int screenX,
            int screenY,
            float distanceScale
    ) {
        float maxHealth = Math.max(1.0F, living.getMaxHealth());
        float healthRatio = Math.min(1.0F, living.getHealth() / maxHealth);
        float absorption = settings.showAbsorption() ? living.getAbsorptionAmount() : 0.0F;
        drawBar(graphics, font, settings, screenX, screenY, distanceScale, healthRatio, absorption, maxHealth);
    }

    private static void drawBar(
            GuiGraphics graphics,
            Font font,
            HealthBarSettings settings,
            int centerX,
            int centerY,
            float distanceScale,
            float healthRatio,
            float absorption,
            float maxHealth
    ) {
        int barWidth = Math.max(16, Math.round(settings.barWidth() * distanceScale));
        int barHeight = Math.max(3, Math.round(settings.barHeight() * distanceScale));
        int barX = centerX - barWidth / 2;
        int barY = centerY;

        if (settings.showNumeric()) {
            String text = String.format("%.0f/%.0f", healthRatio * maxHealth, maxHealth);
            int textWidth = font.width(text);
            FlowGfx.text(graphics, font, Component.literal(text), centerX - textWidth / 2, barY - font.lineHeight - 1, 0xFFFFFFFF, true);
        }

        switch (settings.style()) {
            case SEGMENTS -> drawSegments(graphics, barX, barY, barWidth, barHeight, healthRatio);
            case OUTLINED -> drawOutlined(graphics, barX, barY, barWidth, barHeight, healthRatio);
            case GRADIENT -> drawGradient(graphics, barX, barY, barWidth, barHeight, healthRatio);
            case COMPACT -> drawClassic(graphics, barX, barY, barWidth, Math.max(3, barHeight - 1), healthRatio, 0xAA101018, true);
            case CLASSIC -> drawClassic(graphics, barX, barY, barWidth, barHeight, healthRatio, 0xCC101018, false);
        }

        if (absorption > 0.0F && settings.showAbsorption()) {
            int absorptionHeight = Math.max(2, barHeight - 1);
            int absorptionY = barY - absorptionHeight - 1;
            float absorptionRatio = Math.min(1.0F, absorption / 20.0F);
            drawClassic(graphics, barX, absorptionY, barWidth, absorptionHeight, absorptionRatio, 0xAA2A2208, false, 0xFFFFD54F);
        }
    }

    private static void drawClassic(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            float ratio,
            int backgroundColor,
            boolean thinBorder
    ) {
        drawClassic(graphics, x, y, width, height, ratio, backgroundColor, thinBorder, dynamicColor(ratio));
    }

    private static void drawClassic(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            float ratio,
            int backgroundColor,
            boolean thinBorder,
            int fillColor
    ) {
        graphics.fill(x, y, x + width, y + height, backgroundColor);
        if (thinBorder) {
            graphics.fill(x, y, x + width, y + 1, 0x66FFFFFF);
        }
        int fillWidth = Math.max(1, Math.round(width * ratio));
        graphics.fill(x, y, x + fillWidth, y + height, 0xFF000000 | (fillColor & 0xFFFFFF));
    }

    private static void drawGradient(GuiGraphics graphics, int x, int y, int width, int height, float ratio) {
        graphics.fill(x, y, x + width, y + height, 0xCC101018);
        int fillWidth = Math.max(1, Math.round(width * ratio));
        int leftColor = 0xFFFF5555;
        int rightColor = 0xFF55FF55;
        for (int i = 0; i < fillWidth; i++) {
            float segmentRatio = fillWidth <= 1 ? 1.0F : i / (float) (fillWidth - 1);
            int color = lerpColor(leftColor, rightColor, segmentRatio);
            graphics.fill(x + i, y, x + i + 1, y + height, 0xFF000000 | color);
        }
    }

    private static void drawOutlined(GuiGraphics graphics, int x, int y, int width, int height, float ratio) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
        drawClassic(graphics, x, y, width, height, ratio, 0xCC202028, false);
    }

    private static void drawSegments(GuiGraphics graphics, int x, int y, int width, int height, float ratio) {
        int segments = 10;
        int gap = 1;
        int segmentWidth = Math.max(2, (width - gap * (segments - 1)) / segments);
        int filled = Math.round(ratio * segments);

        for (int i = 0; i < segments; i++) {
            int segmentX = x + i * (segmentWidth + gap);
            int color = i < filled ? dynamicColor((i + 1) / (float) segments) : 0x55202028;
            graphics.fill(segmentX, y, segmentX + segmentWidth, y + height, 0xFF000000 | (color & 0xFFFFFF));
        }
    }

    private static int dynamicColor(float ratio) {
        if (ratio > 0.6F) {
            return 0xFF55FF55;
        }
        if (ratio > 0.3F) {
            return 0xFFFFFF55;
        }
        return 0xFFFF5555;
    }

    private static int lerpColor(int from, int to, float t) {
        int r = lerpChannel(from, to, t, 16);
        int g = lerpChannel(from, to, t, 8);
        int b = lerpChannel(from, to, t, 0);
        return (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t, int shift) {
        int a = (from >> shift) & 0xFF;
        int b = (to >> shift) & 0xFF;
        return (int) (a + (b - a) * t);
    }
}
