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
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Instantly infers a client from resolved skin/cape texture URLs.
 */
public final class ClientTextureAnalyzer {
    private ClientTextureAnalyzer() {}

    public static Set<DetectedClient> analyze(PlayerInfo info) {
        EnumSet<DetectedClient> found = EnumSet.noneOf(DetectedClient.class);
        if (info == null) {
            return found;
        }

        try {
            if (info.getSkinLocation() != null) {
                matchUrl(info.getSkinLocation().toString(), found);
            }
        } catch (Exception ignored) {
        }

        try {
            GameProfile profile = info.getProfile();
            if (profile != null) {
                collectProfileTextures(profile, found);
            }
        } catch (Exception ignored) {
        }

        return found;
    }

    public static Set<DetectedClient> analyzeProfileTextures(GameProfile profile) {
        EnumSet<DetectedClient> found = EnumSet.noneOf(DetectedClient.class);
        if (profile != null) {
            collectProfileTextures(profile, found);
        }
        return found;
    }

    public static Set<DetectedClient> analyzeTextureJson(String decodedTexturesJson) {
        EnumSet<DetectedClient> found = EnumSet.noneOf(DetectedClient.class);
        try {
            JsonElement root = JsonParser.parseString(decodedTexturesJson);
            if (!root.isJsonObject()) {
                return found;
            }
            collectTextureEntries(root.getAsJsonObject(), found);
        } catch (Exception ignored) {
        }
        return found;
    }

    private static void collectProfileTextures(GameProfile profile, Set<DetectedClient> found) {
        Collection<Property> textures = profile.getProperties().get("textures");
        if (textures == null) {
            return;
        }

        for (Property property : textures) {
            try {
                String decoded = new String(Base64.getDecoder().decode(property.getValue()), StandardCharsets.UTF_8);
                JsonElement root = JsonParser.parseString(decoded);
                if (root.isJsonObject()) {
                    collectTextureEntries(root.getAsJsonObject(), found);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void collectTextureEntries(JsonObject root, Set<DetectedClient> found) {
        if (!root.has("textures") || !root.get("textures").isJsonObject()) {
            return;
        }

        JsonObject entries = root.getAsJsonObject("textures");
        for (String key : entries.keySet()) {
            JsonElement entry = entries.get(key);
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject entryObj = entry.getAsJsonObject();
            if (entryObj.has("url")) {
                matchUrl(entryObj.get("url").getAsString(), found);
            }
        }
    }

    static void matchUrl(String url, Set<DetectedClient> found) {
        if (url == null || url.isBlank()) {
            return;
        }
        String lower = url.toLowerCase(Locale.ROOT);

        if (isVanillaTextureHost(lower)) {
            return;
        }

        if (TLauncherDetector.isTlauncherTextureUrl(lower)) {
            found.add(DetectedClient.TLAUNCHER);
        }
    }

    private static boolean isVanillaTextureHost(String lower) {
        return lower.contains("textures.minecraft.net")
                || lower.contains("texture.minecraft.net")
                || lower.startsWith("minecraft:")
                || lower.contains("/avatar/");
    }
}
