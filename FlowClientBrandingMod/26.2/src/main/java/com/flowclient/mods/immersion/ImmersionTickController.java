package com.flowclient.mods.immersion;

import net.minecraft.client.Minecraft;

public final class ImmersionTickController {
    private ImmersionTickController() {
    }

    public static void tick(Minecraft client) {
        PortalFxController.tick();
        ComboCounterTracker.tick(client);

        if (!TrajectoryArcMod.isEnabled() || client == null || client.player == null || client.level == null) {
            return;
        }

        try (var ignored = client.collectPerTickGizmos()) {
            TrajectoryArcRenderer.render(client);
        }
    }
}
