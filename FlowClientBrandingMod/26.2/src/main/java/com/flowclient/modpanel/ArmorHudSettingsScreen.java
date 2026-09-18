package com.flowclient.modpanel;

import com.flowclient.mods.armor.ArmorHudSettings;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ArmorHudSettingsScreen extends ModSettingsPanelScreen<ArmorHudSettings> {
    private static final Component TITLE = Component.literal("Armor HUD");

    public ArmorHudSettingsScreen(Screen parent) {
        super(TITLE, parent, "Armor HUD");
    }

    @Override
    protected ArmorHudSettings settings() {
        return ArmorHudSettings.get();
    }

    @Override
    protected void persistSettings() {
        ArmorHudSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.ARMOR_HUD);
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Slots");
        this.addRow("Main Hand", s -> bool(s.showMainHand()), s -> s.setShowMainHand(!s.showMainHand()), s -> s.resetField("showMainHand"));
        this.addRow("Helmet", s -> bool(s.showHelmet()), s -> s.setShowHelmet(!s.showHelmet()), s -> s.resetField("showHelmet"));
        this.addRow("Chestplate", s -> bool(s.showChestplate()), s -> s.setShowChestplate(!s.showChestplate()), s -> s.resetField("showChestplate"));
        this.addRow("Leggings", s -> bool(s.showLeggings()), s -> s.setShowLeggings(!s.showLeggings()), s -> s.resetField("showLeggings"));
        this.addRow("Boots", s -> bool(s.showBoots()), s -> s.setShowBoots(!s.showBoots()), s -> s.resetField("showBoots"));

        this.addHeader("Durability");
        this.addRow("Show Numbers", s -> bool(s.showDurability()), s -> s.setShowDurability(!s.showDurability()), s -> s.resetField("showDurability"));
        this.addRow("Number Format", s -> s.durabilityFormat().label(), s -> s.setDurabilityFormat(s.durabilityFormat().next()), s -> s.resetField("durabilityFormat"));

        this.addHeader("Appearance");
        this.addRow("Background", s -> bool(s.showBackground()), s -> s.setShowBackground(!s.showBackground()), s -> s.resetField("showBackground"));
        this.addRow("Background Opacity", s -> s.backgroundOpacity() + "%", s -> s.setBackgroundOpacity(cycle(s.backgroundOpacity(), 0, 100, 10)), s -> s.resetField("backgroundOpacity"));
        this.addRow("Text Shadow", s -> bool(s.textShadow()), s -> s.setTextShadow(!s.textShadow()), s -> s.resetField("textShadow"));
        this.addRow("Outline", s -> bool(s.showOutline()), s -> s.setShowOutline(!s.showOutline()), s -> s.resetField("showOutline"));
        this.addRow("Padding X", s -> Integer.toString(s.paddingX()), s -> s.setPaddingX(cycle(s.paddingX(), 0, 16, 1)), s -> s.resetField("paddingX"));
        this.addRow("Padding Y", s -> Integer.toString(s.paddingY()), s -> s.setPaddingY(cycle(s.paddingY(), 0, 12, 1)), s -> s.resetField("paddingY"));
    }

    private static String bool(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private static int cycle(int current, int min, int max, int step) {
        int next = current + step;
        return next > max ? min : next;
    }
}
