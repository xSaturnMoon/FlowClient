package com.flowclient.mods.pearl;

import net.minecraft.client.Minecraft;

public final class PearlLandingMarkerRenderer {
    private PearlLandingMarkerRenderer() {
    }

    public static boolean shouldRender() {
        return PearlLandingMarkerMod.isEnabled();
    }

    public static void render(Minecraft client) {
    }
}
