package com.flowclient.mods.voicechat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class VoiceChatNotifier {
    private VoiceChatNotifier() {}

    static void showToggleResult(boolean enabled, boolean success, String error) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        if (!success) {
            client.player.sendSystemMessage(
                    Component.literal("Simple Voice Chat: failed to update — " + (error == null ? "unknown error" : error)));
            return;
        }

        if (enabled) {
            client.player.sendSystemMessage(Component.literal("Simple Voice Chat enabled."));
        } else {
            client.player.sendSystemMessage(Component.literal("Simple Voice Chat disabled. Microphone released."));
        }
    }
}
