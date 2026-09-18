package com.flowclient.modpanel;

import com.flowclient.mods.zoom.OverlayVisibility;
import com.flowclient.mods.zoom.SoundBehavior;
import com.flowclient.mods.zoom.SpyglassZoomBehavior;
import com.flowclient.mods.zoom.ZoomTransition;
import com.flowclient.mods.zoom.ZoomifySettings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ZoomifySettingsScreen extends ModPanelScreen {
  private static final Component TITLE = Component.literal("Zoomify");
  private static final int ROW_HEIGHT = 22;
  private static final int RESET_WIDTH = 20;

  private final List<SettingRow> rows = new ArrayList<>();
  private int scrollOffset;

  public ZoomifySettingsScreen(Screen parent) {
    super(TITLE, parent);
  }

  @Override
  protected void init() {
    if (this.rows.isEmpty()) {
      this.buildRows();
    }
    this.rebuildWidgets();
  }

  @Override
  protected void initPanel() {
    int frameX = this.frameX();
    int frameY = this.frameY();
    int frameWidth = this.frameWidth();
    int visibleHeight = this.frameHeight();
    int maxScroll = Math.max(0, this.rows.size() * (ROW_HEIGHT + 2) + 16 - visibleHeight);
    this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));

    for (int i = 0; i < this.rows.size(); i++) {
      SettingRow row = this.rows.get(i);
      int y = frameY + 8 + i * (ROW_HEIGHT + 2) - this.scrollOffset;
      if (row.header() || y + ROW_HEIGHT < frameY || y > frameY + visibleHeight) {
        continue;
      }

      int rowWidth = frameWidth - 16 - RESET_WIDTH - 4;
      this.addRenderableWidget(Button.builder(row.label(), button -> {
        row.onClick().accept(ZoomifySettings.get());
        ZoomifySettings.get().persist();
        this.rebuildWidgets();
      }).bounds(frameX + 8, y, rowWidth, ROW_HEIGHT).build());

      this.addRenderableWidget(Button.builder(Component.literal("↺"), button -> {
        row.reset(ZoomifySettings.get());
        ZoomifySettings.get().persist();
        this.rebuildWidgets();
      }).bounds(frameX + frameWidth - RESET_WIDTH - 8, y, RESET_WIDTH, ROW_HEIGHT).build());
    }

    this.addBackButton();
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (this.isHoveringFrame(mouseX, mouseY)) {
      this.scrollOffset = (int) Math.max(0, this.scrollOffset - scrollY * 16);
      this.rebuildWidgets();
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    this.drawContentFrame(graphics, this.frameX(), this.frameY(), this.frameWidth(), this.frameHeight());

    int frameY = this.frameY();
    int visibleHeight = this.frameHeight();
    for (int i = 0; i < this.rows.size(); i++) {
      SettingRow row = this.rows.get(i);
      if (!row.header()) {
        continue;
      }

      int y = frameY + 8 + i * (ROW_HEIGHT + 2) - this.scrollOffset;
      if (y + ROW_HEIGHT < frameY || y > frameY + visibleHeight) {
        continue;
      }

      graphics.drawString(this.font, row.title(), this.frameX() + 8, y + 6, 0xFFCCCCCC);
    }

    super.render(graphics, mouseX, mouseY, partialTick);
  }

  private void buildRows() {
    this.rows.add(SettingRow.header("Main Zoom Settings"));
    this.rows.add(SettingRow.ofDouble(
        "Initial Zoom",
        "initialZoom",
        settings -> settings.initialZoom() + "x",
        settings -> settings.setInitialZoom(nextZoom(settings.initialZoom()))));
    this.rows.add(SettingRow.ofDouble(
        "Zoom In Time",
        "zoomInTime",
        settings -> String.format("%.1f secs", settings.zoomInTime()),
        settings -> settings.setZoomInTime(nextTime(settings.zoomInTime()))));
    this.rows.add(SettingRow.ofDouble(
        "Zoom Out Time",
        "zoomOutTime",
        settings -> String.format("%.1f secs", settings.zoomOutTime()),
        settings -> settings.setZoomOutTime(nextTime(settings.zoomOutTime()))));
    this.rows.add(SettingRow.ofEnum(
        "Zoom In Transition",
        "zoomInTransition",
        settings -> settings.zoomInTransition().label(),
        settings -> settings.setZoomInTransition(settings.zoomInTransition().next())));
    this.rows.add(SettingRow.ofEnum(
        "Zoom Out Transition",
        "zoomOutTransition",
        settings -> settings.zoomOutTransition().label(),
        settings -> settings.setZoomOutTransition(settings.zoomOutTransition().next())));
    this.rows.add(SettingRow.ofBoolean(
        "Affect Hand FOV",
        "affectHandFov",
        settings -> boolLabel(settings.affectHandFov()),
        settings -> settings.setAffectHandFov(!settings.affectHandFov())));

    this.rows.add(SettingRow.header("Scrolling"));
    this.rows.add(SettingRow.ofBoolean(
        "Enable Scroll Zoom",
        "enableScrollZoom",
        settings -> boolLabel(settings.enableScrollZoom()),
        settings -> settings.setEnableScrollZoom(!settings.enableScrollZoom())));
    this.rows.add(SettingRow.ofInt(
        "Scroll Step Count",
        "scrollStepCount",
        settings -> Integer.toString(settings.scrollStepCount()),
        settings -> settings.setScrollStepCount(settings.scrollStepCount() >= 50 ? 1 : settings.scrollStepCount() + 1)));
    this.rows.add(SettingRow.ofDouble(
        "Zoom Per Step",
        "zoomPerStep",
        settings -> settings.zoomPerStep() + "x",
        settings -> settings.setZoomPerStep(nextStepZoom(settings.zoomPerStep()))));
    this.rows.add(SettingRow.ofPercent(
        "Scroll Zoom Smoothness",
        "scrollZoomSmoothness",
        settings -> Math.round(settings.scrollZoomSmoothness() * 100.0) + "%",
        settings -> settings.setScrollZoomSmoothness(nextPercent(settings.scrollZoomSmoothness()))));
    this.rows.add(SettingRow.ofBoolean(
        "Remember Zoom Steps",
        "rememberZoomSteps",
        settings -> boolLabel(settings.rememberZoomSteps()),
        settings -> settings.setRememberZoomSteps(!settings.rememberZoomSteps())));

    this.rows.add(SettingRow.header("Spyglass"));
    this.rows.add(SettingRow.ofEnum(
        "Zoom Behavior",
        "spyglassZoomBehavior",
        settings -> settings.spyglassZoomBehavior().label(),
        settings -> settings.setSpyglassZoomBehavior(settings.spyglassZoomBehavior().next())));
    this.rows.add(SettingRow.ofEnum(
        "Overlay Visibility",
        "overlayVisibility",
        settings -> settings.overlayVisibility().label(),
        settings -> settings.setOverlayVisibility(settings.overlayVisibility().next())));
    this.rows.add(SettingRow.ofEnum(
        "Sound Behavior",
        "soundBehavior",
        settings -> settings.soundBehavior().label(),
        settings -> settings.setSoundBehavior(settings.soundBehavior().next())));
  }

  private static String boolLabel(boolean value) {
    return value ? "Enabled" : "Disabled";
  }

  private static double nextZoom(double current) {
    double[] values = {2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0};
    return cycle(values, current);
  }

  private static double nextTime(double current) {
    double[] values = {0.0, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0};
    return cycle(values, current);
  }

  private static double nextStepZoom(double current) {
    double[] values = {1.1, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0};
    return cycle(values, current);
  }

  private static double nextPercent(double current) {
    double[] values = {0.0, 0.1, 0.25, 0.5, 0.7, 0.85, 1.0};
    return cycle(values, current);
  }

  private static double cycle(double[] values, double current) {
    for (int i = 0; i < values.length; i++) {
      if (Math.abs(values[i] - current) < 0.001) {
        return values[(i + 1) % values.length];
      }
    }
    return values[0];
  }

  private boolean isHoveringFrame(double mouseX, double mouseY) {
    return mouseX >= this.frameX()
        && mouseX <= this.frameX() + this.frameWidth()
        && mouseY >= this.frameY()
        && mouseY <= this.frameY() + this.frameHeight();
  }

  private int frameWidth() {
    return Math.min(this.width - 40, 420);
  }

  private int frameHeight() {
    return Math.min(this.height - 60, this.height - 80);
  }

  private int frameX() {
    return this.centerX(this.frameWidth());
  }

  private int frameY() {
    return 24;
  }

  private record SettingRow(
      String title,
      String fieldId,
      Function<ZoomifySettings, String> valueText,
      Consumer<ZoomifySettings> onClick,
      boolean header) {
    static SettingRow header(String title) {
      return new SettingRow(title, null, null, null, true);
    }

    static SettingRow ofDouble(
        String title,
        String fieldId,
        Function<ZoomifySettings, String> value,
        Consumer<ZoomifySettings> onClick) {
      return new SettingRow(title, fieldId, value, onClick, false);
    }

    static SettingRow ofInt(
        String title,
        String fieldId,
        Function<ZoomifySettings, String> value,
        Consumer<ZoomifySettings> onClick) {
      return ofDouble(title, fieldId, value, onClick);
    }

    static SettingRow ofPercent(
        String title,
        String fieldId,
        Function<ZoomifySettings, String> value,
        Consumer<ZoomifySettings> onClick) {
      return ofDouble(title, fieldId, value, onClick);
    }

    static SettingRow ofBoolean(
        String title,
        String fieldId,
        Function<ZoomifySettings, String> value,
        Consumer<ZoomifySettings> onClick) {
      return new SettingRow(title, fieldId, value, onClick, false);
    }

    static SettingRow ofEnum(
        String title,
        String fieldId,
        Function<ZoomifySettings, String> value,
        Consumer<ZoomifySettings> onClick) {
      return new SettingRow(title, fieldId, value, onClick, false);
    }

    Component label() {
      return Component.literal(this.title + ": " + this.valueText.apply(ZoomifySettings.get()));
    }

    void reset(ZoomifySettings settings) {
      if (this.fieldId != null) {
        settings.resetField(this.fieldId);
      }
    }
  }
}
