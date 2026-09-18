package com.flowclient.modpanel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public final class ToggleSwitchWidget extends AbstractWidget {
    public static final int TRACK_WIDTH = 28;
    public static final int TRACK_HEIGHT = 14;

    private static final int KNOB_SIZE = 10;
    private static final int KNOB_INSET = 2;

    private final BooleanSupplier enabled;
    private final Runnable onToggle;

    public ToggleSwitchWidget(int x, int y, BooleanSupplier enabled, Runnable onToggle) {
        super(x, y, TRACK_WIDTH, TRACK_HEIGHT, Component.empty());
        this.enabled = enabled;
        this.onToggle = onToggle;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        boolean on = this.enabled.getAsBoolean();

        ModPanelTheme.fillBordered(
                graphics,
                x,
                y,
                this.width,
                this.height,
                on ? ModPanelTheme.ACCENT_GREEN : ModPanelTheme.TRACK_OFF
        );

        int knobY = y + KNOB_INSET;
        int knobX = on
                ? x + this.width - KNOB_INSET - KNOB_SIZE
                : x + KNOB_INSET;
        graphics.fill(knobX, knobY, knobX + KNOB_SIZE, knobY + KNOB_SIZE, ModPanelTheme.KNOB);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        playButtonClickSound(Minecraft.getInstance().getSoundManager());
        this.onToggle.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        narration.add(
                net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                Component.literal(this.enabled.getAsBoolean() ? "On" : "Off")
        );
    }
}
