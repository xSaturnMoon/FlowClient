package com.flowclient.mods.immersion;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class PortalFxController {
    public enum PortalEffect {
        NONE,
        NETHER,
        END,
        OVERWORLD
    }

    private static PortalEffect activeEffect = PortalEffect.NONE;
    private static float intensity;
    private static int titleTicks;

    private PortalFxController() {
    }

    public static void reset() {
        activeEffect = PortalEffect.NONE;
        intensity = 0.0F;
        titleTicks = 0;
    }

    public static void onDimensionChange(ResourceKey<Level> from, ResourceKey<Level> to) {
        if (!PortalFxMod.isEnabled()) {
            return;
        }

        if (to.equals(Level.NETHER)) {
            activeEffect = PortalEffect.NETHER;
        } else if (to.equals(Level.END)) {
            activeEffect = PortalEffect.END;
        } else if (from.equals(Level.NETHER) || from.equals(Level.END)) {
            activeEffect = PortalEffect.OVERWORLD;
        } else {
            return;
        }

        intensity = 1.0F;
        titleTicks = 50;
    }

    public static void tick() {
        if (!PortalFxMod.isEnabled() || intensity <= 0.0F) {
            return;
        }
        intensity = Math.max(0.0F, intensity - 0.018F);
        if (titleTicks > 0) {
            titleTicks--;
        }
        if (intensity <= 0.0F) {
            activeEffect = PortalEffect.NONE;
        }
    }

    public static PortalEffect activeEffect() {
        return activeEffect;
    }

    public static float intensity() {
        return intensity;
    }

    public static boolean showTitle() {
        return titleTicks > 0;
    }
}
