package com.flowclient.mods.otherclient;

import com.flowclient.compat.FlowGfx;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientBadgeRenderer {
    public static final int BADGE_SIZE = 12;
    public static final int BADGE_GAP = 2;

    private static final Map<Component, DetectedClient> PENDING_BADGES = new IdentityHashMap<>();
    private static final Map<String, DetectedClient> PENDING_BADGES_BY_TEXT = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> SEE_THROUGH_PASS = ThreadLocal.withInitial(() -> false);

    private ClientBadgeRenderer() {}

    public static void setSeeThroughPass(boolean seeThrough) {
        SEE_THROUGH_PASS.set(seeThrough);
    }

    public static boolean isSeeThroughPass() {
        return Boolean.TRUE.equals(SEE_THROUGH_PASS.get());
    }

    public static void registerPendingBadge(Component component, DetectedClient client) {
        if (component == null || client == null) {
            return;
        }
        PENDING_BADGES.put(component, client);
        PENDING_BADGES_BY_TEXT.put(component.getString(), client);
    }

    public static DetectedClient getPendingBadge(Component component) {
        if (component == null) {
            return null;
        }

        DetectedClient badge = PENDING_BADGES.get(component);
        if (badge != null) {
            return badge;
        }
        return PENDING_BADGES_BY_TEXT.get(component.getString());
    }

    public static void clearPendingBadges() {
        PENDING_BADGES.clear();
        PENDING_BADGES_BY_TEXT.clear();
        SEE_THROUGH_PASS.remove();
    }

    public static float badgeTextOffset(Component component) {
        return getPendingBadge(component) == null ? 0.0F : (BADGE_SIZE + BADGE_GAP) / 2.0F;
    }

    public static RenderType badgeRenderType() {
        FlowEmblemTextures.ensureLoaded();
        return isSeeThroughPass()
                ? RenderType.textSeeThrough(FlowEmblem.TEXTURE)
                : RenderType.text(FlowEmblem.TEXTURE);
    }

    public static void drawNametagBadge(
            Matrix4f pose,
            VertexConsumer consumer,
            float textX,
            float textY,
            int lightCoords
    ) {
        float badgeX = textX - BADGE_SIZE - BADGE_GAP;
        float badgeY = textY - 1.0F;
        drawTexturedQuad(pose, consumer, badgeX, badgeY, BADGE_SIZE, BADGE_SIZE, lightCoords);
    }

    public static void renderTabBadge(GuiGraphics graphics, int x, int y) {
        FlowEmblemTextures.ensureLoaded();
        FlowGfx.blit(graphics, FlowEmblem.TEXTURE, x, y, BADGE_SIZE, BADGE_SIZE, 0.0F, 0.0F, 1.0F, 1.0F);
    }

    public static int tabBadgeWidth() {
        return BADGE_SIZE + BADGE_GAP;
    }

    private static void drawTexturedQuad(
            Matrix4f pose,
            VertexConsumer consumer,
            float x,
            float y,
            float width,
            float height,
            int lightCoords
    ) {
        float x2 = x + width;
        float y2 = y + height;

        consumer.vertex(pose, x, y2, 0).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(lightCoords).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(pose, x2, y2, 0).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(lightCoords).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(pose, x2, y, 0).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(lightCoords).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(pose, x, y, 0).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(lightCoords).normal(0.0F, 0.0F, 1.0F).endVertex();
    }
}
