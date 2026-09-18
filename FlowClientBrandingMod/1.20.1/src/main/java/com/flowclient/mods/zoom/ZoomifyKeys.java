package com.flowclient.mods.zoom;

import com.flowclient.FlowClientKeyCategories;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ZoomifyKeys {
    public static final String ZOOM_KEY = "key.flowclient.zoom";

    private static final KeyMapping ZOOM = new KeyMapping(
            ZOOM_KEY,
            GLFW.GLFW_KEY_C,
            FlowClientKeyCategories.FLOWCLIENT
    );

    private ZoomifyKeys() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(ZOOM);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                ZoomController.setZoomKeyHeld(false);
                return;
            }

            boolean zoomAllowed = client.screen == null;
            ZoomController.setZoomKeyHeld(zoomAllowed && ZOOM.isDown());
            ZoomController.updateTargets();
        });
    }
}
