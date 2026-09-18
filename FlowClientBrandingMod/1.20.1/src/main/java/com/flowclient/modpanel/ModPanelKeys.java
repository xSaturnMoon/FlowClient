package com.flowclient.modpanel;

import com.flowclient.FlowClientKeyCategories;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public final class ModPanelKeys {
    public static final String OPEN_PANEL_KEY = "key.flowclient.mod_panel";
    private static final KeyMapping OPEN_PANEL = new KeyMapping(
            OPEN_PANEL_KEY,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_RSHIFT,
            FlowClientKeyCategories.FLOWCLIENT
    );

    private ModPanelKeys() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_PANEL);
    }

    public static void onRightShiftPressed() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return;
        }

        togglePanel(client);
    }

    private static void togglePanel(Minecraft client) {
        Screen current = client.screen;
        if (isModPanelScreen(current)) {
            client.setScreen(null);
            return;
        }

        if (current != null && current.isPauseScreen()) {
            return;
        }

        client.setScreen(new ModPanelMenuScreen());
    }

    private static boolean isModPanelScreen(Screen screen) {
        return screen instanceof ModPanelScreen;
    }

    public static boolean isOpenKey(int key, int action) {
        return action == GLFW.GLFW_PRESS && key == InputConstants.KEY_RSHIFT;
    }
}
