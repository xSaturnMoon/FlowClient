package com.flowclient.mods.explosion;

import net.minecraft.client.Minecraft;

public final class CrystalBedPreviewRenderer {
    private CrystalBedPreviewRenderer() {
    }

    public static boolean shouldRender() {
        return CrystalBedPreviewMod.isEnabled();
    }

    public static void render(Minecraft client) {
    }
}
