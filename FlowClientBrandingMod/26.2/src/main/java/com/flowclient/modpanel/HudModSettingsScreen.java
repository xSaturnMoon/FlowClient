package com.flowclient.modpanel;

import com.flowclient.mods.FlowModConfig;
import com.flowclient.mods.armor.ArmorHudRenderer;
import com.flowclient.mods.fps.FpsCounterRenderer;
import com.flowclient.mods.blockspeed.BlockSpeedRenderer;
import com.flowclient.mods.blockbreak.BlockBreakProgressRenderer;
import com.flowclient.mods.armordurability.ArmorDurabilityAlertRenderer;
import com.flowclient.mods.cooldown.ItemCooldownHudRenderer;
import com.flowclient.mods.battery.BatteryHudRenderer;
import com.flowclient.mods.serveraddress.ServerAddressHudRenderer;
import com.flowclient.mods.clock.FlowClockRenderer;
import com.flowclient.mods.media.MediaHudRenderer;
import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayout;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudScale;
import com.flowclient.mods.keystrokes.KeystrokesRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudModSettingsScreen extends ModPanelScreen {
    private final HudElement element;

    private ModPanelField xField;
    private ModPanelField yField;
    private ModPanelField scaleField;
    private boolean suppressResponder;

    public HudModSettingsScreen(Screen parent, HudElement element) {
        super(Component.literal(element.getDisplayName() + " HUD"), parent, element.getDisplayName());
        this.element = element;
    }

    @Override
    protected String resolveModDescription() {
        return ModDescriptions.forHudElement(this.element);
    }

    @Override
    protected void init() {
        this.initPanel();
    }

    @Override
    protected void initPanel() {
        int frameX = this.frameX();
        int frameY = this.frameY();
        int frameWidth = this.frameWidth();
        int fieldWidth = frameWidth - 24;
        int y = frameY + 12;

        this.addRenderableWidget(new ModPanelButton(
                frameX + 12,
                y,
                fieldWidth,
                BUTTON_HEIGHT,
                Component.literal("Reset Position"),
                () -> {
                    HudLayoutManager.get().resetPosition(this.element);
                    this.rebuildWidgets();
                }
        ));
        y += BUTTON_HEIGHT + 6;

        this.addRenderableWidget(new ModPanelButton(
                frameX + 12,
                y,
                fieldWidth,
                BUTTON_HEIGHT,
                Component.literal("Reset Size"),
                () -> {
                    HudLayoutManager.get().resetScale(this.element);
                    this.rebuildWidgets();
                }
        ));
        y += BUTTON_HEIGHT + 12;

        HudLayout layout = HudLayoutManager.get().layout(this.element);
        int width = this.elementWidth();
        int height = this.elementHeight();

        int currentX = layout.getRawX() == HudLayout.UNSET
                ? HudLayoutManager.get().resolveX(this.element, width, height)
                : layout.getRawX();
        int currentY = layout.getRawY() == HudLayout.UNSET
                ? HudLayoutManager.get().resolveY(this.element, width, height)
                : layout.getRawY();

        this.xField = this.addNumericField("Position X", frameX + 12, y, fieldWidth, String.valueOf(currentX));
        y += 28;
        this.yField = this.addNumericField("Position Y", frameX + 12, y, fieldWidth, String.valueOf(currentY));
        y += 28;
        this.scaleField = this.addDecimalField("Size", frameX + 12, y, fieldWidth, HudScale.format(layout.getScale()));
        y += 36;

        this.addRenderableWidget(new ModPanelButton(
                frameX + 12,
                y,
                fieldWidth,
                BUTTON_HEIGHT,
                Component.literal("Open Position Editor"),
                () -> this.minecraft.gui.setScreen(new ModPositionPanelScreen(this))
        ));

        this.addBackButton();
    }

    private ModPanelField addNumericField(String label, int x, int y, int width, String value) {
        ModPanelField field = new ModPanelField(this.font, x, y + 12, width, 18, Component.literal(label));
        field.setValue(value);
        field.setResponder(text -> this.onFieldChanged());
        this.addRenderableWidget(field);
        return field;
    }

    private ModPanelField addDecimalField(String label, int x, int y, int width, String value) {
        ModPanelField field = new ModPanelField(this.font, x, y + 12, width, 18, Component.literal(label));
        field.setValue(value);
        field.setResponder(text -> this.onFieldChanged());
        this.addRenderableWidget(field);
        return field;
    }

    private void onFieldChanged() {
        if (this.suppressResponder) {
            return;
        }
        this.applyFields(false);
    }

    private void applyFields(boolean strict) {
        if (this.xField == null || this.yField == null || this.scaleField == null) {
            return;
        }

        try {
            String xText = this.xField.getValue().trim();
            String yText = this.yField.getValue().trim();
            String scaleText = this.scaleField.getValue().trim();

            Integer x = tryParseInt(xText);
            Integer y = tryParseInt(yText);
            Float scale = HudScale.tryParse(scaleText);

            if (x != null && y != null) {
                HudLayoutManager.get().setPositionQuiet(this.element, x, y);
            }

            if (scale != null) {
                HudLayoutManager.get().setScaleQuiet(this.element, scale);
            } else if (strict) {
                this.suppressResponder = true;
                this.scaleField.setValue(HudScale.format(HudLayoutManager.get().layout(this.element).getScale()));
                this.suppressResponder = false;
            }

            if (strict) {
                FlowModConfig.flushSave();
            }
        } catch (Exception ignored) {
            if (strict) {
                this.rebuildWidgets();
            }
        }
    }

    private static Integer tryParseInt(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public void onClose() {
        this.applyFields(true);
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderPreview(graphics);
        this.drawModDescription(graphics, this.frameX(), this.frameWidth());
        this.drawContentFrame(graphics, this.frameX(), this.frameY(), this.frameWidth(), this.frameHeight());

        int labelX = this.frameX() + 12;
        int y = this.frameY() + 12 + (BUTTON_HEIGHT + 6) * 2 + 12;
        graphics.text(this.font, "Position X", labelX, y, 0xFFCCCCCC, false);
        y += 28;
        graphics.text(this.font, "Position Y", labelX, y, 0xFFCCCCCC, false);
        y += 28;
        graphics.text(this.font, "Size", labelX, y, 0xFFCCCCCC, false);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPreview(GuiGraphicsExtractor graphics) {
        switch (this.element) {
            case ARMOR_HUD -> ArmorHudRenderer.render(this.minecraft, graphics, this.font, true);
            case KEYSTROKES -> KeystrokesRenderer.render(this.minecraft, graphics, this.font, true);
            case FPS_COUNTER -> FpsCounterRenderer.render(this.minecraft, graphics, this.font, true);
            case FLOW_CLOCK -> FlowClockRenderer.render(this.minecraft, graphics, this.font, true);
            case BLOCK_SPEED -> BlockSpeedRenderer.render(this.minecraft, graphics, this.font, true);
            case BLOCK_BREAK_PROGRESS -> BlockBreakProgressRenderer.render(this.minecraft, graphics, this.font, true);
            case ARMOR_DURABILITY_ALERT -> ArmorDurabilityAlertRenderer.render(this.minecraft, graphics, this.font, true);
            case ITEM_COOLDOWN_HUD -> ItemCooldownHudRenderer.render(this.minecraft, graphics, this.font, true);
            case SERVER_ADDRESS_HUD -> ServerAddressHudRenderer.render(this.minecraft, graphics, this.font, true);
            case BATTERY_HUD -> BatteryHudRenderer.render(this.minecraft, graphics, this.font, true);
            case MEDIA_HUD -> MediaHudRenderer.render(this.minecraft, graphics, this.font, true);
            case SCOREBOARD -> com.flowclient.mods.render.ScoreboardRenderer.render(this.minecraft, graphics, this.font, true);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
    }

    private int elementWidth() {
        return this.element.getWidth(this.minecraft, this.font, true);
    }

    private int elementHeight() {
        return this.element.getHeight(this.minecraft, this.font, true);
    }

    private int frameWidth() {
        return Math.min(this.width - 40, 280);
    }

    private int frameHeight() {
        return 250;
    }

    private int frameX() {
        return this.centerX(this.frameWidth());
    }

    private int frameY() {
        return this.contentFrameY(24, this.frameWidth());
    }
}
