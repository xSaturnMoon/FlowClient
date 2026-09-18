package com.flowclient.mods.hud;

import com.flowclient.mods.armor.ArmorHudMod;
import com.flowclient.mods.armordurability.ArmorDurabilityAlertMod;
import com.flowclient.mods.blockbreak.BlockBreakProgressMod;
import com.flowclient.mods.blockbreak.BlockBreakProgressRenderer;
import com.flowclient.mods.blockspeed.BlockSpeedMod;
import com.flowclient.mods.blockspeed.BlockSpeedRenderer;
import com.flowclient.mods.cooldown.ItemCooldownHudMod;
import com.flowclient.mods.cooldown.ItemCooldownHudRenderer;
import com.flowclient.mods.fps.FpsCounterMod;
import com.flowclient.mods.fps.FpsCounterRenderer;
import com.flowclient.mods.clock.FlowClockMod;
import com.flowclient.mods.clock.FlowClockRenderer;
import com.flowclient.mods.media.MediaHudMod;
import com.flowclient.mods.media.MediaHudRenderer;
import com.flowclient.mods.keystrokes.KeystrokesMod;
import com.flowclient.mods.armordurability.ArmorDurabilityAlertRenderer;
import com.flowclient.mods.battery.BatteryHudMod;
import com.flowclient.mods.battery.BatteryHudRenderer;
import com.flowclient.mods.serveraddress.ServerAddressHudMod;
import com.flowclient.mods.serveraddress.ServerAddressHudRenderer;
import com.flowclient.mods.coordinates.CoordinatesHudMod;
import com.flowclient.mods.coordinates.CoordinatesHudRenderer;
import com.flowclient.mods.ping.PingHudMod;
import com.flowclient.mods.ping.PingHudRenderer;
import com.flowclient.mods.render.ScoreboardRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;

public enum HudElement {
    ARMOR_HUD("armor_hud", "Armor HUD") {
        @Override
        public boolean isEnabled() {
            return ArmorHudMod.isEnabled();
        }
    },
    KEYSTROKES("keystrokes", "Keystrokes") {
        @Override
        public boolean isEnabled() {
            return KeystrokesMod.isEnabled();
        }
    },
    FPS_COUNTER("fps_counter", "FPS Counter") {
        @Override
        public boolean isEnabled() {
            return FpsCounterMod.isEnabled();
        }
    },
    FLOW_CLOCK("flow_clock", "Flow Clock") {
        @Override
        public boolean isEnabled() {
            return FlowClockMod.isEnabled();
        }
    },
    BLOCK_SPEED("block_speed", "Block Speed") {
        @Override
        public boolean isEnabled() {
            return BlockSpeedMod.isEnabled();
        }
    },
    MEDIA_HUD("media_hud", "Media HUD") {
        @Override
        public boolean isEnabled() {
            return MediaHudMod.isEnabled();
        }
    },
    SCOREBOARD("scoreboard", "Scoreboard") {
        @Override
        public boolean isEnabled() {
            return com.flowclient.mods.render.ScoreboardMod.isEnabled();
        }
    },
    BLOCK_BREAK_PROGRESS("block_break_progress", "Block Break Progress") {
        @Override
        public boolean isEnabled() {
            return BlockBreakProgressMod.isEnabled();
        }
    },
    ARMOR_DURABILITY_ALERT("armor_durability_alert", "Armor Durability Alert") {
        @Override
        public boolean isEnabled() {
            return ArmorDurabilityAlertMod.isEnabled();
        }
    },
    ITEM_COOLDOWN_HUD("item_cooldown_hud", "Item Cooldown HUD") {
        @Override
        public boolean isEnabled() {
            return ItemCooldownHudMod.isEnabled();
        }
    },
    SERVER_ADDRESS_HUD("server_address_hud", "Server Address HUD") {
        @Override
        public boolean isEnabled() {
            return ServerAddressHudMod.isEnabled();
        }
    },
    BATTERY_HUD("battery_hud", "Battery HUD") {
        @Override
        public boolean isEnabled() {
            return BatteryHudMod.isEnabled();
        }
    },
    COORDINATES_HUD("coordinates_hud", "Coordinates HUD") {
        @Override
        public boolean isEnabled() {
            return CoordinatesHudMod.isEnabled();
        }
    },
    PING_HUD("ping_hud", "Ping HUD") {
        @Override
        public boolean isEnabled() {
            return PingHudMod.isEnabled();
        }
    };

    private final String id;
    private final String displayName;

    HudElement(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public abstract boolean isEnabled();

    public static HudElement byId(String id) {
        for (HudElement element : values()) {
            if (element.id.equals(id)) {
                return element;
            }
        }
        return null;
    }

    public static List<HudElement> enabledElements() {
        List<HudElement> enabled = new ArrayList<>();
        for (HudElement element : values()) {
            if (element.isEnabled()) {
                enabled.add(element);
            }
        }
        return enabled;
    }

    public void renderPreview(Minecraft mc, GuiGraphicsExtractor graphics, Font font) {
        switch (this) {
            case ARMOR_HUD -> com.flowclient.mods.armor.ArmorHudRenderer.render(mc, graphics, font, true);
            case KEYSTROKES -> com.flowclient.mods.keystrokes.KeystrokesRenderer.render(mc, graphics, font, true);
            case FPS_COUNTER -> FpsCounterRenderer.render(mc, graphics, font, true);
            case FLOW_CLOCK -> FlowClockRenderer.render(mc, graphics, font, true);
            case BLOCK_SPEED -> BlockSpeedRenderer.render(mc, graphics, font, true);
            case MEDIA_HUD -> MediaHudRenderer.render(mc, graphics, font, true);
            case SCOREBOARD -> ScoreboardRenderer.render(mc, graphics, font, true);
            case BLOCK_BREAK_PROGRESS -> BlockBreakProgressRenderer.render(mc, graphics, font, true);
            case ARMOR_DURABILITY_ALERT -> ArmorDurabilityAlertRenderer.render(mc, graphics, font, true);
            case ITEM_COOLDOWN_HUD -> ItemCooldownHudRenderer.render(mc, graphics, font, true);
            case SERVER_ADDRESS_HUD -> ServerAddressHudRenderer.render(mc, graphics, font, true);
            case BATTERY_HUD -> BatteryHudRenderer.render(mc, graphics, font, true);
            case COORDINATES_HUD -> CoordinatesHudRenderer.render(mc, graphics, font, true);
            case PING_HUD -> PingHudRenderer.render(mc, graphics, font, true);
        }
    }

    public int getDefaultX(Minecraft mc, int width, int height) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        return switch (this) {
            case ARMOR_HUD -> screenW - width - 6;
            case KEYSTROKES -> screenW - width - 10;
            case FPS_COUNTER -> screenW - width - 4;
            case FLOW_CLOCK -> 4;
            case BLOCK_SPEED -> 4;
            case MEDIA_HUD -> 4;
            case SCOREBOARD -> {
                com.flowclient.mods.render.ScoreboardSettings settings = com.flowclient.mods.render.ScoreboardSettings.get();
                yield switch (settings.anchor()) {
                    case RIGHT -> screenW - width - settings.sideMargin();
                    case LEFT -> settings.sideMargin();
                    case CUSTOM -> screenW - width - settings.sideMargin();
                };
            }
            case BLOCK_BREAK_PROGRESS -> (screenW - width) / 2;
            case ARMOR_DURABILITY_ALERT -> (screenW - width) / 2;
            case ITEM_COOLDOWN_HUD -> (screenW - width) / 2;
            case SERVER_ADDRESS_HUD -> (screenW - width) / 2;
            case BATTERY_HUD -> screenW - width - 4;
            case COORDINATES_HUD -> 4;
            case PING_HUD -> screenW - width - 4;
        };
    }

    public int getDefaultY(Minecraft mc, int width, int height) {
        int screenH = mc.getWindow().getGuiScaledHeight();
        return switch (this) {
            case ARMOR_HUD -> screenH - 34 - height;
            case KEYSTROKES -> screenH - height - 10;
            case FPS_COUNTER -> 4;
            case FLOW_CLOCK -> 4;
            case BLOCK_SPEED -> 22;
            case MEDIA_HUD -> screenH - height - 48;
            case SCOREBOARD -> screenH / 2 + height / 3 - height;
            case BLOCK_BREAK_PROGRESS -> screenH / 2 + 24;
            case ARMOR_DURABILITY_ALERT -> 28;
            case ITEM_COOLDOWN_HUD -> screenH / 2 + 8;
            case SERVER_ADDRESS_HUD -> 4;
            case BATTERY_HUD -> 18;
            case COORDINATES_HUD -> 40;
            case PING_HUD -> 4;
        };
    }

    public int getWidth(Minecraft mc, Font font, boolean preview) {
        return switch (this) {
            case ARMOR_HUD -> com.flowclient.mods.armor.ArmorHudRenderer.getWidth();
            case KEYSTROKES -> com.flowclient.mods.keystrokes.KeystrokesRenderer.getWidth();
            case FPS_COUNTER -> FpsCounterRenderer.getWidth(font);
            case FLOW_CLOCK -> FlowClockRenderer.getWidth(font);
            case BLOCK_SPEED -> BlockSpeedRenderer.getWidth(font);
            case MEDIA_HUD -> MediaHudRenderer.getWidth(font);
            case SCOREBOARD -> com.flowclient.mods.render.ScoreboardRenderer.getWidth(font, preview);
            case BLOCK_BREAK_PROGRESS -> BlockBreakProgressRenderer.getWidth(font);
            case ARMOR_DURABILITY_ALERT -> ArmorDurabilityAlertRenderer.getWidth(font);
            case ITEM_COOLDOWN_HUD -> ItemCooldownHudRenderer.getWidth(font);
            case SERVER_ADDRESS_HUD -> ServerAddressHudRenderer.getWidth(font);
            case BATTERY_HUD -> BatteryHudRenderer.getWidth(font);
            case COORDINATES_HUD -> CoordinatesHudRenderer.getWidth(font);
            case PING_HUD -> PingHudRenderer.getWidth(font);
        };
    }

    public int getHeight(Minecraft mc, Font font, boolean preview) {
        return switch (this) {
            case ARMOR_HUD -> com.flowclient.mods.armor.ArmorHudRenderer.getHeight();
            case KEYSTROKES -> com.flowclient.mods.keystrokes.KeystrokesRenderer.getHeight();
            case FPS_COUNTER -> FpsCounterRenderer.getHeight(font);
            case FLOW_CLOCK -> FlowClockRenderer.getHeight(font);
            case BLOCK_SPEED -> BlockSpeedRenderer.getHeight(font);
            case MEDIA_HUD -> MediaHudRenderer.getHeight(font);
            case SCOREBOARD -> com.flowclient.mods.render.ScoreboardRenderer.getHeight(font, preview);
            case BLOCK_BREAK_PROGRESS -> BlockBreakProgressRenderer.getHeight(font);
            case ARMOR_DURABILITY_ALERT -> ArmorDurabilityAlertRenderer.getHeight(font);
            case ITEM_COOLDOWN_HUD -> ItemCooldownHudRenderer.getHeight(font);
            case SERVER_ADDRESS_HUD -> ServerAddressHudRenderer.getHeight(font);
            case BATTERY_HUD -> BatteryHudRenderer.getHeight(font);
            case COORDINATES_HUD -> CoordinatesHudRenderer.getHeight(font);
            case PING_HUD -> PingHudRenderer.getHeight(font);
        };
    }
}
