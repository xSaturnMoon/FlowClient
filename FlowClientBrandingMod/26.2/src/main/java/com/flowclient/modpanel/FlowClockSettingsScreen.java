package com.flowclient.modpanel;

import com.flowclient.mods.clock.FlowClockSettings;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FlowClockSettingsScreen extends ModSettingsPanelScreen<FlowClockSettings> {
    private static final Component TITLE = Component.literal("Flow Clock");

    public FlowClockSettingsScreen(Screen parent) {
        super(TITLE, parent, "Flow Clock");
    }

    @Override
    protected FlowClockSettings settings() {
        return FlowClockSettings.get();
    }

    @Override
    protected void persistSettings() {
        FlowClockSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.FLOW_CLOCK);
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("Format", s -> s.format().label(), s -> s.cycleFormat(), FlowClockSettings::resetFormat);
        this.addRow("Color", s -> s.colorMode().name(), s -> s.cycleColorMode(), FlowClockSettings::resetColorMode);
        this.addRow("Background", s -> s.showBackground() ? "ON" : "OFF", s -> s.toggleShowBackground(), FlowClockSettings::resetShowBackground);
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), FlowClockSettings::resetBackgroundOpacity);
        this.addRow("Text shadow", s -> s.textShadow() ? "ON" : "OFF", s -> s.toggleTextShadow(), FlowClockSettings::resetTextShadow);
        this.addRow("Padding X", s -> Integer.toString(s.paddingX()), s -> s.setPaddingX(cycle(s.paddingX(), 0, 16, 1)), FlowClockSettings::resetPaddingX);
        this.addRow("Padding Y", s -> Integer.toString(s.paddingY()), s -> s.setPaddingY(cycle(s.paddingY(), 0, 12, 1)), FlowClockSettings::resetPaddingY);
    }

    private static int cycle(int current, int min, int max, int step) {
        int next = current + step;
        return next > max ? min : next;
    }
}
