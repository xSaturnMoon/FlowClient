package com.flowclient.neo;

import com.flowclient.modpanel.ModPanelKeys;
import com.flowclient.mods.FlowModConfig;
import com.flowclient.mods.freelook.FreelookController;
import com.flowclient.mods.freelook.FreelookKeys;
import com.flowclient.mods.schematics.AllSchematicsKeys;
import com.flowclient.mods.schematics.SchematicRenderer;
import com.flowclient.mods.zoom.ZoomController;
import com.flowclient.mods.zoom.ZoomifyKeys;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientNeoForgeEvents {
    private ClientNeoForgeEvents() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientNeoForgeEvents::onClientSetup);
        modBus.addListener(ClientNeoForgeEvents::registerKeys);
        NeoForge.EVENT_BUS.addListener(ClientNeoForgeEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(SchematicRenderer::onRenderLevel);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(FlowModConfig::load);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModPanelKeys.getOpenPanelKey());
        event.register(ZoomifyKeys.getZoomKey());
        event.register(FreelookKeys.getFreelookKey());
        event.register(AllSchematicsKeys.getMenuKey());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            ZoomController.setZoomKeyHeld(false);
            return;
        }

        ZoomController.setZoomKeyHeld(ZoomifyKeys.getZoomKey().isDown());
        ZoomController.updateTargets();

        boolean freelookAllowed = client.screen == null;
        FreelookController.updateKeyState(FreelookKeys.isFreelookHeld(client), freelookAllowed);
        AllSchematicsKeys.tick(client);
    }
}
