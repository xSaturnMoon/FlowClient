package com.flowclient.modpanel;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.keystrokes.KeystrokesSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KeystrokesSettingsScreen extends ModSettingsPanelScreen<KeystrokesSettings> {
    private static final Component TITLE = Component.literal("Keystrokes");

    public KeystrokesSettingsScreen(Screen parent) {
        super(TITLE, parent, "Keystrokes");
    }

    @Override
    protected KeystrokesSettings settings() {
        return KeystrokesSettings.get();
    }

    @Override
    protected void persistSettings() {
        KeystrokesSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.KEYSTROKES);
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Style");
        this.addRow("Style Preset", s -> s.style().label(), s -> {
            s.setStyle(s.style().next());
            s.applyStyleDefaults();
        }, s -> s.resetField("style"));
        this.addRow("Color Theme", s -> s.theme().label(), s -> s.setTheme(s.theme().next()), s -> s.resetField("theme"));

        this.addHeader("Visibility");
        this.addRow("Movement Keys", s -> bool(s.showMovementKeys()), s -> s.setShowMovementKeys(!s.showMovementKeys()), s -> s.resetField("showMovementKeys"));
        this.addRow("Mouse Buttons", s -> bool(s.showMouseButtons()), s -> s.setShowMouseButtons(!s.showMouseButtons()), s -> s.resetField("showMouseButtons"));
        this.addRow("Spacebar", s -> bool(s.showSpace()), s -> s.setShowSpace(!s.showSpace()), s -> s.resetField("showSpace"));
        this.addRow("CPS Display", s -> s.cpsDisplay().label(), s -> s.setCpsDisplay(s.cpsDisplay().next()), s -> s.resetField("cpsDisplay"));

        this.addHeader("Sizing");
        this.addRow("Key Width", s -> Integer.toString(s.keyWidth()), s -> s.setKeyWidth(cycle(s.keyWidth(), 18, 40, 2)), s -> s.resetField("keyWidth"));
        this.addRow("Key Height", s -> Integer.toString(s.keyHeight()), s -> s.setKeyHeight(cycle(s.keyHeight(), 14, 30, 2)), s -> s.resetField("keyHeight"));
        this.addRow("Key Gap", s -> Integer.toString(s.keyGap()), s -> s.setKeyGap(cycle(s.keyGap(), 1, 8, 1)), s -> s.resetField("keyGap"));
        this.addRow("Corner Radius", s -> Integer.toString(s.cornerRadius()), s -> s.setCornerRadius(cycle(s.cornerRadius(), 0, 8, 1)), s -> s.resetField("cornerRadius"));
        this.addRow("Panel Padding", s -> Integer.toString(s.panelPadding()), s -> s.setPanelPadding(cycle(s.panelPadding(), 0, 12, 1)), s -> s.resetField("panelPadding"));

        this.addHeader("Effects");
        this.addRow("Panel Background", s -> bool(s.showPanelBackground()), s -> s.setShowPanelBackground(!s.showPanelBackground()), s -> s.resetField("showPanelBackground"));
        this.addRow("Key Borders", s -> bool(s.showBorders()), s -> s.setShowBorders(!s.showBorders()), s -> s.resetField("showBorders"));
        this.addRow("Text Shadow", s -> bool(s.textShadow()), s -> s.setTextShadow(!s.textShadow()), s -> s.resetField("textShadow"));
        this.addRow("Pressed Glow", s -> bool(s.pressedGlow()), s -> s.setPressedGlow(!s.pressedGlow()), s -> s.resetField("pressedGlow"));
    }

    private static String bool(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private static int cycle(int current, int min, int max, int step) {
        int next = current + step;
        return next > max ? min : next;
    }
}
