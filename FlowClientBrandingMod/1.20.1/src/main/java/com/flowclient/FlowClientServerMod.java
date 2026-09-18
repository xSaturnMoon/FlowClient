package com.flowclient;

import com.flowclient.mods.otherclient.ClientPresenceNetworking;
import net.fabricmc.api.DedicatedServerModInitializer;

public final class FlowClientServerMod implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        ClientPresenceNetworking.registerServer();
    }
}
