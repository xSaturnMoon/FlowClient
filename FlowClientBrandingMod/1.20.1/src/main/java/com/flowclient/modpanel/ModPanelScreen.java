package com.flowclient.modpanel;

import com.flowclient.compat.FlowGfx;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ModPanelScreen extends Screen {
    protected static final int BUTTON_WIDTH = 200;
    protected static final int BUTTON_HEIGHT = 22;
    private static final int DESCRIPTION_TOP = 30;
    private static final int DESCRIPTION_GAP = 10;
    private static final int HEADER_TITLE_GAP = 4;

    protected final Screen parent;
    protected final String modDescriptionKey;

    protected ModPanelScreen(Component title, Screen parent) {
        this(title, parent, null);
    }

    protected ModPanelScreen(Component title, Screen parent, String modDescriptionKey) {
        super(title);
        this.parent = parent;
        this.modDescriptionKey = modDescriptionKey;
    }

    protected int centerX(int width) {
        return (this.width - width) / 2;
    }

    protected ModPanelButton panelButton(Component label, int x, int y, int width, Runnable onPress) {
        return new ModPanelButton(x, y, width, BUTTON_HEIGHT, label, onPress, false);
    }

    protected void addBackButton() {
        this.addRenderableWidget(panelButton(
                Component.literal("Back"),
                this.centerX(BUTTON_WIDTH),
                this.height - 32,
                BUTTON_WIDTH,
                this::goBack
        ));
    }

    protected void goBack() {
        Screen target = this.parent;
        if (target instanceof ModPanelMenuScreen) {
            target = new ModPanelMenuScreen();
        } else if (target instanceof ModListPanelScreen listParent) {
            target = new ModListPanelScreen(listParent.parent);
        }

        if (target == null) {
            this.minecraft.setScreen(null);
        } else {
            this.minecraft.setScreen(target);
        }
    }

    @Override
    protected void init() {
        this.initPanel();
    }

    protected void rebuildWidgets() {
        this.clearWidgets();
        this.initPanel();
    }

    protected void refreshPanel() {
        this.rebuildWidgets();
    }

    protected int settingsFrameTop() {
        return 36;
    }

    protected boolean showsModDescription() {
        return this.modDescriptionKey != null && !this.modDescriptionKey.isBlank();
    }

    protected void initPanel() {
    }

    protected String resolveModDescription() {
        return this.modDescriptionKey == null ? null : ModDescriptions.get(this.modDescriptionKey);
    }

    protected int descriptionBlockHeight(int frameWidth) {
        String description = this.resolveModDescription();
        if (description == null || description.isBlank()) {
            return 0;
        }
        return ModDescriptions.blockHeight(this.font, description, frameWidth - 24)
                + this.font.lineHeight
                + HEADER_TITLE_GAP
                + DESCRIPTION_GAP;
    }

    protected int contentFrameY(int baseY, int frameWidth) {
        return baseY + this.descriptionBlockHeight(frameWidth);
    }

    protected void drawModDescription(GuiGraphics graphics, int frameX, int frameWidth) {
        if (!this.showsModDescription()) {
            return;
        }

        String description = this.resolveModDescription();
        if (description == null || description.isBlank()) {
            return;
        }

        int textX = frameX + 12;
        int textWidth = frameWidth - 24;
        int titleY = DESCRIPTION_TOP;
        FlowGfx.text(graphics, this.font, Component.literal(this.modDescriptionKey), textX, titleY, ModPanelTheme.TEXT);

        int descriptionY = titleY + this.font.lineHeight + HEADER_TITLE_GAP;
        ModDescriptions.draw(graphics, this.font, description, textX, descriptionY, textWidth);

        int separatorY = descriptionY + ModDescriptions.blockHeight(this.font, description, textWidth) + 4;
        graphics.fill(textX, separatorY, textX + textWidth, separatorY + 1, ModPanelTheme.SEPARATOR);
    }

    protected void drawContentFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, ModPanelTheme.BG);
        ModPanelTheme.drawBorder(graphics, x, y, width, height, ModPanelTheme.BORDER_MUTED);
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        com.flowclient.mods.FlowModConfig.flushSave();
        super.onClose();
    }
}
