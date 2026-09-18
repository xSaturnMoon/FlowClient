package com.flowclient.mods.otherclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/**
 * Client-side Bedrock / Floodgate detection without server plugins.
 * <p>
 * Signals used (highest reliability first):
 * <ul>
 *   <li>Floodgate UUID layout ({@code 00000000-0000-0000-xxxx-xxxxxxxxxxxx})</li>
 *   <li>Floodgate username prefix ({@code .}, {@code *}, {@code +}) — invalid on real Java accounts</li>
 *   <li>Geyser/Floodgate skin proxy URLs in the player profile</li>
 * </ul>
 */
public final class BedrockDetector {

    private static final char[] FLOODGATE_PREFIXES = {'.', '*', '+'};

    private BedrockDetector() {}

    public static boolean isBedrockPlayer(UUID uuid, String username, PlayerInfo info) {
        return detect(uuid, username, info) != null;
    }

    public static DetectedClient detect(UUID uuid, String username, PlayerInfo info) {
        if (uuid == null || !ClientBadgeHelper.isValidUsername(username)) {
            return null;
        }

        if (hasFloodgateUuid(uuid)) {
            return DetectedClient.BEDROCK;
        }

        if (hasFloodgatePrefix(username)) {
            return DetectedClient.BEDROCK;
        }

        GameProfile profile = info != null ? info.getProfile() : null;
        if (hasGeyserTextureSignal(info, profile)) {
            return DetectedClient.BEDROCK;
        }

        return null;
    }

    /**
     * Floodgate maps Xbox XUIDs to {@code UUID(0, xuid)}, which stringifies to
     * {@code 00000000-0000-0000-xxxx-xxxxxxxxxxxx}.
     */
    public static boolean hasFloodgateUuid(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        if (msb != 0L || lsb == 0L) {
            return false;
        }

        String rendered = uuid.toString().toLowerCase(Locale.ROOT);
        return rendered.startsWith("00000000-0000-0000-")
                && !rendered.equals("00000000-0000-0000-0000-000000000000");
    }

    /**
     * Mojang Java usernames cannot start with these characters; Floodgate uses them as prefixes.
     */
    public static boolean hasFloodgatePrefix(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        char first = username.charAt(0);
        for (char prefix : FLOODGATE_PREFIXES) {
            if (first == prefix) {
                return username.length() >= 2 && isBedrockBodyChar(username.charAt(1));
            }
        }
        return false;
    }

    private static boolean isBedrockBodyChar(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '_';
    }

    private static boolean hasGeyserTextureSignal(PlayerInfo info, GameProfile profile) {
        if (info != null && info.getSkinLocation() != null) {
            if (isGeyserTextureUrl(info.getSkinLocation().toString())) {
                return true;
            }
        }

        if (profile != null) {
            return profileTexturesContainGeyser(profile);
        }
        return false;
    }

    private static boolean profileTexturesContainGeyser(GameProfile profile) {
        Collection<Property> textures = profile.getProperties().get("textures");
        if (textures == null || textures.isEmpty()) {
            return false;
        }

        for (Property property : textures) {
            try {
                String decoded = new String(Base64.getDecoder().decode(property.getValue()), StandardCharsets.UTF_8);
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
                    if (entryObj.has("url") && isGeyserTextureUrl(entryObj.get("url").getAsString())) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    static boolean isGeyserTextureUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("geysermc.org")
                || lower.contains("geysermc.net")
                || lower.contains("floodgate")
                || lower.contains("bedrockskin")
                || lower.contains("skin.geyser");
    }
}
