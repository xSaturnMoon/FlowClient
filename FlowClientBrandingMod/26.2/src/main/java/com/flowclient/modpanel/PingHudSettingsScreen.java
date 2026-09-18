package com.flowclient.modpanel;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.ping.PingHudSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PingHudSettingsScreen extends ModSettingsPanelScreen<PingHudSettings> {
    private static final Component TITLE = Component.literal("Ping HUD");

    public PingHudSettingsScreen(Screen parent) {
        super(TITLE, parent, "Ping HUD");
    }

    @Override
    protected PingHudSettings settings() {
        return PingHudSettings.get();
    }

    @Override
    protected void persistSettings() {
        PingHudSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.PING_HUD);
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("Dynamic color", s -> onOff(s.dynamicColor()), s -> s.toggleDynamicColor(), PingHudSettings::resetDynamicColor);
        this.addRow("Background", s -> onOff(s.showBackground()), s -> s.toggleShowBackground(), PingHudSettings::resetShowBackground);
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), PingHudSettings::resetBackgroundOpacity);
        this.addRow("Text shadow", s -> onOff(s.textShadow()), s -> s.toggleTextShadow(), PingHudSettings::resetTextShadow);
        this.addRow("Padding X", s -> Integer.toString(s.paddingX()), s -> s.cyclePaddingX(), PingHudSettings::resetPaddingX);
        this.addRow("Padding Y", s -> Integer.toString(s.paddingY()), s -> s.cyclePaddingY(), PingHudSettings::resetPaddingY);
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
