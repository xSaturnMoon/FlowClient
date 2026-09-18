package com.flowclient.mods.otherclient;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Client detection. Uses the global presence API for FlowClient and
 * async HTTP requests to the OptiFine cape server for OptiFine detection.
 */
public final class ClientDetector {

    private static final List<DetectedClient> LOADING_SENTINEL = List.of(DetectedClient.LOADING);
    private static final int ASYNC_BATCH_SIZE = 80;

    private static final ConcurrentHashMap<UUID, List<DetectedClient>> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, DetectedClient> NETWORK_OVERRIDES = new ConcurrentHashMap<>();

    private static int scanOffset;

    private ClientDetector() {}

    public static List<DetectedClient> getClients(UUID uuid) {
        return CACHE.getOrDefault(uuid, LOADING_SENTINEL);
    }

    public static DetectedClient getPrimary(UUID uuid) {
        return getClients(uuid).stream()
                .filter(c -> c != DetectedClient.LOADING && c != DetectedClient.VANILLA && c.isGameClient())
                .max(DetectedClient.BY_PRIORITY)
                .orElse(DetectedClient.VANILLA);
    }

    public static void setNetworkClient(UUID uuid, DetectedClient client) {
        NETWORK_OVERRIDES.put(uuid, client);
        mergeAndStore(uuid, EnumSet.of(client));
    }

    public static void inspectTextures(UUID uuid, PlayerInfo info) {
        if (info == null) return;
        Set<DetectedClient> textureHits = ClientTextureAnalyzer.analyze(info);
        if (!textureHits.isEmpty()) {
            mergeAndStore(uuid, textureHits);
        }
    }

    public static void inspectProfileTextures(UUID uuid, GameProfile profile) {
        if (profile == null) return;
        Set<DetectedClient> textureHits = ClientTextureAnalyzer.analyzeProfileTextures(profile);
        if (!textureHits.isEmpty()) {
            mergeAndStore(uuid, textureHits);
        }
    }

    public static void inspectLocal(UUID uuid, String username, PlayerInfo info) {
        if (!ClientBadgeHelper.isValidUsername(username)) return;

        inspectTextures(uuid, info);
        if (info != null) {
            inspectProfileTextures(uuid, info.getProfile());
        }

        DetectedClient bedrock = BedrockDetector.detect(uuid, username, info);
        if (bedrock != null) {
            mergeAndStore(uuid, EnumSet.of(bedrock));
        }

        DetectedClient tlauncher = TLauncherDetector.detect(uuid, username, info);
        if (tlauncher != null) {
            mergeAndStore(uuid, EnumSet.of(tlauncher));
        }

        if (ClientPresenceAPI.isFlowPlayer(uuid)) {
            setNetworkClient(uuid, DetectedClient.FLOWCLIENT);
        } else if (bedrock == null && tlauncher == null) {
            if (getPrimary(uuid) == DetectedClient.VANILLA && !TLauncherDetector.hasVanillaMojangSkin(info)) {
                TLauncherDetector.queryAsync(uuid, username);
            }
            if (tlauncher == null && getPrimary(uuid) != DetectedClient.TLAUNCHER) {
                OptiFineDetector.queryAsync(uuid, username);
            }
        }

        if (!CACHE.containsKey(uuid) && !NETWORK_OVERRIDES.containsKey(uuid)) {
            CACHE.put(uuid, List.of(DetectedClient.VANILLA));
        }
    }

    public static void query(UUID uuid, String username, PlayerInfo info) {
        inspectLocal(uuid, username, info);
    }

    public static void scanOnlinePlayers() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        List<PlayerInfo> players = new ArrayList<>(mc.getConnection().getOnlinePlayers());
        if (players.isEmpty()) {
            return;
        }

        int batch = Math.min(ASYNC_BATCH_SIZE, players.size());
        for (int i = 0; i < batch; i++) {
            int index = (scanOffset + i) % players.size();
            PlayerInfo info = players.get(index);
            inspectLocal(info.getProfile().id(), info.getProfile().name(), info);
        }
        scanOffset = (scanOffset + batch) % players.size();
    }

    public static void clearCache() {
        CACHE.clear();
        NETWORK_OVERRIDES.clear();
        scanOffset = 0;
        OptiFineDetector.clearCache();
        TLauncherDetector.clearCache();
    }

    public static void mergeOptiFinePlayer(UUID uuid) {
        mergeAndStore(uuid, EnumSet.of(DetectedClient.OPTIFINE));
    }

    public static void mergeTLauncherPlayer(UUID uuid) {
        mergeAndStore(uuid, EnumSet.of(DetectedClient.TLAUNCHER));
    }

    private static void mergeAndStore(UUID uuid, Set<DetectedClient> additions) {
        DetectedClient network = NETWORK_OVERRIDES.get(uuid);
        if (network != null) {
            CACHE.put(uuid, List.of(network));
            return;
        }

        Set<DetectedClient> merged = EnumSet.noneOf(DetectedClient.class);
        List<DetectedClient> existing = CACHE.get(uuid);
        if (existing != null) {
            for (DetectedClient client : existing) {
                if (client != DetectedClient.LOADING && client != DetectedClient.VANILLA) {
                    merged.add(client);
                }
            }
        }
        merged.addAll(additions);

        List<DetectedClient> gameClients = merged.stream()
                .filter(DetectedClient::isGameClient)
                .sorted(DetectedClient.BY_PRIORITY)
                .collect(Collectors.toList());

        if (gameClients.isEmpty()) {
            CACHE.put(uuid, List.of(DetectedClient.VANILLA));
            return;
        }

        CACHE.put(uuid, List.of(gameClients.getFirst()));
    }
}
