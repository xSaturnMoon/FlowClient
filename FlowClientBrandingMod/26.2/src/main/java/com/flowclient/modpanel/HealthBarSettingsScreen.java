package com.flowclient.modpanel;

import com.flowclient.mods.health.HealthBarRenderer;
import com.flowclient.mods.health.HealthBarSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HealthBarSettingsScreen extends ModSettingsPanelScreen<HealthBarSettings> {
    private static final Component TITLE = Component.literal("Health Bar");

    public HealthBarSettingsScreen(Screen parent) {
        super(TITLE, parent, "Health Bar");
    }

    @Override
    protected HealthBarSettings settings() {
        return HealthBarSettings.get();
    }

    @Override
    protected void persistSettings() {
        HealthBarSettings.get().persist();
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Bar");
        this.addRow("Style", s -> s.style().label(), HealthBarSettings::cycleStyle, HealthBarSettings::resetStyle);
        this.addRow("Target", s -> s.target().label(), HealthBarSettings::cycleTarget, HealthBarSettings::resetTarget);
        this.addRow("Bar width", s -> String.valueOf(s.barWidth()), s -> s.cycleBarWidth(), HealthBarSettings::resetBarWidth);
        this.addRow("Bar height", s -> String.valueOf(s.barHeight()), s -> s.cycleBarHeight(), HealthBarSettings::resetBarHeight);
        this.addRow("Height offset", s -> String.format("%.2f", s.yOffset()), s -> s.cycleYOffset(), HealthBarSettings::resetYOffset);
        this.addRow("Max distance", s -> s.maxDistance() + "m", s -> s.cycleMaxDistance(), HealthBarSettings::resetMaxDistance);

        this.addHeader("Visibility");
        this.addRow("Hide full HP", s -> s.hideFullHealth() ? "ON" : "OFF", HealthBarSettings::toggleHideFullHealth, HealthBarSettings::resetHideFullHealth);
        this.addRow("Show numbers", s -> s.showNumeric() ? "ON" : "OFF", HealthBarSettings::toggleShowNumeric, HealthBarSettings::resetShowNumeric);
        this.addRow("Show absorption", s -> s.showAbsorption() ? "ON" : "OFF", HealthBarSettings::toggleShowAbsorption, HealthBarSettings::resetShowAbsorption);
        this.addRow("Show on self", s -> s.showOnSelf() ? "ON" : "OFF", HealthBarSettings::toggleShowOnSelf, HealthBarSettings::resetShowOnSelf);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        HealthBarRenderer.renderPreview(
                graphics,
                this.font,
                this.settingsFrameX() + this.settingsFrameWidth() / 2,
                this.settingsFrameY() + this.settingsFrameHeight() - 28
        );
    }
}
