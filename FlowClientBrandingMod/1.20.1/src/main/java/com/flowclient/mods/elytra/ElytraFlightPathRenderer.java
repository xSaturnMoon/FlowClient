package com.flowclient.mods.elytra;

import net.minecraft.client.Minecraft;

public final class ElytraFlightPathRenderer {
    private ElytraFlightPathRenderer() {
    }

    public static boolean shouldRender() {
        return ElytraFlightPathMod.isEnabled();
    }

    public static void render(Minecraft client) {
    }
}
