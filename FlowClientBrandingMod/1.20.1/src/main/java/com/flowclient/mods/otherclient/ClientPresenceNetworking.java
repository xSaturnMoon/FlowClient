package com.flowclient.mods.otherclient;

import com.flowclient.FlowClientMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.UUID;

/**
 * Announces FlowClient (and reads announcements from other FlowClient users)
 * when the server also runs the FlowClient relay.
 */
public final class ClientPresenceNetworking {
    public static final ResourceLocation CHANNEL =
            new ResourceLocation(FlowClientMod.MOD_ID, "client_presence");

    private ClientPresenceNetworking() {}

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, responseSender) -> {
            UUID playerId = buf.readUUID();
            String clientId = buf.readUtf();
            client.execute(() -> {
                DetectedClient detected = DetectedClient.fromClientId(clientId);
                ClientDetector.setNetworkClient(playerId, detected);
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!OtherClientMod.isEnabled()) {
                return;
            }
            if (ClientPlayNetworking.canSend(CHANNEL) && client.player != null) {
                FriendlyByteBuf packet = PacketByteBufs.create();
                packet.writeUUID(client.player.getUUID());
                packet.writeUtf(DetectedClient.FLOWCLIENT.clientId);
                ClientPlayNetworking.send(CHANNEL, packet);
            }
        });
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, (server, player, handler, buf, responseSender) -> {
            UUID playerId = buf.readUUID();
            String clientId = buf.readUtf();
            if (!player.getUUID().equals(playerId)) {
                return;
            }

            String normalized = clientId.toLowerCase(Locale.ROOT);
            for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                FriendlyByteBuf packet = PacketByteBufs.create();
                packet.writeUUID(player.getUUID());
                packet.writeUtf(normalized);
                ServerPlayNetworking.send(target, CHANNEL, packet);
            }
        });
    }
}
