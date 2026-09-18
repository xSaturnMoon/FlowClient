package com.flowclient;

import com.flowclient.modpanel.ModPanelKeys;
import com.flowclient.mods.FlowModConfig;
import com.flowclient.mods.ModKeybindCategoryFilter;
import com.flowclient.mods.fullbright.FullbrightKeys;
import com.flowclient.mods.freelook.FreelookKeys;
import com.flowclient.mods.otherclient.ClientPresenceNetworking;
import com.flowclient.mods.otherclient.FlowEmblemTextures;
import com.flowclient.mods.schematics.AllSchematicsKeys;
import com.flowclient.mods.schematics.AllSchematicsMod;
import com.flowclient.mods.voicechat.VoiceChatJarManager;
import com.flowclient.mods.voicechat.VoiceChatRuntimeController;
import com.flowclient.mods.jei.JeiRuntimeController;
import com.flowclient.mods.immersion.ImmersionTickController;
import com.flowclient.mods.preview.WorldPreviewTickController;
import com.flowclient.mods.advancements.AdvancementsUnlockController;
import com.flowclient.mods.blockspeed.BlockSpeedTracker;
import com.flowclient.mods.inventory.InventorySortKeys;
import com.flowclient.mods.zoom.ZoomifyKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class FlowClientMod implements ClientModInitializer {
    public static final String MOD_ID = "flowclient";

    @Override
    public void onInitializeClient() {
        FlowModConfig.registerLifecycle();
        FlowSessionFiles.register();
        ClientPresenceNetworking.registerClient();
        com.flowclient.mods.otherclient.ClientPresenceAPI.fetchGlobalPresence();
        FlowEmblemTextures.register();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> FlowShutdown.onClientStopping());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> FlowEmblemTextures.load());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> com.flowclient.mods.quiet.FlowQuietController.onClientStarted(client));
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                VoiceChatJarManager.ensureInstalled();
            } catch (Exception ignored) {
            }
            VoiceChatRuntimeController.logStartupDiagnostics();
            VoiceChatRuntimeController.syncFromFlowConfig();
            ModKeybindCategoryFilter.logKnownCategoriesOnce();
            JeiRuntimeController.logStartupDiagnostics();
            JeiRuntimeController.syncFromFlowConfig();
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> com.flowclient.mods.media.MediaSessionPoller.get().start());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> com.flowclient.mods.media.MediaSessionPoller.get().stop());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> com.flowclient.mods.battery.BatteryPoller.get().start());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> com.flowclient.mods.battery.BatteryPoller.get().stop());
        ModPanelKeys.register();
        ZoomifyKeys.register();
        FreelookKeys.register();
        FullbrightKeys.register();
        AllSchematicsMod.register();
        AllSchematicsKeys.register();
        InventorySortKeys.register();

        // Scan ALL online players as soon as we join a server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> com.flowclient.mods.otherclient.ClientDetector.scanOnlinePlayers());
        });

        // Also clear the cache when disconnecting so the next server is fresh
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            com.flowclient.mods.otherclient.ClientDetector.clearCache();
            AdvancementsUnlockController.get().reset();
        });

        // Rescan in small batches so large cracked networks stay covered without flooding HTTP.
        int[] tickCounter = {0};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            com.flowclient.mods.tab.FlowTabCinematicController.tick(client);
            com.flowclient.mods.quiet.FlowQuietController.tick(client);
            com.flowclient.mods.chat.ChatTweaksController.tick(client);
            com.flowclient.mods.render.DebugVisualController.tick(client);
            com.flowclient.mods.render.DamageIndicatorTracker.tick(client);
            BlockSpeedTracker.tick(client);
            AdvancementsUnlockController.get().tick(client);
            ImmersionTickController.tick(client);
            WorldPreviewTickController.tick(client);
            VoiceChatRuntimeController.enforce();
            JeiRuntimeController.enforce();
            if (client.getConnection() == null) return;
            tickCounter[0]++;
            if (tickCounter[0] >= 100) { // every 5 seconds
                tickCounter[0] = 0;
                com.flowclient.mods.otherclient.ClientDetector.scanOnlinePlayers();
            }
        });
    }

}
