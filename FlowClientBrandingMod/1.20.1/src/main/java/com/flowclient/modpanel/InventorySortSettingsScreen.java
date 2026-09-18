package com.flowclient.modpanel;

import com.flowclient.mods.inventory.InventorySortSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class InventorySortSettingsScreen extends ModSettingsPanelScreen<InventorySortSettings> {
    private static final Component TITLE = Component.literal("Inventory Sort");

    public InventorySortSettingsScreen(Screen parent) {
        super(TITLE, parent, "Inventory Sort");
    }

    @Override
    protected InventorySortSettings settings() {
        return InventorySortSettings.get();
    }

    @Override
    protected void persistSettings() {
        InventorySortSettings.get().persist();
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Sorting");
        this.addRow("Include hotbar", s -> s.includeHotbar() ? "ON" : "OFF", s -> s.setIncludeHotbar(!s.includeHotbar()), s -> s.setIncludeHotbar(true));
        this.addRow("Merge stacks", s -> s.mergeStacks() ? "ON" : "OFF", s -> s.setMergeStacks(!s.mergeStacks()), s -> s.setMergeStacks(true));
        this.addHeader("Controls");
        this.addHeader("Press R while inventory is open");
    }
}
