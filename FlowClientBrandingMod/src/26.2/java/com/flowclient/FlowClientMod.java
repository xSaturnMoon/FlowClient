package com.flowclient;

import com.flowclient.modpanel.ModPanelKeys;
import com.flowclient.mods.FlowModConfig;
import net.fabricmc.api.ClientModInitializer;

public final class FlowClientMod implements ClientModInitializer {
    public static final String MOD_ID = "flowclient";

    @Override
    public void onInitializeClient() {
        FlowModConfig.load();
        ModPanelKeys.register();
    }
}
