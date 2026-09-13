package com.flowclient.mods;

import com.flowclient.FlowClientMod;
import com.flowclient.mods.immersion.ComboCounterMod;
import com.flowclient.mods.immersion.PortalFxMod;
import com.flowclient.mods.immersion.TrajectoryArcMod;
import com.flowclient.mods.jei.JeiMod;
import com.flowclient.mods.advancements.AdvancementsUnlockMod;
import com.flowclient.mods.armor.ArmorHudMod;
import com.flowclient.mods.armor.ArmorHudSettings;
import com.flowclient.mods.environment.TimeChangerMod;
import com.flowclient.mods.environment.TimeChangerSettings;
import com.flowclient.mods.environment.WeatherChangerMod;
import com.flowclient.mods.environment.WeatherChangerSettings;
import com.flowclient.mods.fps.FpsCounterMod;
import com.flowclient.mods.fps.FpsCounterSettings;
import com.flowclient.mods.clock.FlowClockMod;
import com.flowclient.mods.clock.FlowClockSettings;
import com.flowclient.mods.media.MediaHudMod;
import com.flowclient.mods.media.MediaHudSettings;
import com.flowclient.mods.fullbright.FullbrightController;
import com.flowclient.mods.fullbright.FullbrightMod;
import com.flowclient.mods.quiet.FlowQuietMod;
import com.flowclient.mods.quiet.FlowQuietSettings;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.keystrokes.KeystrokesMod;
import com.flowclient.mods.keystrokes.KeystrokesSettings;
import com.flowclient.mods.render.BlockOverlayMod;
import com.flowclient.mods.render.BlockOverlaySettings;
import com.flowclient.mods.render.CrosshairMod;
import com.flowclient.mods.render.CrosshairSettings;
import com.flowclient.mods.render.ItemPhysicsMod;
import com.flowclient.mods.render.MotionBlurMod;
import com.flowclient.mods.render.ScoreboardMod;
import com.flowclient.mods.render.ScoreboardSettings;
import com.flowclient.mods.tab.FlowTabMod;
import com.flowclient.mods.tab.FlowTabSettings;
import com.flowclient.mods.chat.ChatTweaksMod;
import com.flowclient.mods.chat.ChatTweaksSettings;
import com.flowclient.mods.blockspeed.BlockSpeedMod;
import com.flowclient.mods.blockspeed.BlockSpeedSettings;
import com.flowclient.mods.blockbreak.BlockBreakProgressMod;
import com.flowclient.mods.blockbreak.BlockBreakProgressSettings;
import com.flowclient.mods.armordurability.ArmorDurabilityAlertMod;
import com.flowclient.mods.armordurability.ArmorDurabilityAlertSettings;
import com.flowclient.mods.cooldown.ItemCooldownHudMod;
import com.flowclient.mods.cooldown.ItemCooldownHudSettings;
import com.flowclient.mods.battery.BatteryHudMod;
import com.flowclient.mods.battery.BatteryHudSettings;
import com.flowclient.mods.serveraddress.ServerAddressHudMod;
import com.flowclient.mods.serveraddress.ServerAddressHudSettings;
import com.flowclient.mods.coordinates.CoordinatesHudMod;
import com.flowclient.mods.coordinates.CoordinatesHudSettings;
import com.flowclient.mods.ping.PingHudMod;
import com.flowclient.mods.ping.PingHudSettings;
import com.flowclient.mods.pearl.PearlLandingMarkerMod;
import com.flowclient.mods.elytra.ElytraFlightPathMod;
import com.flowclient.mods.reach.ReachRingMod;
import com.flowclient.mods.explosion.CrystalBedPreviewMod;
import com.flowclient.mods.health.HealthBarMod;
import com.flowclient.mods.health.HealthBarSettings;
import com.flowclient.mods.inventory.InventorySortMod;
import com.flowclient.mods.inventory.InventorySortSettings;
import com.flowclient.mods.render.ChunkBordersMod;
import com.flowclient.mods.render.DamageIndicatorMod;
import com.flowclient.mods.render.DamageIndicatorSettings;
import com.flowclient.mods.render.HitboxesMod;
import com.flowclient.mods.tooltip.ShulkerTooltipMod;
import com.flowclient.mods.zoom.ZoomifySettings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class FlowModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowClientMod.MOD_ID);
    private static final Object SAVE_LOCK = new Object();
    private static final ScheduledExecutorService SAVE_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FlowClient-ConfigSave");
        t.setDaemon(true);
        return t;
    });

    private static volatile ScheduledFuture<?> pendingSave;

    private FlowModConfig() {
    }

    public static void registerLifecycle() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> load());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> FullbrightController.sync());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> flushSave());
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("flowclient-mods.json");
    }

    public static void load() {
        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            LOGGER.info("No mod config found at {} — creating initial config with all mods disabled", path);
            writeNow();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            loadModStates(json);
            loadSettings(json);
            writeNow();
            LOGGER.info("Loaded and normalized FlowClient mod config from {}", path);
        } catch (Exception e) {
            LOGGER.error("Failed to load FlowClient mod config from {}", path, e);
        }
    }

    public static void save() {
        scheduleSave(300L);
    }

    public static void flushSave() {
        synchronized (SAVE_LOCK) {
            if (pendingSave != null) {
                pendingSave.cancel(false);
                pendingSave = null;
            }
            writeNow();
        }
    }

    private static void scheduleSave(long delayMs) {
        synchronized (SAVE_LOCK) {
            if (pendingSave != null) {
                pendingSave.cancel(false);
            }
            pendingSave = SAVE_SCHEDULER.schedule(FlowModConfig::writeNow, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private static void writeNow() {
        synchronized (SAVE_LOCK) {
            pendingSave = null;
            Path path = configPath();
            JsonObject json = buildJson();
            String contents = GsonHelper.toStableString(json);

            try {
                Files.createDirectories(path.getParent());
                if (Files.isRegularFile(path)) {
                    Path backup = path.resolveSibling(path.getFileName() + ".bak");
                    try {
                        Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception backupFailed) {
                        LOGGER.debug("Could not back up mod config before save: {}", backupFailed.toString());
                    }
                }
                Path temp = path.resolveSibling(path.getFileName() + ".tmp");
                Files.writeString(temp, contents);
                try {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception atomicFailed) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to save FlowClient mod config to {}", path, e);
            }
        }
    }

    private static JsonObject buildJson() {
        JsonObject json = new JsonObject();
        json.addProperty("nametag", NametagMod.isEnabled());
        json.addProperty("modifyF3", ModifyF3Mod.isEnabled());
        json.addProperty("zoomify", ZoomifyMod.isEnabled());
        json.addProperty("freelook", FreelookMod.isEnabled());
        json.addProperty("keystrokes", KeystrokesMod.isEnabled());
        json.addProperty("armorHud", ArmorHudMod.isEnabled());
        json.addProperty("timeChanger", TimeChangerMod.isEnabled());
        json.addProperty("weatherChanger", WeatherChangerMod.isEnabled());
        json.addProperty("blockOverlay", BlockOverlayMod.isEnabled());
        json.addProperty("crosshair", CrosshairMod.isEnabled());
        json.addProperty("scoreboard", ScoreboardMod.isEnabled());
        json.addProperty("itemPhysics", ItemPhysicsMod.isEnabled());
        json.addProperty("motionBlur", MotionBlurMod.isEnabled());
        json.addProperty("motionBlurAmount", MotionBlurMod.getBlurAmount());
        json.addProperty("otherClient", com.flowclient.mods.otherclient.OtherClientMod.isEnabled());
        json.addProperty("voiceChat", com.flowclient.mods.voicechat.VoiceChatMod.isEnabled());
        json.addProperty("allSchematics", com.flowclient.mods.schematics.AllSchematicsMod.isEnabled());
        json.addProperty("fpsCounter", FpsCounterMod.isEnabled());
        json.addProperty("flowQuiet", FlowQuietMod.isEnabled());
        json.addProperty("flowClock", FlowClockMod.isEnabled());
        json.addProperty("healthBar", HealthBarMod.isEnabled());
        json.addProperty("blockSpeed", BlockSpeedMod.isEnabled());
        json.addProperty("blockBreakProgress", BlockBreakProgressMod.isEnabled());
        json.addProperty("armorDurabilityAlert", ArmorDurabilityAlertMod.isEnabled());
        json.addProperty("itemCooldownHud", ItemCooldownHudMod.isEnabled());
        json.addProperty("serverAddressHud", ServerAddressHudMod.isEnabled());
        json.addProperty("batteryHud", BatteryHudMod.isEnabled());
        json.addProperty("fullbright", FullbrightMod.isEnabled());
        json.addProperty("flowTab", FlowTabMod.isEnabled());
        json.addProperty("mediaHud", MediaHudMod.isEnabled());
        json.addProperty("chatTweaks", ChatTweaksMod.isEnabled());
        json.addProperty("shulkerTooltip", ShulkerTooltipMod.isEnabled());
        json.addProperty("hitboxes", HitboxesMod.isEnabled());
        json.addProperty("chunkBorders", ChunkBordersMod.isEnabled());
        json.addProperty("damageIndicator", DamageIndicatorMod.isEnabled());
        json.addProperty("inventorySort", InventorySortMod.isEnabled());
        json.addProperty("advancementsUnlock", AdvancementsUnlockMod.isEnabled());
        json.addProperty("portalFx", PortalFxMod.isEnabled());
        json.addProperty("comboCounter", ComboCounterMod.isEnabled());
        json.addProperty("trajectoryArc", TrajectoryArcMod.isEnabled());
        json.addProperty("pearlLandingMarker", PearlLandingMarkerMod.isEnabled());
        json.addProperty("elytraFlightPath", ElytraFlightPathMod.isEnabled());
        json.addProperty("reachRing", ReachRingMod.isEnabled());
        json.addProperty("crystalBedPreview", CrystalBedPreviewMod.isEnabled());
        json.addProperty("coordinatesHud", CoordinatesHudMod.isEnabled());
        json.addProperty("pingHud", PingHudMod.isEnabled());
        json.addProperty("jei", JeiMod.isEnabled());
        json.addProperty("blockAnimation", com.flowclient.mods.render.BlockAnimationMod.isEnabled());
        ZoomifySettings.save(json);
        TimeChangerSettings.save(json);
        WeatherChangerSettings.save(json);
        KeystrokesSettings.save(json);
        ArmorHudSettings.save(json);
        FpsCounterSettings.save(json);
        FlowQuietSettings.save(json);
        FlowClockSettings.save(json);
        HealthBarSettings.save(json);
        BlockSpeedSettings.save(json);
        BlockBreakProgressSettings.save(json);
        ArmorDurabilityAlertSettings.save(json);
        ItemCooldownHudSettings.save(json);
        ServerAddressHudSettings.save(json);
        BatteryHudSettings.save(json);
        CoordinatesHudSettings.save(json);
        PingHudSettings.save(json);
        MediaHudSettings.save(json);
        ScoreboardSettings.save(json);
        BlockOverlaySettings.save(json);
        CrosshairSettings.save(json);
        FlowTabSettings.save(json);
        ChatTweaksSettings.save(json);
        DamageIndicatorSettings.save(json);
        InventorySortSettings.save(json);
        HudLayoutManager.get().save(json);
        return json;
    }

    private static void loadModStates(JsonObject json) {
        safeLoad("nametag", () -> NametagMod.setEnabled(GsonHelper.getAsBoolean(json, "nametag", false), false));
        safeLoad("modifyF3", () -> ModifyF3Mod.setEnabled(GsonHelper.getAsBoolean(json, "modifyF3", false), false));
        safeLoad("zoomify", () -> ZoomifyMod.setEnabled(GsonHelper.getAsBoolean(json, "zoomify", false), false));
        safeLoad("freelook", () -> FreelookMod.setEnabled(GsonHelper.getAsBoolean(json, "freelook", false), false));
        safeLoad("keystrokes", () -> KeystrokesMod.setEnabled(GsonHelper.getAsBoolean(json, "keystrokes", false), false));
        safeLoad("armorHud", () -> ArmorHudMod.setEnabled(GsonHelper.getAsBoolean(json, "armorHud", false), false));
        migrateLegacyTimeWeather(json);
        safeLoad("timeChanger", () -> TimeChangerMod.setEnabled(GsonHelper.getAsBoolean(json, "timeChanger", false), false));
        safeLoad("weatherChanger", () -> WeatherChangerMod.setEnabled(GsonHelper.getAsBoolean(json, "weatherChanger", false), false));
        safeLoad("blockOverlay", () -> BlockOverlayMod.setEnabled(GsonHelper.getAsBoolean(json, "blockOverlay", false), false));
        safeLoad("crosshair", () -> CrosshairMod.setEnabled(GsonHelper.getAsBoolean(json, "crosshair", false), false));
        safeLoad("scoreboard", () -> ScoreboardMod.setEnabled(GsonHelper.getAsBoolean(json, "scoreboard", false), false));
        safeLoad("itemPhysics", () -> ItemPhysicsMod.setEnabled(GsonHelper.getAsBoolean(json, "itemPhysics", false), false));
        safeLoad("motionBlur", () -> {
            MotionBlurMod.setEnabled(GsonHelper.getAsBoolean(json, "motionBlur", false), false);
            MotionBlurMod.setBlurAmount(GsonHelper.getAsFloat(json, "motionBlurAmount", 0.5f), false);
        });
        safeLoad("otherClient", () -> com.flowclient.mods.otherclient.OtherClientMod.setEnabled(GsonHelper.getAsBoolean(json, "otherClient", false), false));
        safeLoad("voiceChat", () -> com.flowclient.mods.voicechat.VoiceChatMod.setEnabled(GsonHelper.getAsBoolean(json, "voiceChat", false), false));
        safeLoad("allSchematics", () -> com.flowclient.mods.schematics.AllSchematicsMod.setEnabled(GsonHelper.getAsBoolean(json, "allSchematics", false), false));
        safeLoad("fpsCounter", () -> FpsCounterMod.setEnabled(GsonHelper.getAsBoolean(json, "fpsCounter", false), false));
        safeLoad("flowQuiet", () -> FlowQuietMod.setEnabled(GsonHelper.getAsBoolean(json, "flowQuiet", false), false));
        safeLoad("flowClock", () -> FlowClockMod.setEnabled(GsonHelper.getAsBoolean(json, "flowClock", false), false));
        safeLoad("healthBar", () -> HealthBarMod.setEnabled(GsonHelper.getAsBoolean(json, "healthBar", false), false));
        safeLoad("blockSpeed", () -> BlockSpeedMod.setEnabled(GsonHelper.getAsBoolean(json, "blockSpeed", false), false));
        safeLoad("blockBreakProgress", () -> BlockBreakProgressMod.setEnabled(GsonHelper.getAsBoolean(json, "blockBreakProgress", false), false));
        safeLoad("armorDurabilityAlert", () -> ArmorDurabilityAlertMod.setEnabled(GsonHelper.getAsBoolean(json, "armorDurabilityAlert", false), false));
        safeLoad("itemCooldownHud", () -> ItemCooldownHudMod.setEnabled(GsonHelper.getAsBoolean(json, "itemCooldownHud", false), false));
        safeLoad("serverAddressHud", () -> ServerAddressHudMod.setEnabled(GsonHelper.getAsBoolean(json, "serverAddressHud", false), false));
        safeLoad("batteryHud", () -> BatteryHudMod.setEnabled(GsonHelper.getAsBoolean(json, "batteryHud", false), false));
        safeLoad("fullbright", () -> FullbrightMod.setEnabled(GsonHelper.getAsBoolean(json, "fullbright", false), false));
        safeLoad("flowTab", () -> FlowTabMod.setEnabled(GsonHelper.getAsBoolean(json, "flowTab", false), false));
        safeLoad("mediaHud", () -> MediaHudMod.setEnabled(GsonHelper.getAsBoolean(json, "mediaHud", false), false));
        safeLoad("chatTweaks", () -> ChatTweaksMod.setEnabled(GsonHelper.getAsBoolean(json, "chatTweaks", false), false));
        safeLoad("shulkerTooltip", () -> ShulkerTooltipMod.setEnabled(GsonHelper.getAsBoolean(json, "shulkerTooltip", false), false));
        safeLoad("hitboxes", () -> HitboxesMod.setEnabled(GsonHelper.getAsBoolean(json, "hitboxes", false), false));
        safeLoad("chunkBorders", () -> ChunkBordersMod.setEnabled(GsonHelper.getAsBoolean(json, "chunkBorders", false), false));
        safeLoad("damageIndicator", () -> DamageIndicatorMod.setEnabled(GsonHelper.getAsBoolean(json, "damageIndicator", false), false));
        safeLoad("inventorySort", () -> InventorySortMod.setEnabled(GsonHelper.getAsBoolean(json, "inventorySort", false), false));
        safeLoad("advancementsUnlock", () -> AdvancementsUnlockMod.setEnabled(GsonHelper.getAsBoolean(json, "advancementsUnlock", false), false));
        safeLoad("portalFx", () -> PortalFxMod.setEnabled(GsonHelper.getAsBoolean(json, "portalFx", false), false));
        safeLoad("comboCounter", () -> ComboCounterMod.setEnabled(GsonHelper.getAsBoolean(json, "comboCounter", false), false));
        safeLoad("trajectoryArc", () -> TrajectoryArcMod.setEnabled(GsonHelper.getAsBoolean(json, "trajectoryArc", false), false));
        safeLoad("pearlLandingMarker", () -> PearlLandingMarkerMod.setEnabled(GsonHelper.getAsBoolean(json, "pearlLandingMarker", false), false));
        safeLoad("elytraFlightPath", () -> ElytraFlightPathMod.setEnabled(GsonHelper.getAsBoolean(json, "elytraFlightPath", false), false));
        safeLoad("reachRing", () -> ReachRingMod.setEnabled(GsonHelper.getAsBoolean(json, "reachRing", false), false));
        safeLoad("crystalBedPreview", () -> CrystalBedPreviewMod.setEnabled(GsonHelper.getAsBoolean(json, "crystalBedPreview", false), false));
        safeLoad("coordinatesHud", () -> CoordinatesHudMod.setEnabled(GsonHelper.getAsBoolean(json, "coordinatesHud", false), false));
        safeLoad("pingHud", () -> PingHudMod.setEnabled(GsonHelper.getAsBoolean(json, "pingHud", false), false));
        safeLoad("blockAnimation", () -> com.flowclient.mods.render.BlockAnimationMod.setEnabled(GsonHelper.getAsBoolean(json, "blockAnimation", false), false));
        safeLoad("jei", () -> {
            boolean jeiEnabled = GsonHelper.getAsBoolean(json, "jei", false);
            LOGGER.info("[flowclient-jei] loaded jei={} from {}", jeiEnabled, configPath());
            JeiMod.setEnabled(jeiEnabled, false);
        });
    }

    private static void loadSettings(JsonObject json) {
        safeLoad("zoomifySettings", () -> ZoomifySettings.load(json));
        safeLoad("timeChangerSettings", () -> TimeChangerSettings.load(json));
        safeLoad("weatherChangerSettings", () -> WeatherChangerSettings.load(json));
        safeLoad("keystrokesSettings", () -> KeystrokesSettings.load(json));
        safeLoad("armorHudSettings", () -> ArmorHudSettings.load(json));
        safeLoad("fpsCounterSettings", () -> FpsCounterSettings.load(json));
        safeLoad("flowQuietSettings", () -> FlowQuietSettings.load(json));
        safeLoad("flowClockSettings", () -> FlowClockSettings.load(json));
        safeLoad("healthBarSettings", () -> HealthBarSettings.load(json));
        safeLoad("blockSpeedSettings", () -> BlockSpeedSettings.load(json));
        safeLoad("blockBreakProgressSettings", () -> BlockBreakProgressSettings.load(json));
        safeLoad("armorDurabilityAlertSettings", () -> ArmorDurabilityAlertSettings.load(json));
        safeLoad("itemCooldownHudSettings", () -> ItemCooldownHudSettings.load(json));
        safeLoad("serverAddressHudSettings", () -> ServerAddressHudSettings.load(json));
        safeLoad("batteryHudSettings", () -> BatteryHudSettings.load(json));
        safeLoad("coordinatesHudSettings", () -> CoordinatesHudSettings.load(json));
        safeLoad("pingHudSettings", () -> PingHudSettings.load(json));
        safeLoad("mediaHudSettings", () -> MediaHudSettings.load(json));
        safeLoad("scoreboardSettings", () -> ScoreboardSettings.load(json));
        safeLoad("blockOverlaySettings", () -> BlockOverlaySettings.load(json));
        safeLoad("crosshairSettings", () -> CrosshairSettings.load(json));
        safeLoad("flowTabSettings", () -> FlowTabSettings.load(json));
        safeLoad("chatTweaksSettings", () -> ChatTweaksSettings.load(json));
        safeLoad("damageIndicatorSettings", () -> DamageIndicatorSettings.load(json));
        safeLoad("inventorySortSettings", () -> InventorySortSettings.load(json));
        safeLoad("hudLayouts", () -> HudLayoutManager.get().load(json));
    }

    private static void safeLoad(String section, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            LOGGER.warn("Failed to load config section '{}'", section, e);
        }
    }

    private static void migrateLegacyTimeWeather(JsonObject json) {
        if (!json.has("timeWeather") || json.has("timeChanger") || json.has("weatherChanger")) {
            return;
        }

        boolean legacyEnabled = json.get("timeWeather").getAsBoolean();
        json.addProperty("timeChanger", legacyEnabled);
        json.addProperty("weatherChanger", legacyEnabled);
    }
}
