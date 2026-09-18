package com.flowclient.mods;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class NametagMod {
    private static boolean enabled;

    private NametagMod() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        setEnabled(value, true);
    }

    static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (persist) {
            FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static boolean shouldForceNametag(LivingEntity entity) {
        if (!enabled || !(entity instanceof Player)) {
            return false;
        }

        var client = net.minecraft.client.Minecraft.getInstance();
        return client.player == null || !entity.isInvisibleTo(client.player);
    }
}
