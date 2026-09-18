package com.flowclient.mods.freelook;

import com.flowclient.FlowClientMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class FreelookKeys {
    public static final String FREELOOK_KEY = "key.flowclient.freelook";

    private static final KeyMapping FREELOOK = new KeyMapping(
            FREELOOK_KEY,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.category." + FlowClientMod.MOD_ID + ".flowclient"
    );

    private FreelookKeys() {}

    public static KeyMapping getFreelookKey() {
        return FREELOOK;
    }

    public static boolean isFreelookHeld(Minecraft client) {
        if (FREELOOK.isDown()) {
            return true;
        }

        var window = client.getWindow();
        if (window == null) {
            return false;
        }

        long handle = window.getWindow();
        return InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_ALT);
    }
}
