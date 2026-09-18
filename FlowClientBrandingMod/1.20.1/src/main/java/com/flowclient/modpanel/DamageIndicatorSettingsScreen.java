package com.flowclient.modpanel;

import com.flowclient.mods.render.DamageIndicatorSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DamageIndicatorSettingsScreen extends ModSettingsPanelScreen<DamageIndicatorSettings> {
    private static final Component TITLE = Component.literal("Damage Indicator");

    public DamageIndicatorSettingsScreen(Screen parent) {
        super(TITLE, parent, "Damage Indicator");
    }

    @Override
    protected DamageIndicatorSettings settings() {
        return DamageIndicatorSettings.get();
    }

    @Override
    protected void persistSettings() {
        DamageIndicatorSettings.get().persist();
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Display");
        this.addRow("Show healing", s -> s.showHealing() ? "ON" : "OFF", s -> s.setShowHealing(!s.showHealing()), s -> s.setShowHealing(false));
        this.addRow("Duration", s -> s.durationMs() + "ms", s -> s.cycleDuration(), s -> s.cycleDuration());
        this.addRow("Color", s -> colorName(s.textColor()), s -> s.cycleColor(), s -> s.cycleColor());
    }

    private static String colorName(int color) {
        return switch (color) {
            case 0xFFFF5555 -> "Red";
            case 0xFFFFAA00 -> "Orange";
            case 0xFFFFFFFF -> "White";
            case 0xFFFF66FF -> "Pink";
            default -> "Custom";
        };
    }
}
