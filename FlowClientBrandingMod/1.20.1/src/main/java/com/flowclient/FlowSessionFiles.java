package com.flowclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class FlowSessionFiles {
    public static final String SESSION_FILE = ".flowclient-session.json";

    private FlowSessionFiles() {}

    public static void register() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> writeCurrentProcess());
    }

    public static void deleteSessionFile() {
        try {
            Files.deleteIfExists(sessionPath());
        } catch (Exception ignored) {
        }
    }

    private static void writeCurrentProcess() {
        try {
            long pid = ProcessHandle.current().pid();
            Path path = sessionPath();
            Files.createDirectories(path.getParent());

            List<Integer> pids = new ArrayList<>();
            pids.add((int) pid);

            if (Files.isRegularFile(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("pids") && json.get("pids").isJsonArray()) {
                        json.getAsJsonArray("pids").forEach(element -> {
                            try {
                                int existing = element.getAsInt();
                                if (!pids.contains(existing)) {
                                    pids.add(existing);
                                }
                            } catch (Exception ignored) {
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }

            JsonObject json = new JsonObject();
            json.addProperty("gameDirectory", path.getParent().toString());
            json.addProperty("rootPid", pid);
            json.addProperty("startedAtUtc", Instant.now().toString());
            var pidArray = new com.google.gson.JsonArray();
            for (int value : pids) {
                pidArray.add(value);
            }
            json.add("pids", pidArray);

            Files.writeString(path, GsonHelper.toStableString(json));
        } catch (Exception ignored) {
        }
    }

    private static Path sessionPath() {
        return FabricLoader.getInstance().getGameDir().resolve(SESSION_FILE);
    }
}
