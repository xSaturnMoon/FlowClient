package com.flowclient.modpanel;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.serveraddress.ServerAddressHudSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ServerAddressHudSettingsScreen extends ModSettingsPanelScreen<ServerAddressHudSettings> {
    private static final Component TITLE = Component.literal("Server Address HUD");

    public ServerAddressHudSettingsScreen(Screen parent) {
        super(TITLE, parent, "Server Address HUD");
    }

    @Override
    protected ServerAddressHudSettings settings() {
        return ServerAddressHudSettings.get();
    }

    @Override
    protected void persistSettings() {
        ServerAddressHudSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.SERVER_ADDRESS_HUD);
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("Display mode", s -> s.displayMode().label(), s -> s.cycleDisplayMode(), ServerAddressHudSettings::resetDisplayMode);
        this.addRow("Hide singleplayer", s -> onOff(s.hideSingleplayer()), s -> s.toggleHideSingleplayer(), ServerAddressHudSettings::resetHideSingleplayer);
        this.addRow("Background", s -> onOff(s.showBackground()), s -> s.toggleShowBackground(), ServerAddressHudSettings::resetShowBackground);
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), ServerAddressHudSettings::resetBackgroundOpacity);
        this.addRow("Text shadow", s -> onOff(s.textShadow()), s -> s.toggleTextShadow(), ServerAddressHudSettings::resetTextShadow);
        this.addRow("Padding X", s -> Integer.toString(s.paddingX()), s -> s.cyclePaddingX(), ServerAddressHudSettings::resetPaddingX);
        this.addRow("Padding Y", s -> Integer.toString(s.paddingY()), s -> s.cyclePaddingY(), ServerAddressHudSettings::resetPaddingY);
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }
}
