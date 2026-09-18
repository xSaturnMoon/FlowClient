package com.flowclient.mods.reach;

import net.minecraft.client.Minecraft;

public final class ReachRingRenderer {
    private ReachRingRenderer() {
    }

    public static boolean shouldRender() {
        return ReachRingMod.isEnabled();
    }

    public static void render(Minecraft client) {
    }
}
