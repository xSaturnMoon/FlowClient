package com.flowclient.mods.chat;

import net.minecraft.client.Minecraft;

public final class ChatTweaksController {
    private static Double savedChatOpacity;
    private static Double savedChatScale;

    private ChatTweaksController() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.options == null || !ChatTweaksMod.isEnabled()) {
            return;
        }

        ChatTweaksSettings settings = ChatTweaksSettings.get();
        if (savedChatOpacity == null) {
            savedChatOpacity = client.options.chatOpacity().get();
        }
        if (savedChatScale == null) {
            savedChatScale = client.options.chatScale().get();
        }

        client.options.chatOpacity().set(settings.backgroundOpacity() / 100.0D);
        client.options.chatScale().set(settings.chatScalePercent() / 100.0D);
    }

    public static void onDisabled() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.options != null) {
            if (savedChatOpacity != null) {
                client.options.chatOpacity().set(savedChatOpacity);
            }
            if (savedChatScale != null) {
                client.options.chatScale().set(savedChatScale);
            }
        }
        savedChatOpacity = null;
        savedChatScale = null;
    }
}
