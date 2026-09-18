package com.flowclient.modpanel;

import com.flowclient.mods.cooldown.ItemCooldownHudSettings;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ItemCooldownHudSettingsScreen extends ModSettingsPanelScreen<ItemCooldownHudSettings> {
    private static final Component TITLE = Component.literal("Item Cooldown HUD");

    public ItemCooldownHudSettingsScreen(Screen parent) {
        super(TITLE, parent, "Item Cooldown HUD");
    }

    @Override
    protected ItemCooldownHudSettings settings() {
        return ItemCooldownHudSettings.get();
    }

    @Override
    protected void persistSettings() {
        ItemCooldownHudSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.ITEM_COOLDOWN_HUD);
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("Attack cooldown", s -> onOff(s.showAttackCooldown()), s -> s.toggleShowAttackCooldown(), ItemCooldownHudSettings::resetShowAttackCooldown);
        this.addRow("Item cooldown", s -> onOff(s.showItemCooldown()), s -> s.toggleShowItemCooldown(), ItemCooldownHudSettings::resetShowItemCooldown);
        this.addRow("Bar width", s -> s.barWidth() + "px", s -> s.cycleBarWidth(), ItemCooldownHudSettings::resetBarWidth);
        this.addRow("Background", s -> onOff(s.showBackground()), s -> s.toggleShowBackground(), ItemCooldownHudSettings::resetShowBackground);
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), ItemCooldownHudSettings::resetBackgroundOpacity);
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }
}
