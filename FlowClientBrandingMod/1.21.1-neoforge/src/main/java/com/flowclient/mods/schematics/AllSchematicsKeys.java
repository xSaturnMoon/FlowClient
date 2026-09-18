package com.flowclient.mods.schematics;

import com.flowclient.FlowClientMod;
import com.flowclient.mods.schematics.ui.SchematicBrowserScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class AllSchematicsKeys {
    public static final String MENU_KEY = "key.flowclient.all_schematics_menu";

    private static final KeyMapping MENU = new KeyMapping(
            MENU_KEY,
            GLFW.GLFW_KEY_COMMA,
            "key.category." + FlowClientMod.MOD_ID + ".flowclient"
    );

    private AllSchematicsKeys() {}

    public static KeyMapping getMenuKey() {
        return MENU;
    }

    public static boolean isMenuKey(int key, int action) {
        return action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_COMMA;
    }

    public static void onMenuKeyPressed() {
        tryOpenMenu(Minecraft.getInstance());
    }

    public static void tick(Minecraft client) {
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
