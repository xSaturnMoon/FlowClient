package com.flowclient.modpanel;

import com.flowclient.mods.quiet.FlowQuietSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FlowQuietSettingsScreen extends ModSettingsPanelScreen<FlowQuietSettings> {
    private static final Component TITLE = Component.literal("Flow Quiet");

    public FlowQuietSettingsScreen(Screen parent) {
        super(TITLE, parent, "Flow Quiet");
    }

    @Override
    protected FlowQuietSettings settings() {
        return FlowQuietSettings.get();
    }

    @Override
    protected void persistSettings() {
        FlowQuietSettings.get().persist();
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Triggers");
        this.addRow("When unfocused", s -> s.isUnfocusedEnabled() ? "ON" : "OFF", s -> s.setUnfocusedEnabled(!s.isUnfocusedEnabled()), s -> s.setUnfocusedEnabled(true));
        this.addRow("When AFK", s -> s.isAfkEnabled() ? "ON" : "OFF", s -> s.setAfkEnabled(!s.isAfkEnabled()), s -> s.setAfkEnabled(true));
        this.addRow("AFK timeout", s -> s.getAfkTimeoutSeconds() + "s", s -> s.cycleAfkTimeoutSeconds(), s -> s.setAfkTimeoutSeconds(60));

        this.addHeader("Background mode");
        this.addRow("Background FPS", s -> s.getBackgroundFps() + " FPS", s -> s.cycleBackgroundFps(), s -> s.setBackgroundFps(20));
        this.addRow("Mute all audio", s -> s.isMuteAudio() ? "ON" : "OFF", s -> s.setMuteAudio(!s.isMuteAudio()), s -> s.setMuteAudio(true));
    }
}
