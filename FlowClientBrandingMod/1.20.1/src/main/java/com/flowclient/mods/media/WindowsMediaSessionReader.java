package com.flowclient.mods.media;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class WindowsMediaSessionReader {
    private static final long TIMEOUT_SECONDS = 6L;

    private WindowsMediaSessionReader() {
    }

    static boolean isSupported() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("win");
    }

    static MediaSessionState read(AtomicReference<MediaProbeStatus> probeStatus) {
        if (!isSupported()) {
            probeStatus.set(MediaProbeStatus.MISSING);
            return MediaSessionState.INACTIVE;
        }

        Path probe = MediaProbeInstaller.ensureInstalled();
        if (probe == null) {
            probeStatus.set(MediaProbeStatus.MISSING);
            return MediaSessionState.INACTIVE;
        }

        probeStatus.set(MediaProbeStatus.READY);

        try {
            ProcessBuilder builder = new ProcessBuilder(probe.toAbsolutePath().toString());
            builder.directory(probe.getParent().toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().reduce((first, second) -> second).orElse("");
            }

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                probeStatus.set(MediaProbeStatus.MISSING);
                return MediaSessionState.INACTIVE;
            }

            if (process.exitValue() != 0 || output.isBlank()) {
                probeStatus.set(MediaProbeStatus.MISSING);
                return MediaSessionState.INACTIVE;
            }

            MediaSessionState state = parse(output);
            probeStatus.set(state.active() ? MediaProbeStatus.ACTIVE : MediaProbeStatus.NO_SESSION);
            return state;
        } catch (Exception ignored) {
            probeStatus.set(MediaProbeStatus.MISSING);
            return MediaSessionState.INACTIVE;
        }
    }

    private static MediaSessionState parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("active") || !root.get("active").getAsBoolean()) {
            return MediaSessionState.INACTIVE;
        }

        long now = System.currentTimeMillis();
        return new MediaSessionState(
                true,
                text(root, "title"),
                text(root, "artist"),
                text(root, "album"),
                text(root, "trackId"),
                text(root, "artPath"),
                root.has("position") ? root.get("position").getAsDouble() : 0.0,
                root.has("duration") ? root.get("duration").getAsDouble() : 0.0,
                !root.has("playing") || root.get("playing").getAsBoolean(),
                text(root, "app"),
                now
        );
    }

    private static String text(JsonObject root, String key) {
        if (!root.has(key) || root.get(key).isJsonNull()) {
            return "";
        }
        return root.get(key).getAsString();
    }
}
