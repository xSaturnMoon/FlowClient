package com.flowclient.modpanel;

import com.flowclient.mods.FreelookMod;
import com.flowclient.mods.ModifyF3Mod;
import com.flowclient.mods.NametagMod;
import com.flowclient.mods.ZoomifyMod;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public final class ModRegistry {
    private static final List<ModEntry> ENTRIES = new ArrayList<>();

    static {
        registerVisual();
        registerCombat();
        registerHud();
        registerUtility();
        registerWorld();
        registerMisc();
    }

    private ModRegistry() {
    }

    public static List<ModEntry> all() {
        return List.copyOf(ENTRIES);
    }

    public static List<ModEntry> visible(ModCategory category, String searchQuery) {
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        boolean globalSearch = !query.isEmpty();

        return ENTRIES.stream()
                .filter(entry -> globalSearch || entry.category() == category)
                .filter(entry -> !globalSearch || entry.displayName().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator
                        .comparing(ModEntry::category)
                        .thenComparing(ModEntry::priority)
                        .thenComparing(entry -> entry.displayName().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static void register(
            String id,
            String displayName,
            ModCategory category,
            int priority,
            net.minecraft.world.item.Item iconItem,
            net.minecraft.resources.ResourceLocation customIcon,
            BooleanSupplier enabled,
            Runnable toggle,
            Function<Screen, Screen> settingsScreenFactory
    ) {
        ENTRIES.add(new ModEntry(
                id,
                displayName,
                category,
                priority,
                iconItem,
                customIcon,
                enabled,
                toggle,
                settingsScreenFactory
        ));
    }

    private static void registerVisual() {
        register("nametag", "Nametag", ModCategory.VISUAL, 10, Items.NAME_TAG, null,
                NametagMod::isEnabled, NametagMod::toggle, null);
        register("zoomify", "Zoomify", ModCategory.VISUAL, 11, Items.SPYGLASS, null,
                ZoomifyMod::isEnabled, ZoomifyMod::toggle, ZoomifySettingsScreen::new);
        register("crosshair", "Crosshair", ModCategory.VISUAL, 12, Items.SPECTRAL_ARROW, null,
                com.flowclient.mods.render.CrosshairMod::isEnabled, com.flowclient.mods.render.CrosshairMod::toggle,
                CrosshairSettingsScreen::new);
        register("fps_counter", "FPS Counter", ModCategory.VISUAL, 13, Items.REPEATER, null,
                com.flowclient.mods.fps.FpsCounterMod::isEnabled, com.flowclient.mods.fps.FpsCounterMod::toggle,
                FpsCounterSettingsScreen::new);
        register("fullbright", "Fullbright", ModCategory.VISUAL, 14, Items.GLOWSTONE, null,
                com.flowclient.mods.fullbright.FullbrightMod::isEnabled, com.flowclient.mods.fullbright.FullbrightMod::toggle, null);
        register("portal_fx", "Portal FX", ModCategory.VISUAL, 15, Items.END_PORTAL_FRAME, null,
                com.flowclient.mods.immersion.PortalFxMod::isEnabled, com.flowclient.mods.immersion.PortalFxMod::toggle, null);
        register("item_physics", "Item Physics", ModCategory.VISUAL, 16, Items.SLIME_BALL, null,
                com.flowclient.mods.render.ItemPhysicsMod::isEnabled, com.flowclient.mods.render.ItemPhysicsMod::toggle, null);
        register("freelook", "Freelook", ModCategory.VISUAL, 17, Items.ENDER_PEARL, null,
                FreelookMod::isEnabled, FreelookMod::toggle, null);
        register("modify_f3", "Modify F3", ModCategory.VISUAL, 18, Items.REDSTONE_TORCH, null,
                ModifyF3Mod::isEnabled, ModifyF3Mod::toggle, null);
        register("block_animation", "Block Animation", ModCategory.VISUAL, 19, Items.GRASS_BLOCK, null,
                com.flowclient.mods.render.BlockAnimationMod::isEnabled, com.flowclient.mods.render.BlockAnimationMod::toggle, null);
    }

    private static void registerCombat() {
        register("hitboxes", "Hitboxes", ModCategory.COMBAT, 10, Items.BARRIER, null,
                com.flowclient.mods.render.HitboxesMod::isEnabled, com.flowclient.mods.render.HitboxesMod::toggle, null);
        register("damage_indicator", "Damage Indicator", ModCategory.COMBAT, 11, Items.IRON_SWORD, null,
                com.flowclient.mods.render.DamageIndicatorMod::isEnabled, com.flowclient.mods.render.DamageIndicatorMod::toggle,
                DamageIndicatorSettingsScreen::new);
        register("combo_counter", "Combo Counter", ModCategory.COMBAT, 12, Items.DIAMOND_SWORD, null,
                com.flowclient.mods.immersion.ComboCounterMod::isEnabled, com.flowclient.mods.immersion.ComboCounterMod::toggle, null);
        register("trajectory_arc", "Trajectory Arc", ModCategory.COMBAT, 13, Items.BOW, null,
                com.flowclient.mods.immersion.TrajectoryArcMod::isEnabled, com.flowclient.mods.immersion.TrajectoryArcMod::toggle, null);
        register("pearl_landing_marker", "Pearl Landing Marker", ModCategory.COMBAT, 14, Items.ENDER_PEARL, null,
                com.flowclient.mods.pearl.PearlLandingMarkerMod::isEnabled, com.flowclient.mods.pearl.PearlLandingMarkerMod::toggle, null);
        register("elytra_flight_path", "Elytra Flight Path", ModCategory.COMBAT, 15, Items.ELYTRA, null,
                com.flowclient.mods.elytra.ElytraFlightPathMod::isEnabled, com.flowclient.mods.elytra.ElytraFlightPathMod::toggle, null);
        register("reach_ring", "Reach Ring", ModCategory.COMBAT, 16, Items.IRON_SWORD, null,
                com.flowclient.mods.reach.ReachRingMod::isEnabled, com.flowclient.mods.reach.ReachRingMod::toggle, null);
        register("crystal_bed_preview", "Crystal/Bed Preview", ModCategory.COMBAT, 17, Items.END_CRYSTAL, null,
                com.flowclient.mods.explosion.CrystalBedPreviewMod::isEnabled, com.flowclient.mods.explosion.CrystalBedPreviewMod::toggle, null);
        register("keystrokes", "Keystrokes", ModCategory.COMBAT, 18, Items.OAK_SIGN, null,
                com.flowclient.mods.keystrokes.KeystrokesMod::isEnabled, com.flowclient.mods.keystrokes.KeystrokesMod::toggle,
                KeystrokesSettingsScreen::new);
    }

    private static void registerHud() {
        register("armor_hud", "Armor HUD", ModCategory.HUD, 10, Items.IRON_CHESTPLATE, null,
                com.flowclient.mods.armor.ArmorHudMod::isEnabled, com.flowclient.mods.armor.ArmorHudMod::toggle,
                ArmorHudSettingsScreen::new);
        register("health_bar", "Health Bar", ModCategory.HUD, 11, Items.GOLDEN_APPLE, null,
                com.flowclient.mods.health.HealthBarMod::isEnabled, com.flowclient.mods.health.HealthBarMod::toggle,
                HealthBarSettingsScreen::new);
        register("media_hud", "Media HUD", ModCategory.HUD, 12, Items.MUSIC_DISC_CAT, null,
                com.flowclient.mods.media.MediaHudMod::isEnabled, com.flowclient.mods.media.MediaHudMod::toggle,
                MediaHudSettingsScreen::new);
        register("flow_clock", "Flow Clock", ModCategory.HUD, 13, Items.CLOCK, ModIcons.FLOW_CLOCK,
                com.flowclient.mods.clock.FlowClockMod::isEnabled, com.flowclient.mods.clock.FlowClockMod::toggle,
                FlowClockSettingsScreen::new);
        register("scoreboard", "Scoreboard", ModCategory.HUD, 14, Items.ITEM_FRAME, null,
                com.flowclient.mods.render.ScoreboardMod::isEnabled, com.flowclient.mods.render.ScoreboardMod::toggle,
                ScoreboardSettingsScreen::new);
        register("flow_tab", "Flow Tab", ModCategory.HUD, 15, Items.PLAYER_HEAD, ModIcons.FLOW_TAB,
                com.flowclient.mods.tab.FlowTabMod::isEnabled, com.flowclient.mods.tab.FlowTabMod::toggle,
                FlowTabSettingsScreen::new);
        register("block_speed", "Block Speed", ModCategory.HUD, 16, Items.SUGAR, null,
                com.flowclient.mods.blockspeed.BlockSpeedMod::isEnabled, com.flowclient.mods.blockspeed.BlockSpeedMod::toggle,
                BlockSpeedSettingsScreen::new);
        register("block_break_progress", "Block Break Progress", ModCategory.HUD, 17, Items.IRON_PICKAXE, null,
                com.flowclient.mods.blockbreak.BlockBreakProgressMod::isEnabled, com.flowclient.mods.blockbreak.BlockBreakProgressMod::toggle,
                BlockBreakProgressSettingsScreen::new);
        register("armor_durability_alert", "Armor Durability Alert", ModCategory.HUD, 18, Items.ANVIL, null,
                com.flowclient.mods.armordurability.ArmorDurabilityAlertMod::isEnabled, com.flowclient.mods.armordurability.ArmorDurabilityAlertMod::toggle,
                ArmorDurabilityAlertSettingsScreen::new);
        register("item_cooldown_hud", "Item Cooldown HUD", ModCategory.HUD, 19, Items.CLOCK, null,
                com.flowclient.mods.cooldown.ItemCooldownHudMod::isEnabled, com.flowclient.mods.cooldown.ItemCooldownHudMod::toggle,
                ItemCooldownHudSettingsScreen::new);
        register("server_address_hud", "Server Address HUD", ModCategory.HUD, 20, Items.PAPER, null,
                com.flowclient.mods.serveraddress.ServerAddressHudMod::isEnabled, com.flowclient.mods.serveraddress.ServerAddressHudMod::toggle,
                ServerAddressHudSettingsScreen::new);
        register("battery_hud", "Battery HUD", ModCategory.HUD, 21, Items.REDSTONE, null,
                com.flowclient.mods.battery.BatteryHudMod::isEnabled, com.flowclient.mods.battery.BatteryHudMod::toggle,
                BatteryHudSettingsScreen::new);
        register("coordinates_hud", "Coordinates HUD", ModCategory.HUD, 22, Items.COMPASS, null,
                com.flowclient.mods.coordinates.CoordinatesHudMod::isEnabled, com.flowclient.mods.coordinates.CoordinatesHudMod::toggle,
                CoordinatesHudSettingsScreen::new);
        register("ping_hud", "Ping HUD", ModCategory.HUD, 23, Items.REPEATER, null,
                com.flowclient.mods.ping.PingHudMod::isEnabled, com.flowclient.mods.ping.PingHudMod::toggle,
                PingHudSettingsScreen::new);
    }

    private static void registerUtility() {
        register("jei", "JEI", ModCategory.UTILITY, 0, Items.BOOK, ModIcons.JEI,
                com.flowclient.mods.jei.JeiMod::isEnabled, com.flowclient.mods.jei.JeiMod::toggle, null);
        register("voice_chat", "Voice Chat", ModCategory.UTILITY, 1, Items.GOAT_HORN, ModIcons.VOICE_CHAT,
                com.flowclient.mods.voicechat.VoiceChatMod::isEnabled, com.flowclient.mods.voicechat.VoiceChatMod::toggle, null);
        register("other_client", "OtherClient", ModCategory.UTILITY, 2, Items.COMPASS, ModIcons.OTHER_CLIENT,
                com.flowclient.mods.otherclient.OtherClientMod::isEnabled, com.flowclient.mods.otherclient.OtherClientMod::toggle, null);
        register("inventory_sort", "Inventory Sort", ModCategory.UTILITY, 10, Items.HOPPER, null,
                com.flowclient.mods.inventory.InventorySortMod::isEnabled, com.flowclient.mods.inventory.InventorySortMod::toggle,
                InventorySortSettingsScreen::new);
        register("shulker_tooltip", "Shulker Tooltip", ModCategory.UTILITY, 11, Items.SHULKER_BOX, null,
                com.flowclient.mods.tooltip.ShulkerTooltipMod::isEnabled, com.flowclient.mods.tooltip.ShulkerTooltipMod::toggle, null);
        register("all_schematics", "AllSchematics", ModCategory.UTILITY, 12, Items.FILLED_MAP, null,
                com.flowclient.mods.schematics.AllSchematicsMod::isEnabled, com.flowclient.mods.schematics.AllSchematicsMod::toggle, null);
        register("block_overlay", "Block Overlay", ModCategory.UTILITY, 13, Items.STRUCTURE_BLOCK, null,
                com.flowclient.mods.render.BlockOverlayMod::isEnabled, com.flowclient.mods.render.BlockOverlayMod::toggle,
                BlockOverlaySettingsScreen::new);
    }

    private static void registerWorld() {
        register("chunk_borders", "Chunk Borders", ModCategory.WORLD, 10, Items.COMPASS, null,
                com.flowclient.mods.render.ChunkBordersMod::isEnabled, com.flowclient.mods.render.ChunkBordersMod::toggle, null);
        register("time_changer", "Time Changer", ModCategory.WORLD, 11, Items.CLOCK, null,
                com.flowclient.mods.environment.TimeChangerMod::isEnabled, com.flowclient.mods.environment.TimeChangerMod::toggle,
                TimeChangerSettingsScreen::new);
        register("weather_changer", "Weather Changer", ModCategory.WORLD, 12, Items.WATER_BUCKET, null,
                com.flowclient.mods.environment.WeatherChangerMod::isEnabled, com.flowclient.mods.environment.WeatherChangerMod::toggle,
                WeatherChangerSettingsScreen::new);
    }

    private static void registerMisc() {
        register("chat_tweaks", "Chat Tweaks", ModCategory.MISC, 10, Items.WRITABLE_BOOK, null,
                com.flowclient.mods.chat.ChatTweaksMod::isEnabled, com.flowclient.mods.chat.ChatTweaksMod::toggle,
                ChatTweaksSettingsScreen::new);
        register("flow_quiet", "Flow Quiet", ModCategory.MISC, 11, Items.BELL, null,
                com.flowclient.mods.quiet.FlowQuietMod::isEnabled, com.flowclient.mods.quiet.FlowQuietMod::toggle,
                FlowQuietSettingsScreen::new);
        register("advancements_unlock", "Advancements Unlock", ModCategory.MISC, 12, Items.KNOWLEDGE_BOOK, null,
                com.flowclient.mods.advancements.AdvancementsUnlockMod::isEnabled,
                com.flowclient.mods.advancements.AdvancementsUnlockMod::toggle, null);
    }
}
