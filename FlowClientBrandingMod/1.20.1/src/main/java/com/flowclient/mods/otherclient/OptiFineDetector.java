package com.flowclient.mods.otherclient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Detects OptiFine players by querying the OptiFine cape API.
 * OptiFine capes are NOT stored in the GameProfile textures property,
 * so the only way to detect them is via HTTP request to s.optifine.net.
 */
public final class OptiFineDetector {

    private static final String CAPE_URL = "http://s.optifine.net/capes/%s.png";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "FlowClient-OptiFine-Detector");
        t.setDaemon(true);
        return t;
    });

    // Confirmed: player has OptiFine cape
    private static final Set<UUID> HAS_OPTIFINE = ConcurrentHashMap.newKeySet();
    // Already queried: don't spam requests
    private static final Set<UUID> QUERIED = ConcurrentHashMap.newKeySet();

    private OptiFineDetector() {}

    /**
     * Asynchronously checks if a player has an OptiFine cape.
     * When confirmed, stores the result so ClientDetector can pick it up.
     *
     * @param uuid     player UUID
     * @param username player username
     */
    public static void queryAsync(UUID uuid, String username) {
        if (uuid == null || username == null || username.isBlank()) return;
        if (QUERIED.contains(uuid)) return;
        QUERIED.add(uuid);

        EXECUTOR.submit(() -> {
            try {
                String url = String.format(CAPE_URL, username);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<Void> response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) {
                    HAS_OPTIFINE.add(uuid);
                    // Update the detector cache so the badge appears next render frame
                    ClientDetector.mergeOptiFinePlayer(uuid);
                }
            } catch (Exception ignored) {
                // Silent fail - no cape = not OptiFine, or network error
            }
        });
    }

    public static boolean hasOptiFine(UUID uuid) {
        return HAS_OPTIFINE.contains(uuid);
    }

    public static void clearCache() {
        HAS_OPTIFINE.clear();
        QUERIED.clear();
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
