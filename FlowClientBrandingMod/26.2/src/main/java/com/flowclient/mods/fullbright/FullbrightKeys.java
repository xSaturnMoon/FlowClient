package com.flowclient.mods.fullbright;

import com.flowclient.FlowClientKeyCategories;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class FullbrightKeys {
    public static final String FULLBRIGHT_KEY = "key.flowclient.fullbright";

    private static final KeyMapping TOGGLE = new KeyMapping(
            FULLBRIGHT_KEY,
            GLFW.GLFW_KEY_B,
            FlowClientKeyCategories.FLOWCLIENT
    );

    private FullbrightKeys() {
    }

    public static void register() {
        KeyMappingHelper.registerKeyMapping(TOGGLE);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                return;
            }
            if (client.gui.screen() != null) {
                return;
            }
            while (TOGGLE.consumeClick()) {
                FullbrightMod.toggle();
            }
            if (FullbrightMod.isEnabled()) {
                FullbrightController.apply(client);
            }
        });
    }
}
