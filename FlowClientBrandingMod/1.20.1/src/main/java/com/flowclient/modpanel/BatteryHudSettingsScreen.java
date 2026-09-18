package com.flowclient.modpanel;

import com.flowclient.mods.battery.BatteryHudSettings;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BatteryHudSettingsScreen extends ModSettingsPanelScreen<BatteryHudSettings> {
    private static final Component TITLE = Component.literal("Battery HUD");

    public BatteryHudSettingsScreen(Screen parent) {
        super(TITLE, parent, "Battery HUD");
    }

    @Override
    protected BatteryHudSettings settings() {
        return BatteryHudSettings.get();
    }

    @Override
    protected void persistSettings() {
        BatteryHudSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.BATTERY_HUD);
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("System battery", s -> onOff(s.showSystemBattery()), s -> s.toggleShowSystemBattery(), BatteryHudSettings::resetShowSystemBattery);
        this.addRow("Connected devices", s -> onOff(s.showConnectedDevices()), s -> {
            s.toggleShowConnectedDevices();
            if (s.showConnectedDevices()) {
                com.flowclient.mods.battery.BatteryPoller.get().requestConnectedPoll();
            }
        }, BatteryHudSettings::resetShowConnectedDevices);
        this.addRow("Device names", s -> onOff(s.showDeviceNames()), s -> s.toggleShowDeviceNames(), BatteryHudSettings::resetShowDeviceNames);
        this.addRow("Progress bar", s -> onOff(s.showProgressBar()), s -> s.toggleShowProgressBar(), BatteryHudSettings::resetShowProgressBar);
        this.addRow("Background", s -> onOff(s.showBackground()), s -> s.toggleShowBackground(), BatteryHudSettings::resetShowBackground);
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), BatteryHudSettings::resetBackgroundOpacity);
        this.addRow("Text shadow", s -> onOff(s.textShadow()), s -> s.toggleTextShadow(), BatteryHudSettings::resetTextShadow);
        this.addRow("Low threshold", s -> s.lowThreshold() + "%", s -> s.cycleLowThreshold(), BatteryHudSettings::resetLowThreshold);
        this.addRow("Mid threshold", s -> s.midThreshold() + "%", s -> s.cycleMidThreshold(), BatteryHudSettings::resetMidThreshold);
        this.addRow("Padding X", s -> Integer.toString(s.paddingX()), s -> s.cyclePaddingX(), BatteryHudSettings::resetPaddingX);
        this.addRow("Padding Y", s -> Integer.toString(s.paddingY()), s -> s.cyclePaddingY(), BatteryHudSettings::resetPaddingY);
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
