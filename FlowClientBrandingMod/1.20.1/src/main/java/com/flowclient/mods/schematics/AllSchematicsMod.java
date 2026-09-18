package com.flowclient.mods.schematics;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class AllSchematicsMod {
    private static boolean enabled;

    private AllSchematicsMod() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AllSchematicsMod::onClientTick);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (!enabled) {
            PlacementManager.clear();
        }
        if (persist) {
            com.flowclient.mods.FlowModConfig.save();
        }
    }

    public static void toggle() {
        setEnabled(!enabled, true);
    }

    private static void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || !PlacementManager.hasPlacement()) {
            return;
        }
        SchematicRenderer.render(client);
    }
}
