package com.flowclient.modpanel;

import com.flowclient.mods.blockbreak.BlockBreakProgressSettings;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BlockBreakProgressSettingsScreen extends ModSettingsPanelScreen<BlockBreakProgressSettings> {
    private static final Component TITLE = Component.literal("Block Break Progress");

    public BlockBreakProgressSettingsScreen(Screen parent) {
        super(TITLE, parent, "Block Break Progress");
    }

    @Override
    protected BlockBreakProgressSettings settings() {
        return BlockBreakProgressSettings.get();
    }

    @Override
    protected void persistSettings() {
        BlockBreakProgressSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.BLOCK_BREAK_PROGRESS);
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("Bar width", s -> s.barWidth() + "px", s -> s.cycleBarWidth(), BlockBreakProgressSettings::resetBarWidth);
        this.addRow("Show percentage", s -> bool(s.showPercentage()), s -> s.toggleShowPercentage(), BlockBreakProgressSettings::resetShowPercentage);
        this.addRow("Background", s -> bool(s.showBackground()), s -> s.toggleShowBackground(), BlockBreakProgressSettings::resetShowBackground);
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), BlockBreakProgressSettings::resetBackgroundOpacity);
    }

    private static String bool(boolean value) {
        return value ? "ON" : "OFF";
    }
}
