package com.flowclient;

import com.flowclient.neo.ClientNeoForgeEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(FlowClientMod.MOD_ID)
public final class FlowClientNeoForgeMod {
    public FlowClientNeoForgeMod(IEventBus modBus) {
        ClientNeoForgeEvents.register(modBus);
    }
}
