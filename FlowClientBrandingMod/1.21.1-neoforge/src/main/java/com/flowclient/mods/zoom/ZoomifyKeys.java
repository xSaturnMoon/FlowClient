package com.flowclient.mods.zoom;

import com.flowclient.FlowClientMod;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ZoomifyKeys {
    public static final String ZOOM_KEY = "key.flowclient.zoom";

    private static final KeyMapping ZOOM = new KeyMapping(
            ZOOM_KEY,
            GLFW.GLFW_KEY_C,
            "key.category." + FlowClientMod.MOD_ID + ".flowclient"
    );

    private ZoomifyKeys() {}

    public static KeyMapping getZoomKey() {
        return ZOOM;
    }
}
