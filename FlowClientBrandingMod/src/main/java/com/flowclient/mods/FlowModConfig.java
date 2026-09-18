package com.flowclient.mods;

import com.flowclient.FlowClientMod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FlowModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(FlowClientMod.MOD_ID + "-mods.json");

    private FlowModConfig() {
    }

    public static void load() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            NametagMod.setEnabled(GsonHelper.getAsBoolean(json, "nametag", false), false);
            ModifyF3Mod.setEnabled(GsonHelper.getAsBoolean(json, "modifyF3", false), false);
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("nametag", NametagMod.isEnabled());
        json.addProperty("modifyF3", ModifyF3Mod.isEnabled());

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GsonHelper.toStableString(json));
        } catch (IOException ignored) {
        }
    }
}
