package com.flowclient.modpanel;

import com.flowclient.mods.blockspeed.BlockSpeedSettings;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BlockSpeedSettingsScreen extends ModSettingsPanelScreen<BlockSpeedSettings> {
    private static final Component TITLE = Component.literal("Block Speed");

    public BlockSpeedSettingsScreen(Screen parent) {
        super(TITLE, parent, "Block Speed");
    }

    @Override
    protected BlockSpeedSettings settings() {
        return BlockSpeedSettings.get();
    }

    @Override
    protected void persistSettings() {
        BlockSpeedSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.BLOCK_SPEED);
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("Format", s -> s.format().example(), s -> s.cycleFormat(), BlockSpeedSettings::resetFormat);
        this.addRow("Color", s -> s.colorMode().label(), s -> s.cycleColorMode(), BlockSpeedSettings::resetColorMode);
        this.addRow("Background", s -> s.showBackground() ? "ON" : "OFF", s -> s.toggleShowBackground(), BlockSpeedSettings::resetShowBackground);
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), BlockSpeedSettings::resetBackgroundOpacity);
        this.addRow("Text shadow", s -> s.textShadow() ? "ON" : "OFF", s -> s.toggleTextShadow(), BlockSpeedSettings::resetTextShadow);
        this.addRow("Padding X", s -> Integer.toString(s.paddingX()), s -> s.setPaddingX(cycle(s.paddingX(), 0, 16, 1)), BlockSpeedSettings::resetPaddingX);
        this.addRow("Padding Y", s -> Integer.toString(s.paddingY()), s -> s.setPaddingY(cycle(s.paddingY(), 0, 12, 1)), BlockSpeedSettings::resetPaddingY);
        this.addRow("Smoothing", s -> s.smoothing() ? "ON" : "OFF", s -> s.toggleSmoothing(), BlockSpeedSettings::resetSmoothing);
    }

    private static int cycle(int current, int min, int max, int step) {
        int next = current + step;
        return next > max ? min : next;
    }
}
