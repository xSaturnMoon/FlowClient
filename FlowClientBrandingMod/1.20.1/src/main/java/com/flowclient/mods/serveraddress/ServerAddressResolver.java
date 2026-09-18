package com.flowclient.mods.serveraddress;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerAddressResolver {
    private ServerAddressResolver() {
    }

    public static String resolve(Minecraft mc, ServerAddressDisplayMode mode) {
        if (mc.isLocalServer()) {
            return "Singleplayer";
        }

        return switch (mode) {
            case SERVER_NAME -> resolveServerName(mc);
            case BRAND -> resolveBrand(mc);
            case ADDRESS -> resolveAddress(mc);
        };
    }

    private static String resolveServerName(Minecraft mc) {
        ServerData server = mc.getCurrentServer();
        if (server != null && server.name != null && !server.name.isBlank()) {
            return server.name;
        }
        return resolveBrand(mc);
    }

    private static String resolveBrand(Minecraft mc) {
        ServerData server = mc.getCurrentServer();
        if (server != null && server.name != null && !server.name.isBlank()) {
            return server.name;
        }
        return resolveAddress(mc);
    }

    private static String resolveAddress(Minecraft mc) {
        ServerData server = mc.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isBlank()) {
            return server.ip;
        }
        return "Unknown";
    }
}
