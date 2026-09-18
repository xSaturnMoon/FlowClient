package com.flowclient.mods.pearl;

import com.flowclient.mods.preview.GizmoShapes;
import com.flowclient.mods.preview.ProjectileSimulation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PearlLandingMarkerRenderer {
    private static final int ARC_COLOR = 0xFF66AAFF;
    private static final int MARKER_COLOR = 0xFFFFEE55;

    private PearlLandingMarkerRenderer() {
    }

    public static boolean shouldRender() {
        return PearlLandingMarkerMod.isEnabled();
    }

    public static void render(Minecraft client) {
        if (!PearlLandingMarkerMod.isEnabled() || client.player == null || client.level == null) {
            return;
        }

        LocalPlayer player = client.player;
        if (!isHoldingPearl(player)) {
            return;
        }

        ProjectileSimulation.SimulationResult result = ProjectileSimulation.simulateEnderPearl(client.level, player);
        if (result.points().size() < 2) {
            return;
        }

        for (int i = 1; i < result.points().size(); i++) {
            Gizmos.line(result.points().get(i - 1), result.points().get(i), ARC_COLOR, 1.5F);
        }

        GizmoShapes.landingCross(result.landing(), MARKER_COLOR, 2.0F);
        GizmoShapes.horizontalCircle(result.landing(), 0.75D, MARKER_COLOR, 1.5F);
    }

    private static boolean isHoldingPearl(LocalPlayer player) {
        return isPearl(player.getMainHandItem()) || isPearl(player.getOffhandItem());
    }

    private static boolean isPearl(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.ENDER_PEARL);
    }
}
