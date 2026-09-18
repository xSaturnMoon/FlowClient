package com.flowclient.mods.voicechat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Ensures the official Simple Voice Chat jar is present in the instance mods folder.
 * The jar stays installed; FlowClient toggles voice chat at runtime instead of removing it.
 */
public final class VoiceChatJarManager {
    private static final String PROJECT_ID = "simple-voice-chat";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private VoiceChatJarManager() {}

    public static void ensureInstalled() throws Exception {
        if (FabricLoader.getInstance().isModLoaded("voicechat")) {
            return;
        }

        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        Files.createDirectories(modsDir);

        if (hasVoiceChatJar(modsDir)) {
            return;
        }

        Path runtimeJar = resolveRuntimeJar()
                .orElseGet(() -> {
                    try {
                        return downloadRuntimeJar();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        Files.copy(runtimeJar, modsDir.resolve(runtimeJar.getFileName()),
                StandardCopyOption.REPLACE_EXISTING);
    }

    public static boolean isInstalledInModsFolder() {
        try {
            return hasVoiceChatJar(FabricLoader.getInstance().getGameDir().resolve("mods"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean hasVoiceChatJar(Path modsDir) throws Exception {
        if (!Files.isDirectory(modsDir)) {
            return false;
        }

        try (var stream = Files.list(modsDir)) {
            return stream.anyMatch(path -> isVoiceChatJar(path.getFileName().toString()));
        }
    }

    private static Optional<Path> resolveRuntimeJar() throws Exception {
        String mcVersion = SharedConstants.getCurrentVersion().getName();
        Path versionDir = runtimeDir().resolve(mcVersion);
        if (!Files.isDirectory(versionDir)) {
            return Optional.empty();
        }

        try (var stream = Files.list(versionDir)) {
            return stream
                    .filter(path -> isVoiceChatJar(path.getFileName().toString()))
                    .findFirst();
        }
    }

    private static Path downloadRuntimeJar() throws Exception {
        String mcVersion = SharedConstants.getCurrentVersion().getName();
        String gameVersions = URLEncoder.encode("[\"" + mcVersion + "\"]", StandardCharsets.UTF_8);
        String loaders = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
        String query = "project/" + PROJECT_ID + "/version?game_versions=" + gameVersions + "&loaders=" + loaders;

        HttpRequest versionRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/" + query))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "FlowClient/1.0")
                .GET()
                .build();

        HttpResponse<String> versionResponse = HTTP.send(versionRequest, HttpResponse.BodyHandlers.ofString());
        if (versionResponse.statusCode() != 200) {
            throw new IllegalStateException("Simple Voice Chat is not available for Minecraft " + mcVersion);
        }

        JsonArray versions = JsonParser.parseString(versionResponse.body()).getAsJsonArray();
        if (versions.isEmpty()) {
            throw new IllegalStateException("No Simple Voice Chat release found for Minecraft " + mcVersion);
        }

        JsonElement version = versions.get(0).getAsJsonObject();
        JsonArray files = version.getAsJsonObject().getAsJsonArray("files");
        JsonElement file = StreamSupport.stream(files.spliterator(), false)
                .filter(el -> el.getAsJsonObject().has("primary")
                        && el.getAsJsonObject().get("primary").getAsBoolean())
                .findFirst()
                .orElse(files.get(0));

        String downloadUrl = file.getAsJsonObject().get("url").getAsString();
        String filename = file.getAsJsonObject().get("filename").getAsString();

        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .timeout(Duration.ofSeconds(120))
                .header("User-Agent", "FlowClient/1.0")
                .GET()
                .build();

        HttpResponse<InputStream> downloadResponse =
                HTTP.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (downloadResponse.statusCode() != 200) {
            throw new IllegalStateException("Failed to download Simple Voice Chat");
        }

        Path versionDir = runtimeDir().resolve(mcVersion);
        Files.createDirectories(versionDir);
        Path target = versionDir.resolve(filename);

        try (InputStream stream = downloadResponse.body()) {
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return target;
    }

    private static Path runtimeDir() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            appData = System.getProperty("user.home");
        }
        return Path.of(appData, "FlowLauncher", "runtime", "voicechat");
    }

    private static boolean isVoiceChatJar(String filename) {
        String name = filename.toLowerCase(Locale.ROOT);
        return name.startsWith("voicechat") && name.endsWith(".jar");
    }
}
