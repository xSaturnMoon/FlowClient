package com.flowclient.mods.freelook;

import com.flowclient.FlowClientKeyCategories;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class FreelookKeys {
    public static final String FREELOOK_KEY = "key.flowclient.freelook";

    private static final KeyMapping FREELOOK = new KeyMapping(
            FREELOOK_KEY,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            FlowClientKeyCategories.FLOWCLIENT
    );

    private FreelookKeys() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(FREELOOK);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean allowed = isAllowed(client);
            FreelookController.updateKeyState(isFreelookHeld(client), allowed);
        });
    }

    private static boolean isAllowed(Minecraft client) {
        return client.player != null
                && client.level != null
                && client.screen == null;
    }

    private static boolean isFreelookHeld(Minecraft client) {
        if (FREELOOK.isDown()) {
            return true;
        }

        var window = client.getWindow();
        if (window == null) {
            return false;
        }

        return InputConstants.isKeyDown(window.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
    }
}
