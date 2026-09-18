package com.flowclient.mods.schematics;

import com.flowclient.mods.schematics.ui.SchematicBrowserScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class AllSchematicsKeys {
    public static final String MENU_KEY = "key.flowclient.all_schematics_menu";

    private static final KeyMapping MENU = new KeyMapping(
            MENU_KEY,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            com.flowclient.FlowClientKeyCategories.FLOWCLIENT
    );

    private AllSchematicsKeys() {}

    public static void register() {
        KeyBindingHelper.registerKeyBinding(MENU);
        ClientTickEvents.END_CLIENT_TICK.register(AllSchematicsKeys::tick);
    }

    public static boolean isMenuKey(int key, int scancode, int action, int modifiers) {
        if (action != GLFW.GLFW_PRESS) {
            return false;
        }
        return MENU.matches(key, scancode);
    }

    public static void onMenuKeyPressed() {
        tryOpenMenu(Minecraft.getInstance());
    }

    private static void tick(Minecraft client) {
        if (!AllSchematicsMod.isEnabled() || client.player == null || client.level == null) {
            return;
        }

        while (MENU.consumeClick()) {
            tryOpenMenu(client);
        }
    }

    private static void tryOpenMenu(Minecraft client) {
        if (client.screen != null) {
            return;
        }
        client.setScreen(new SchematicBrowserScreen(null));
    }
}
