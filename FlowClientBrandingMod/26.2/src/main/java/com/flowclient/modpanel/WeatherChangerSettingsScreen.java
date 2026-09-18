package com.flowclient.modpanel;

import com.flowclient.mods.environment.WeatherChangerSettings;
import com.flowclient.mods.environment.WeatherMode;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class WeatherChangerSettingsScreen extends ModSettingsPanelScreen<WeatherChangerSettings> {
    private static final Component TITLE = Component.literal("Weather Changer");

    private int scrollOffset;

    public WeatherChangerSettingsScreen(Screen parent) {
        super(TITLE, parent, "Weather Changer");
    }

    @Override
    protected WeatherChangerSettings settings() {
        return WeatherChangerSettings.get();
    }

    @Override
    protected void persistSettings() {
        WeatherChangerSettings.get().persist();
    }

    @Override
    protected void init() {
        this.clearSettingRows();
        this.buildSettingRows();
        this.rebuildWidgets();
    }

    @Override
    protected void initPanel() {
        int frameX = this.settingsFrameX();
        int frameY = this.settingsFrameY();
        int frameWidth = this.settingsFrameWidth();
        int visibleHeight = this.settingsFrameHeight();
        int totalRows = this.settingRows().size() + this.extraRowsReserved();
        int maxScroll = Math.max(0, totalRows * (ROW_HEIGHT + ROW_GAP) + 16 - visibleHeight);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));

        WeatherChangerSettings current = this.settings();
        for (int i = 0; i < this.settingRows().size(); i++) {
            ModSettingRow<WeatherChangerSettings> row = this.settingRows().get(i);
            int y = frameY + FRAME_INSET + i * (ROW_HEIGHT + ROW_GAP) - this.scrollOffset;
            if (row.header() || y + ROW_HEIGHT < frameY || y > frameY + visibleHeight) {
                continue;
            }

            int rowWidth = frameWidth - FRAME_INSET * 2 - RESET_WIDTH - 4;
            this.addRenderableWidget(new ModPanelButton(
                    frameX + FRAME_INSET,
                    y,
                    rowWidth,
                    ROW_HEIGHT,
                    row.label(current),
                    () -> {
                        row.click(this.settings());
                        this.persistSettings();
                        this.rebuildWidgets();
                    }
            ));

            this.addRenderableWidget(new ModSettingsResetButton(
                    frameX + frameWidth - FRAME_INSET - RESET_WIDTH,
                    y + (ROW_HEIGHT - RESET_WIDTH) / 2,
                    () -> {
                        row.reset(this.settings());
                        this.persistSettings();
                        this.rebuildWidgets();
                    }
            ));
        }

        if (this.settings().mode() == WeatherMode.CUSTOM) {
            int rainY = frameY + FRAME_INSET + this.settingRows().size() * (ROW_HEIGHT + ROW_GAP) - this.scrollOffset;
            int thunderY = rainY + ROW_HEIGHT + 4;
            if (rainY + ROW_HEIGHT >= frameY && rainY <= frameY + visibleHeight) {
                this.addRenderableWidget(new WeatherSlider(
                        frameX + FRAME_INSET,
                        rainY,
                        frameWidth - FRAME_INSET * 2,
                        ROW_HEIGHT,
                        "Rain Intensity",
                        this.settings().customRain(),
                        value -> WeatherChangerSettings.get().setCustomRain(value)
                ));
            }
            if (thunderY + ROW_HEIGHT >= frameY && thunderY <= frameY + visibleHeight) {
                this.addRenderableWidget(new WeatherSlider(
                        frameX + FRAME_INSET,
                        thunderY,
                        frameWidth - FRAME_INSET * 2,
                        ROW_HEIGHT,
                        "Thunder Intensity",
                        this.settings().customThunder(),
                        value -> WeatherChangerSettings.get().setCustomThunder(value)
                ));
            }
        }

        this.addBackButton();
    }

    @Override
    protected int extraRowsReserved() {
        return this.settings().mode() == WeatherMode.CUSTOM ? 2 : 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isHoveringSettingsFrame(mouseX, mouseY)) {
            this.scrollOffset = (int) Math.max(0, this.scrollOffset - scrollY * 16);
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Weather Settings");
        this.addRow("Mode", s -> s.mode().label(), s -> s.setMode(s.mode().next()), s -> s.resetField("mode"));
        this.addRow("Lock Weather", s -> boolLabel(s.lockWeather()), s -> s.setLockWeather(!s.lockWeather()), s -> s.resetField("lockWeather"));

        if (this.settings().mode() == WeatherMode.PRESET) {
            this.addRow("Preset", s -> s.preset().label(), s -> s.setPreset(s.preset().next()), s -> s.resetField("preset"));
        }

        this.addRow("Current Rain", s -> percentLabel(s.resolveRainLevel()), s -> {}, s -> s.resetField("customRain"));
        this.addRow("Current Thunder", s -> percentLabel(s.resolveThunderLevel()), s -> {}, s -> s.resetField("customThunder"));
    }

    private static String boolLabel(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private static String percentLabel(float value) {
        return Math.round(value * 100.0f) + "%";
    }

    private static final class WeatherSlider extends AbstractSliderButton {
        private final String prefix;
        private final Consumer<Float> onApply;

        WeatherSlider(int x, int y, int width, int height, String prefix, float value, Consumer<Float> onApply) {
            super(x, y, width, height, Component.empty(), value);
            this.prefix = prefix;
            this.onApply = onApply;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(this.prefix + ": " + Math.round(this.value * 100.0) + "%"));
        }

        @Override
        protected void applyValue() {
            this.onApply.accept((float) this.value);
            WeatherChangerSettings.get().persist();
        }
    }
}
