package com.flowclient.modpanel;

import com.flowclient.mods.chat.ChatTweaksSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ChatTweaksSettingsScreen extends ModSettingsPanelScreen<ChatTweaksSettings> {
    private static final Component TITLE = Component.literal("Chat Tweaks");

    public ChatTweaksSettingsScreen(Screen parent) {
        super(TITLE, parent, "Chat Tweaks");
    }

    @Override
    protected ChatTweaksSettings settings() {
        return ChatTweaksSettings.get();
    }

    @Override
    protected void persistSettings() {
        ChatTweaksSettings.get().persist();
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Display");
        this.addRow("Timestamps", s -> s.timestamps() ? "ON" : "OFF", s -> s.setTimestamps(!s.timestamps()), s -> s.setTimestamps(false));
        this.addRow("Hide background", s -> s.hideBackground() ? "ON" : "OFF", s -> s.setHideBackground(!s.hideBackground()), s -> s.setHideBackground(false));
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), s -> s.setBackgroundOpacity(50));
        this.addRow("Chat scale", s -> s.chatScalePercent() + "%", s -> s.cycleChatScale(), s -> s.cycleChatScale());
    }
}
