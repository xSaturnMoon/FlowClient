package com.flowclient.mods.otherclient;

import com.flowclient.FlowClientMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.UUID;

/**
 * Announces FlowClient (and reads announcements from other FlowClient users)
 * when the server also runs the FlowClient relay.
 */
public final class ClientPresenceNetworking {
    public static final Identifier CHANNEL =
            Identifier.fromNamespaceAndPath(FlowClientMod.MOD_ID, "client_presence");

    private ClientPresenceNetworking() {}

    public record Payload(UUID playerId, String clientId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Payload> TYPE =
                new CustomPacketPayload.Type<>(CHANNEL);

        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeUUID(payload.playerId());
                            buf.writeUtf(payload.clientId());
                        },
                        buf -> new Payload(buf.readUUID(), buf.readUtf())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerClient() {
        PayloadTypeRegistry.serverboundPlay().register(Payload.TYPE, Payload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    DetectedClient client = DetectedClient.fromClientId(payload.clientId());
                    ClientDetector.setNetworkClient(payload.playerId(), client);
                }));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!OtherClientMod.isEnabled()) return;
            if (ClientPlayNetworking.canSend(Payload.TYPE) && client.player != null) {
                ClientPlayNetworking.send(new Payload(
                        client.player.getUUID(),
                        DetectedClient.FLOWCLIENT.clientId));
            }
        });
    }

    public static void registerServer() {
        PayloadTypeRegistry.serverboundPlay().register(Payload.TYPE, Payload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            if (!sender.getUUID().equals(payload.playerId())) return;

            MinecraftServer server = context.server();
            String clientId = payload.clientId().toLowerCase(Locale.ROOT);
            for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(target, new Payload(sender.getUUID(), clientId));
            }
        });
    }
}
