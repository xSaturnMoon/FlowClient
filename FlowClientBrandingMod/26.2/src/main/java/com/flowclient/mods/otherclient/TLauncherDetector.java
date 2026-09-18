package com.flowclient.mods.otherclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client-side TLauncher detection.
 * <p>
 * Network servers (Jartex, etc.) assign their own UUID, so we cannot require the tab UUID to match
 * the Ely account UUID. A registered TLauncher.org account is detected when Mojang has no premium
 * profile for the nickname but Ely does.
 */
public final class TLauncherDetector {

    private static final String MOJANG_PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final String ELY_PROFILE_URL = "https://authserver.ely.by/api/users/profiles/minecraft/%s";
    private static final String ELY_SKIN_PROFILE_URL = "http://skinsystem.ely.by/profile/%s";

    private static final long RETRY_AFTER_MS = 45_000L;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(6, r -> {
        Thread t = new Thread(r, "FlowClient-TLauncher-Detector");
        t.setDaemon(true);
        return t;
    });

    private static final Set<UUID> CONFIRMED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_QUERY = new ConcurrentHashMap<>();
    private static final Map<UUID, String> PENDING_USERNAMES = new ConcurrentHashMap<>();

    private TLauncherDetector() {}

    public static DetectedClient detect(UUID uuid, String username, PlayerInfo info) {
        if (uuid == null || BedrockDetector.detect(uuid, username, info) != null) {
            return null;
        }
        if (!ClientBadgeHelper.isValidUsername(username)) {
            return null;
        }

        if (CONFIRMED.contains(uuid)) {
            return DetectedClient.TLAUNCHER;
        }

        GameProfile profile = info != null ? info.getProfile() : null;
        if (hasTlauncherTextureSignal(info, profile)) {
            return DetectedClient.TLAUNCHER;
        }

        if (hasTlauncherAccountUuid(uuid)) {
            return DetectedClient.TLAUNCHER;
        }

        if (profile != null && hasElyAuthProperty(profile)) {
            return DetectedClient.TLAUNCHER;
        }

        return null;
    }

    public static void queryAsync(UUID uuid, String username) {
        if (uuid == null || username == null || username.isBlank() || CONFIRMED.contains(uuid)) {
            return;
        }

        if (hasTlauncherAccountUuid(uuid)) {
            confirm(uuid);
            return;
        }

        long now = System.currentTimeMillis();
        Long last = LAST_QUERY.get(uuid);
        if (last != null && now - last < RETRY_AFTER_MS) {
            return;
        }

        LAST_QUERY.put(uuid, now);
        PENDING_USERNAMES.put(uuid, username);

        EXECUTOR.submit(() -> evaluate(uuid, username));
    }

    private static void evaluate(UUID uuid, String username) {
        try {
            if (BedrockDetector.hasFloodgateUuid(uuid) || BedrockDetector.hasFloodgatePrefix(username)) {
                return;
            }

            if (hasTlauncherAccountUuid(uuid)) {
                confirm(uuid);
                return;
            }

            String mojangId = fetchProfileId(String.format(MOJANG_PROFILE_URL, username));
            // Name is registered on Mojang (premium account). Network servers use their own UUID,
            // so a UUID mismatch must NOT be treated as TLauncher — that caused false positives
            // on premium players (e.g. server owners on CoralMC).
            if (mojangId != null) {
                return;
            }

            String elyId = fetchProfileId(String.format(ELY_PROFILE_URL, username));
            if (elyId != null && uuidMatches(uuid, elyId)) {
                confirm(uuid);
                return;
            }

            if (hasTlauncherHostedElySkin(username)) {
                confirm(uuid);
            }
        } catch (Exception ignored) {
            LAST_QUERY.remove(uuid);
        }
    }

    private static void confirm(UUID uuid) {
        CONFIRMED.add(uuid);
        PENDING_USERNAMES.remove(uuid);
        ClientDetector.mergeTLauncherPlayer(uuid);
    }

    public static boolean hasTlauncherAccountUuid(UUID uuid) {
        if (uuid == null || uuid.version() != 1) {
            return false;
        }

        long node = uuid.getLeastSignificantBits() & 0x0000FFFFFFFFFFFFL;
        if (node == 0L || (node & 0xFFFFFFL) != 0L) {
            return false;
        }

        int marker = (int) ((node >> 24) & 0xFFFF);
        return marker == 0x0403 || marker == 0x0304 || marker == 0x0400 || marker == 0x0300;
    }

    private static boolean hasElyAuthProperty(GameProfile profile) {
        return profile != null && profile.properties().containsKey("ely");
    }

    static boolean isTlauncherTextureUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("tlauncher.org")
                || lower.contains("tlauncher.ru")
                || lower.contains("repo.tlauncher.org")
                || lower.contains("res.tlauncher.org")
                || lower.contains("page.tlauncher.org")
                || lower.contains("auth.tlauncher")
                || lower.contains("tlmods.org")
                || lower.contains("org/tlauncher")
                || lower.contains("tl_skin")
                || lower.contains("tlskincape");
    }

    private static boolean hasTlauncherTextureSignal(PlayerInfo info, GameProfile profile) {
        if (info != null) {
            try {
                PlayerSkin skin = info.getSkin();
                if (skin != null) {
                    if (isTlauncherTextureUrl(textureUrl(skin.body()))) return true;
                    if (isTlauncherTextureUrl(textureUrl(skin.cape()))) return true;
                    if (isTlauncherTextureUrl(textureUrl(skin.elytra()))) return true;
                }
            } catch (Exception ignored) {}
        }

        if (profile != null) {
            return profileTexturesContainTlauncher(profile);
        }
        return false;
    }

    private static String textureUrl(ClientAsset.Texture texture) {
        if (texture == null) {
            return null;
        }
        if (texture instanceof ClientAsset.DownloadedTexture downloaded) {
            return firstNonBlank(downloaded.url(), downloaded.texturePath().toString(), downloaded.id().toString());
        }
        if (texture instanceof ClientAsset.ResourceTexture resource) {
            return firstNonBlank(resource.texturePath().toString(), resource.id().toString());
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean profileTexturesContainTlauncher(GameProfile profile) {
        return profileTexturesMatch(profile, TLauncherDetector::isTlauncherTextureUrl);
    }

    static boolean hasVanillaMojangSkin(PlayerInfo info) {
        if (info == null) {
            return false;
        }
        try {
            PlayerSkin skin = info.getSkin();
            if (skin == null) {
                return false;
            }
            String body = textureUrl(skin.body());
            return body != null && isMojangTextureHost(body.toLowerCase(Locale.ROOT));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isMojangTextureHost(String lower) {
        return lower.contains("textures.minecraft.net") || lower.contains("texture.minecraft.net");
    }

    private static boolean profileTexturesMatch(GameProfile profile, java.util.function.Predicate<String> matcher) {
        Collection<Property> textures = profile.properties().get("textures");
        if (textures == null || textures.isEmpty()) {
            return false;
        }

        for (Property property : textures) {
            if (matcher.test(property.value())) {
                return true;
            }

            try {
                String decoded = new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8);
                if (matcher.test(decoded)) {
                    return true;
                }

                JsonElement root = JsonParser.parseString(decoded);
                if (!root.isJsonObject()) {
                    continue;
                }
                JsonObject rootObj = root.getAsJsonObject();
                if (!rootObj.has("textures") || !rootObj.get("textures").isJsonObject()) {
                    continue;
                }
                JsonObject entries = rootObj.getAsJsonObject("textures");
                for (String key : entries.keySet()) {
                    JsonElement entry = entries.get(key);
                    if (!entry.isJsonObject()) {
                        continue;
                    }
                    JsonObject entryObj = entry.getAsJsonObject();
                    if (entryObj.has("url") && matcher.test(entryObj.get("url").getAsString())) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static boolean hasTlauncherHostedElySkin(String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(ELY_SKIN_PROFILE_URL, username)))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "FlowClient-OtherClient/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().isBlank()) {
                return false;
            }

            JsonElement root = JsonParser.parseString(response.body());
            if (!root.isJsonObject()) {
                return false;
            }

            JsonObject profile = root.getAsJsonObject();
            if (!profile.has("properties") || !profile.get("properties").isJsonArray()) {
                return false;
            }

            for (JsonElement propertyElement : profile.getAsJsonArray("properties")) {
                if (!propertyElement.isJsonObject()) {
                    continue;
                }
                JsonObject property = propertyElement.getAsJsonObject();
                if (!property.has("name") || !"textures".equals(property.get("name").getAsString())) {
                    continue;
                }
                if (!property.has("value")) {
                    continue;
                }
                String decoded = new String(
                        Base64.getDecoder().decode(property.get("value").getAsString()),
                        StandardCharsets.UTF_8
                );
                JsonElement texturesRoot = JsonParser.parseString(decoded);
                if (!texturesRoot.isJsonObject() || !texturesRoot.getAsJsonObject().has("textures")) {
                    continue;
                }
                JsonObject textures = texturesRoot.getAsJsonObject().getAsJsonObject("textures");
                if (!textures.has("SKIN") || !textures.get("SKIN").isJsonObject()) {
                    continue;
                }
                JsonObject skin = textures.getAsJsonObject("SKIN");
                if (!skin.has("url")) {
                    continue;
                }
                if (isTlauncherTextureUrl(skin.get("url").getAsString())) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static String fetchProfileId(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "FlowClient-OtherClient/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().isBlank()) {
                return null;
            }

            JsonElement root = JsonParser.parseString(response.body());
            if (!root.isJsonObject() || !root.getAsJsonObject().has("id")) {
                return null;
            }
            return root.getAsJsonObject().get("id").getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean uuidMatches(UUID liveUuid, String profileId) {
        if (liveUuid == null || profileId == null || profileId.isBlank()) {
            return false;
        }
        String normalized = profileId.replace("-", "").toLowerCase(Locale.ROOT);
        return liveUuid.toString().replace("-", "").equalsIgnoreCase(normalized);
    }

    public static void clearCache() {
        CONFIRMED.clear();
        LAST_QUERY.clear();
        PENDING_USERNAMES.clear();
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
