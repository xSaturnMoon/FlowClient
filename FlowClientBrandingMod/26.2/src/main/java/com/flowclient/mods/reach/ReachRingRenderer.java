package com.flowclient.mods.reach;

import com.flowclient.mods.preview.GizmoShapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ReachRingRenderer {
    private static final int RING_COLOR = 0xFF55FFAA;

    private ReachRingRenderer() {
    }

    public static boolean shouldRender() {
        return ReachRingMod.isEnabled();
    }

    public static void render(Minecraft client) {
        if (!ReachRingMod.isEnabled() || client.player == null) {
            return;
        }

        LocalPlayer player = client.player;
        double reach = resolveReach(player);
        Vec3 center = new Vec3(player.getX(), Math.floor(player.getY()) + 0.02D, player.getZ());
        GizmoShapes.horizontalCircle(center, reach, RING_COLOR, 2.0F);
        GizmoShapes.horizontalCircle(center, reach * 0.5D, 0x8855FFAA, 1.0F);
    }

    private static double resolveReach(LocalPlayer player) {
        return player.entityInteractionRange();
    }
}
