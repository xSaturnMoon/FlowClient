package com.flowclient.modpanel;

import com.flowclient.mods.environment.TimeChangerLogic;
import com.flowclient.mods.environment.TimeChangerSettings;
import com.flowclient.mods.environment.TimeMode;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TimeChangerSettingsScreen extends ModSettingsPanelScreen<TimeChangerSettings> {
    private static final Component TITLE = Component.literal("Time Changer");

    private int scrollOffset;

    public TimeChangerSettingsScreen(Screen parent) {
        super(TITLE, parent, "Time Changer");
    }

    @Override
    protected TimeChangerSettings settings() {
        return TimeChangerSettings.get();
    }

    @Override
    protected void persistSettings() {
        TimeChangerSettings.get().persist();
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

        TimeChangerSettings current = this.settings();
        for (int i = 0; i < this.settingRows().size(); i++) {
            ModSettingRow<TimeChangerSettings> row = this.settingRows().get(i);
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

        if (this.settings().mode() == TimeMode.CUSTOM) {
            int sliderY = frameY + FRAME_INSET + this.settingRows().size() * (ROW_HEIGHT + ROW_GAP) - this.scrollOffset;
            if (sliderY + ROW_HEIGHT >= frameY && sliderY <= frameY + visibleHeight) {
                this.addRenderableWidget(new CustomTimeSlider(
                        frameX + FRAME_INSET,
                        sliderY,
                        frameWidth - FRAME_INSET * 2,
                        ROW_HEIGHT,
                        this.settings().customTime()
                ));
            }
        }

        this.addBackButton();
    }

    @Override
    protected int extraRowsReserved() {
        return this.settings().mode() == TimeMode.CUSTOM ? 1 : 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (this.isHoveringSettingsFrame(mouseX, mouseY)) {
            this.scrollOffset = (int) Math.max(0, this.scrollOffset - scrollDelta * 16);
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Time Settings");
        this.addRow("Mode", s -> s.mode().label(), s -> s.setMode(s.mode().next()), s -> s.resetField("mode"));
        this.addRow("Lock Day/Night Cycle", s -> boolLabel(s.lockCycle()), s -> s.setLockCycle(!s.lockCycle()), s -> s.resetField("lockCycle"));

        TimeChangerSettings settings = this.settings();
        if (settings.mode() == TimeMode.PRESET) {
            this.addRow(
                    "Preset",
                    s -> s.preset().label() + " (" + TimeChangerLogic.formatTicks(s.preset().ticks()) + ")",
                    s -> s.setPreset(s.preset().next()),
                    s -> s.resetField("preset")
            );
        }
        if (settings.mode() == TimeMode.REAL_TIME) {
            this.addRow(
                    "Current Time",
                    s -> TimeChangerLogic.formatTicks(TimeChangerLogic.resolveTime(s)),
                    s -> {},
                    s -> s.resetField("lockedTime")
            );
        }
    }

    private static String boolLabel(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private static final class CustomTimeSlider extends AbstractSliderButton {
        CustomTimeSlider(int x, int y, int width, int height, long currentTime) {
            super(x, y, width, height, Component.empty(), currentTime / 23999.0);
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            long ticks = Math.round(this.value * 23999.0);
            this.setMessage(Component.literal("Custom Time: " + TimeChangerLogic.formatTicks(ticks)));
        }

        @Override
        protected void applyValue() {
            long ticks = Math.round(this.value * 23999.0);
            TimeChangerSettings.get().setCustomTime(ticks);
            TimeChangerSettings.get().persist();
        }
    }
}
