package com.flowclient.modpanel;

import com.flowclient.compat.FlowGfx;

import java.util.ArrayList;
import java.util.List;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ModSettingsPanelScreen<T> extends ModPanelScreen {
    protected static final int ROW_HEIGHT = 22;
    protected static final int ROW_GAP = 2;
    protected static final int RESET_WIDTH = ModSettingsResetButton.SIZE;
    protected static final int FRAME_INSET = 8;

    private final List<ModSettingRow<T>> rows = new ArrayList<>();
    private int scrollOffset;
    private boolean rowsBuilt;

    protected ModSettingsPanelScreen(Component title, Screen parent, String modDescriptionKey) {
        super(title, parent, modDescriptionKey);
    }

    protected abstract T settings();

    protected abstract void persistSettings();

    protected abstract void buildSettingRows();

    protected void clearSettingRows() {
        this.rows.clear();
    }

    protected void addHeader(String title) {
        this.rows.add(ModSettingRow.header(title));
    }

    protected void addRow(
            String title,
            java.util.function.Function<T, String> valueText,
            java.util.function.Consumer<T> onClick,
            java.util.function.Consumer<T> resetAction
    ) {
        this.rows.add(ModSettingRow.of(title, valueText, onClick, resetAction));
    }

    protected List<ModSettingRow<T>> settingRows() {
        return this.rows;
    }

    protected int extraRowsReserved() {
        return 0;
    }

    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
    }

    protected int settingsFrameWidth() {
        return Math.min(this.width - 40, 420);
    }

    protected int settingsFrameHeight() {
        return Math.min(this.height - 80, this.height - 80);
    }

    protected int settingsFrameX() {
        return this.centerX(this.settingsFrameWidth());
    }

    protected int settingsFrameY() {
        return this.contentFrameY(24, this.settingsFrameWidth());
    }

    @Override
    protected void init() {
        if (!this.rowsBuilt) {
            this.buildSettingRows();
            this.rowsBuilt = true;
        }
        this.rebuildWidgets();
    }

    @Override
    protected void initPanel() {
        int frameX = this.settingsFrameX();
        int frameY = this.settingsFrameY();
        int frameWidth = this.settingsFrameWidth();
        int visibleHeight = this.settingsFrameHeight();
        int totalRows = this.rows.size() + this.extraRowsReserved();
        int maxScroll = Math.max(0, totalRows * (ROW_HEIGHT + ROW_GAP) + 16 - visibleHeight);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));

        T current = this.settings();
        for (int i = 0; i < this.rows.size(); i++) {
            ModSettingRow<T> row = this.rows.get(i);
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

        this.addExtraWidgets(frameX, frameY, frameWidth, visibleHeight, this.rows.size());
        this.addBackButton();
    }

    protected void addHudPositionButton(int frameX, int frameY, int frameWidth, int visibleHeight, int rowIndex, HudElement element) {
        int y = frameY + FRAME_INSET + rowIndex * (ROW_HEIGHT + ROW_GAP) - this.scrollOffset;
        if (y + ROW_HEIGHT < frameY || y > frameY + visibleHeight) {
            return;
        }

        this.addRenderableWidget(new ModPanelButton(
                frameX + FRAME_INSET,
                y,
                frameWidth - FRAME_INSET * 2,
                ROW_HEIGHT,
                Component.literal("HUD Position & Size"),
                () -> this.minecraft.setScreen(new HudModSettingsScreen(this, element))
        ));
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int frameX = this.settingsFrameX();
        int frameY = this.settingsFrameY();
        int frameWidth = this.settingsFrameWidth();
        int frameHeight = this.settingsFrameHeight();

        this.drawModDescription(graphics, frameX, frameWidth);
        this.drawContentFrame(graphics, frameX, frameY, frameWidth, frameHeight);
        this.drawSectionHeaders(graphics, frameX, frameY, frameWidth, frameHeight);
        this.drawRowSeparators(graphics, frameX, frameY, frameWidth, frameHeight);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSectionHeaders(GuiGraphics graphics, int frameX, int frameY, int frameWidth, int frameHeight) {
        for (int i = 0; i < this.rows.size(); i++) {
            ModSettingRow<T> row = this.rows.get(i);
            if (!row.header()) {
                continue;
            }

            int y = frameY + FRAME_INSET + i * (ROW_HEIGHT + ROW_GAP) - this.scrollOffset;
            if (y + ROW_HEIGHT < frameY || y > frameY + frameHeight) {
                continue;
            }

            FlowGfx.text(graphics, this.font, row.title(), frameX + FRAME_INSET, y + 6, ModPanelTheme.ACCENT_BLUE);
        }
    }

    private void drawRowSeparators(GuiGraphics graphics, int frameX, int frameY, int frameWidth, int frameHeight) {
        boolean drewAny = false;
        for (int i = 0; i < this.rows.size(); i++) {
            ModSettingRow<T> row = this.rows.get(i);
            if (row.header()) {
                continue;
            }

            int y = frameY + FRAME_INSET + i * (ROW_HEIGHT + ROW_GAP) - this.scrollOffset;
            if (y + ROW_HEIGHT < frameY || y > frameY + frameHeight) {
                continue;
            }

            if (drewAny) {
                graphics.fill(
                        frameX + FRAME_INSET,
                        y,
                        frameX + frameWidth - FRAME_INSET,
                        y + 1,
                        ModPanelTheme.SEPARATOR
                );
            }
            drewAny = true;
        }
    }

    protected boolean isHoveringSettingsFrame(double mouseX, double mouseY) {
        return mouseX >= this.settingsFrameX()
                && mouseX <= this.settingsFrameX() + this.settingsFrameWidth()
                && mouseY >= this.settingsFrameY()
                && mouseY <= this.settingsFrameY() + this.settingsFrameHeight();
    }
}
