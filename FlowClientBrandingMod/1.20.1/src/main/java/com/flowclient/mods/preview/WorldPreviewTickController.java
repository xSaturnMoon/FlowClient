package com.flowclient.mods.preview;

import com.flowclient.mods.elytra.ElytraFlightPathRenderer;
import com.flowclient.mods.explosion.CrystalBedPreviewRenderer;
import com.flowclient.mods.pearl.PearlLandingMarkerRenderer;
import com.flowclient.mods.reach.ReachRingRenderer;
import net.minecraft.client.Minecraft;

public final class WorldPreviewTickController {
    private WorldPreviewTickController() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return;
        }

        if (!PearlLandingMarkerRenderer.shouldRender()
                && !ElytraFlightPathRenderer.shouldRender()
                && !ReachRingRenderer.shouldRender()
                && !CrystalBedPreviewRenderer.shouldRender()) {
            return;
        }

        PearlLandingMarkerRenderer.render(client);
        ElytraFlightPathRenderer.render(client);
        ReachRingRenderer.render(client);
        CrystalBedPreviewRenderer.render(client);
    }
}
