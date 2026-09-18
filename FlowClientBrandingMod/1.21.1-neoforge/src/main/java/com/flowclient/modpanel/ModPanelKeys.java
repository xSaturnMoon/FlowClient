package com.flowclient.modpanel;

import com.flowclient.mods.schematics.ui.SchematicActionScreen;
import com.flowclient.mods.schematics.ui.SchematicBrowserScreen;
import com.flowclient.mods.schematics.ui.SchematicPlacementScreen;
import com.flowclient.mods.schematics.ui.SchematicRenameScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public final class ModPanelKeys {
    public static final String OPEN_PANEL_KEY = "key.flowclient.mod_panel";
    private static final KeyMapping OPEN_PANEL = new KeyMapping(
            OPEN_PANEL_KEY,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "key.categories.misc"
    );

    private ModPanelKeys() {}

    public static KeyMapping getOpenPanelKey() {
        return OPEN_PANEL;
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
        return screen instanceof ModPanelMenuScreen
                || screen instanceof ModListPanelScreen
                || screen instanceof ModPositionPanelScreen
                || screen instanceof ZoomifySettingsScreen
                || screen instanceof SchematicBrowserScreen
                || screen instanceof SchematicActionScreen
                || screen instanceof SchematicRenameScreen
                || screen instanceof SchematicPlacementScreen;
    }

    public static boolean isOpenKey(int key, int action) {
        return action == GLFW.GLFW_PRESS && key == InputConstants.KEY_RSHIFT;
    }
}
