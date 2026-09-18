package com.flowclient.modpanel;

import com.flowclient.compat.FlowGfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ModListPanelScreen extends ModPanelScreen {
    private static final Component TITLE = Component.literal("Mods");

    private static final int FRAME_WIDTH = 540;
    private static final int FRAME_HEIGHT = 380;
    private static final int PADDING = 8;
    private static final int SIDEBAR_WIDTH = 96;
    private static final int SEARCH_HEIGHT = 18;
    private static final int SEARCH_GAP = 6;
    private static final int SIDEBAR_BUTTON_HEIGHT = 22;
    private static final int SIDEBAR_GAP = 4;
    private static final int ICON_SIZE = 16;
    private static final int ICON_LEFT = 6;
    private static final int ICON_TEXT_GAP = 6;
    private static final int CONTROL_PADDING = 6;
    private static final int GEAR_SWITCH_GAP = 6;
    private static final int COLUMN_GAP = 6;

    private ModCategory selectedCategory = ModCategory.VISUAL;
    private String searchQuery = "";
    private int scrollOffset;
    private ModSearchField searchField;

    private ModEntryListWidget listWidget;
    private int switchX;
    private int gearX;

    public ModListPanelScreen(Screen parent) {
        super(TITLE, parent);
    }

    @Override
    protected void rebuildWidgets() {
        boolean searchFocused = this.searchField != null && this.searchField.isFocused();
        int cursor = searchFocused ? this.searchField.getCursorPosition() : 0;
        super.rebuildWidgets();
        if (searchFocused && this.searchField != null) {
            this.setFocused(this.searchField);
            this.searchField.setCursorPosition(cursor);
        }
    }

    @Override
    protected void initPanel() {
        int frameX = this.frameX();
        int frameY = this.frameY();
        int innerX = frameX + PADDING;
        int innerY = frameY + PADDING;
        int innerWidth = FRAME_WIDTH - PADDING * 2;
        int searchBlockHeight = SEARCH_HEIGHT + SEARCH_GAP;

        this.searchField = new ModSearchField(this.font, innerX, innerY, innerWidth, SEARCH_HEIGHT, Component.literal("Search"));
        this.searchField.setMaxLength(64);
        this.searchField.setHint(Component.literal("Search flow mods..."));
        this.searchField.setValue(this.searchQuery);
        this.searchField.setResponder(value -> {
            this.searchQuery = value;
            this.scrollOffset = 0;
            this.rebuildWidgets();
        });
        this.addRenderableWidget(this.searchField);

        this.listWidget = ModEntryListWidget.layout(
                this.font,
                frameX,
                frameY,
                FRAME_WIDTH,
                FRAME_HEIGHT,
                PADDING,
                searchBlockHeight,
                SIDEBAR_WIDTH,
                COLUMN_GAP
        );

        int bodyTop = innerY + searchBlockHeight;
        this.switchX = this.listWidget.x() + this.listWidget.width() - CONTROL_PADDING - ToggleSwitchWidget.TRACK_WIDTH;
        this.gearX = this.switchX - GEAR_SWITCH_GAP - ModSettingsIconButton.SIZE;

        boolean searchActive = this.isSearchActive();
        int sidebarY = bodyTop;
        for (ModCategory category : ModCategory.values()) {
            ModCategorySidebarButton button = new ModCategorySidebarButton(
                    innerX,
                    sidebarY,
                    SIDEBAR_WIDTH,
                    SIDEBAR_BUTTON_HEIGHT,
                    category.displayName(),
                    () -> {
                        this.selectedCategory = category;
                        this.scrollOffset = 0;
                        this.rebuildWidgets();
                    }
            );
            button.setSelected(!searchActive && category == this.selectedCategory);
            button.setDimmed(searchActive);
            this.addRenderableWidget(button);
            sidebarY += SIDEBAR_BUTTON_HEIGHT + SIDEBAR_GAP;
        }

        List<ModEntry> visibleEntries = ModRegistry.visible(this.selectedCategory, this.searchQuery);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.listWidget.maxScroll(visibleEntries.size())));

        for (int index = 0; index < visibleEntries.size(); index++) {
            ModEntry entry = visibleEntries.get(index);
            int rowY = this.listWidget.rowY(index, this.scrollOffset);
            if (!this.listWidget.isRowVisible(rowY)) {
                continue;
            }

            if (entry.hasDedicatedSettingsScreen()) {
                this.addRenderableWidget(ModSettingsButtons.create(
                        this.gearX,
                        rowY + (ModEntryListWidget.ROW_HEIGHT - ModSettingsIconButton.SIZE) / 2,
                        () -> this.minecraft.setScreen(entry.openSettings(this))
                ));
            }

            this.addRenderableWidget(new ToggleSwitchWidget(
                    this.switchX,
                    rowY + (ModEntryListWidget.ROW_HEIGHT - ToggleSwitchWidget.TRACK_HEIGHT) / 2,
                    entry::isEnabled,
                    () -> {
                        entry.toggle();
                        this.rebuildWidgets();
                    }
            ));
        }

        this.addBackButton();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (this.listWidget != null && this.listWidget.contains(mouseX, mouseY)) {
            this.scrollOffset = (int) Math.max(0, this.scrollOffset - scrollDelta * ModEntryListWidget.ROW_HEIGHT);
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int frameX = this.frameX();
        int frameY = this.frameY();
        this.drawPanelFrame(graphics, frameX, frameY, FRAME_WIDTH, FRAME_HEIGHT);

        int innerX = frameX + PADDING;
        int bodyTop = frameY + PADDING + SEARCH_HEIGHT + SEARCH_GAP;
        int separatorX = innerX + SIDEBAR_WIDTH + 2;
        graphics.fill(separatorX, bodyTop, separatorX + 1, this.listWidget.bottom(), ModPanelTheme.SEPARATOR);

        List<ModEntry> visibleEntries = ModRegistry.visible(this.selectedCategory, this.searchQuery);
        Font font = Minecraft.getInstance().font;
        boolean showCategoryBadge = this.isSearchActive();

        for (int index = 0; index < visibleEntries.size(); index++) {
            ModEntry entry = visibleEntries.get(index);
            int rowY = this.listWidget.rowY(index, this.scrollOffset);
            if (!this.listWidget.isRowVisible(rowY)) {
                continue;
            }

            boolean hovered = mouseX >= this.listWidget.x()
                    && mouseX <= this.listWidget.x() + this.listWidget.width()
                    && mouseY >= rowY
                    && mouseY <= rowY + ModEntryListWidget.ROW_HEIGHT;
            if (hovered) {
                graphics.fill(
                        this.listWidget.x(),
                        rowY,
                        this.listWidget.x() + this.listWidget.width(),
                        rowY + ModEntryListWidget.ROW_HEIGHT,
                        ModPanelTheme.BG_HOVER
                );
            }

            if (index > 0) {
                graphics.fill(this.listWidget.x(), rowY, this.listWidget.x() + this.listWidget.width(), rowY + 1, ModPanelTheme.SEPARATOR);
            }

            int iconY = rowY + (ModEntryListWidget.ROW_HEIGHT - ICON_SIZE) / 2;
            ModIcons.draw(graphics, entry, this.listWidget.x() + ICON_LEFT, iconY);

            int textX = this.listWidget.x() + ICON_LEFT + ICON_SIZE + ICON_TEXT_GAP;
            int textY = rowY + (ModEntryListWidget.ROW_HEIGHT - font.lineHeight) / 2;
            FlowGfx.text(graphics, font, Component.literal(entry.displayName()), textX, textY, ModPanelTheme.TEXT);

            if (showCategoryBadge) {
                String badge = " · " + entry.category().label();
                int badgeX = textX + font.width(entry.displayName());
                FlowGfx.text(graphics, font, badge, badgeX, textY, ModPanelTheme.TEXT_MUTED);
            }
        }

        graphics.fill(
                this.listWidget.x(),
                this.listWidget.footerSeparatorY(),
                this.listWidget.x() + this.listWidget.width(),
                this.listWidget.footerSeparatorY() + ModEntryListWidget.FOOTER_SEPARATOR,
                ModPanelTheme.SEPARATOR
        );
        this.drawFooter(graphics, visibleEntries);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanelFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, ModPanelTheme.BG);
        ModPanelTheme.drawBorder(graphics, x, y, width, height, ModPanelTheme.BORDER_MUTED);
    }

    private void drawFooter(GuiGraphics graphics, List<ModEntry> visibleEntries) {
        Font font = Minecraft.getInstance().font;
        String footer;
        if (this.isSearchActive()) {
            long enabledCount = visibleEntries.stream().filter(ModEntry::isEnabled).count();
            footer = "Search · " + visibleEntries.size() + " mods · " + enabledCount + " on";
        } else {
            long enabledCount = visibleEntries.stream().filter(ModEntry::isEnabled).count();
            footer = this.selectedCategory.label() + " · " + visibleEntries.size() + " mods · " + enabledCount + " on";
        }
        FlowGfx.text(graphics, font, footer, this.listWidget.x(), this.listWidget.footerTextY(), ModPanelTheme.TEXT_MUTED);
    }

    private boolean isSearchActive() {
        return this.searchQuery != null && !this.searchQuery.isBlank();
    }

    private int frameX() {
        return this.centerX(FRAME_WIDTH);
    }

    private int frameY() {
        return 32;
    }
}
