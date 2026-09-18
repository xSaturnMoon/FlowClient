package com.flowclient.mods.inventory;

import com.flowclient.FlowClientKeyCategories;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class InventorySortKeys {
    public static final String SORT_KEY = "key.flowclient.inventory_sort";

    private static final KeyMapping SORT = new KeyMapping(
            SORT_KEY,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            FlowClientKeyCategories.FLOWCLIENT
    );

    private InventorySortKeys() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(SORT);
        ClientTickEvents.END_CLIENT_TICK.register(InventorySortKeys::tick);
    }

    private static void tick(Minecraft client) {
        InventorySortExecutor.tick(client);

        if (!InventorySortMod.isEnabled() || client.player == null) {
            return;
        }

        while (SORT.consumeClick()) {
            InventorySortExecutor.requestSort(client);
        }
    }
}
