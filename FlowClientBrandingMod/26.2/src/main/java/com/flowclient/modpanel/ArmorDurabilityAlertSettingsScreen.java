package com.flowclient.modpanel;

import com.flowclient.mods.armordurability.ArmorDurabilityAlertSettings;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ArmorDurabilityAlertSettingsScreen extends ModSettingsPanelScreen<ArmorDurabilityAlertSettings> {
    private static final Component TITLE = Component.literal("Armor Durability Alert");

    public ArmorDurabilityAlertSettingsScreen(Screen parent) {
        super(TITLE, parent, "Armor Durability Alert");
    }

    @Override
    protected ArmorDurabilityAlertSettings settings() {
        return ArmorDurabilityAlertSettings.get();
    }

    @Override
    protected void persistSettings() {
        ArmorDurabilityAlertSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.ARMOR_DURABILITY_ALERT);
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("Alert threshold", s -> s.thresholdPercent() + "%", s -> s.cycleThresholdPercent(), ArmorDurabilityAlertSettings::resetThresholdPercent);
        this.addRow("Background", s -> onOff(s.showBackground()), s -> s.toggleShowBackground(), ArmorDurabilityAlertSettings::resetShowBackground);
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), ArmorDurabilityAlertSettings::resetBackgroundOpacity);
        this.addRow("Text shadow", s -> onOff(s.textShadow()), s -> s.toggleTextShadow(), ArmorDurabilityAlertSettings::resetTextShadow);
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }
}
