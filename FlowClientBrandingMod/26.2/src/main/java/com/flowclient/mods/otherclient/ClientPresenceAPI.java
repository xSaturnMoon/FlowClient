package com.flowclient.mods.otherclient;

import com.flowclient.FlowClientMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

public final class ClientPresenceAPI {
    // Sostituisci questo URL con il link raw del tuo file JSON (es. GitHub Raw, Pastebin Raw, o una tua API)
    public static final String USERS_API_URL = "https://raw.githubusercontent.com/FlowClient/presence/main/users.json";

    private static final Set<UUID> GLOBAL_FLOW_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FlowClient-Presence-Thread");
        t.setDaemon(true);
        return t;
    });

    private ClientPresenceAPI() {}

    public static void fetchGlobalPresence() {
        EXECUTOR.submit(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(USERS_API_URL))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "FlowClient-Presence-Fetcher")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    parseUsersJson(response.body());
                } else {
                    System.err.println("[FlowClient] Failed to fetch presence API: HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("[FlowClient] Failed to fetch presence API: " + e.getMessage());
            }
        });
    }

    private static void parseUsersJson(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (root.isJsonObject()) {
                JsonObject rootObj = root.getAsJsonObject();
                if (rootObj.has("players") && rootObj.get("players").isJsonArray()) {
                    JsonArray players = rootObj.getAsJsonArray("players");
                    for (JsonElement element : players) {
                        try {
                            if (element.isJsonPrimitive()) {
                                UUID uuid = UUID.fromString(element.getAsString());
                                GLOBAL_FLOW_PLAYERS.add(uuid);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
            System.out.println("[FlowClient] Loaded " + GLOBAL_FLOW_PLAYERS.size() + " FlowClient users from global API.");
        } catch (Exception e) {
            System.err.println("[FlowClient] Failed to parse presence JSON: " + e.getMessage());
        }
    }

    public static boolean isFlowPlayer(UUID uuid) {
        return GLOBAL_FLOW_PLAYERS.contains(uuid);
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
