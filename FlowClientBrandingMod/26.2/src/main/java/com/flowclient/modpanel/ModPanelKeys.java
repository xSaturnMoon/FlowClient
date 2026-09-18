package com.flowclient.modpanel;

import com.flowclient.FlowClientKeyCategories;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
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
        KeyMappingHelper.registerKeyMapping(OPEN_PANEL);
    }

    public static void onRightShiftPressed() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return;
        }

        togglePanel(client);
    }

    private static void togglePanel(Minecraft client) {
        Screen current = client.gui.screen();
        if (isModPanelScreen(current)) {
            client.gui.setScreen(null);
            return;
        }

        if (current != null && !client.gui.canInterruptScreen()) {
            return;
        }

        client.gui.setScreen(new ModPanelMenuScreen());
    }

    private static boolean isModPanelScreen(Screen screen) {
        return screen instanceof ModPanelScreen;
    }

    public static boolean isOpenKey(int key, int action) {
        return action == GLFW.GLFW_PRESS && key == InputConstants.KEY_RSHIFT;
    }
}
