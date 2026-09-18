package com.flowclient.mods.schematics;

import com.flowclient.FlowClientKeyCategories;
import com.flowclient.mods.schematics.ui.SchematicBrowserScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class AllSchematicsKeys {
    public static final String MENU_KEY = "key.flowclient.all_schematics_menu";

    private static final KeyMapping MENU = new KeyMapping(
            MENU_KEY,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            FlowClientKeyCategories.FLOWCLIENT
    );

    private AllSchematicsKeys() {}

    public static void register() {
        KeyMappingHelper.registerKeyMapping(MENU);
        ClientTickEvents.END_CLIENT_TICK.register(AllSchematicsKeys::tick);
    }

    public static boolean isMenuKey(KeyEvent event, int action) {
        if (action != GLFW.GLFW_PRESS) {
            return false;
        }
        return MENU.matches(event);
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
        if (client.gui.screen() != null) {
            return;
        }
        client.gui.setScreen(new SchematicBrowserScreen(null));
    }
}
