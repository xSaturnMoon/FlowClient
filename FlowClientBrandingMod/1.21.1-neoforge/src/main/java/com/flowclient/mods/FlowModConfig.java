package com.flowclient.mods;

import com.flowclient.FlowClientMod;
import com.flowclient.mods.schematics.AllSchematicsMod;
import com.flowclient.mods.zoom.ZoomifySettings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FlowModConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(FlowClientMod.MOD_ID + "-mods.json");

    private FlowModConfig() {}

    public static void load() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            NametagMod.setEnabled(GsonHelper.getAsBoolean(json, "nametag", false), false);
            ModifyF3Mod.setEnabled(GsonHelper.getAsBoolean(json, "modifyF3", false), false);
            ZoomifyMod.setEnabled(GsonHelper.getAsBoolean(json, "zoomify", false), false);
            FreelookMod.setEnabled(GsonHelper.getAsBoolean(json, "freelook", false), false);
            AllSchematicsMod.setEnabled(GsonHelper.getAsBoolean(json, "allSchematics", false), false);
            ZoomifySettings.load(json);
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("nametag", NametagMod.isEnabled());
        json.addProperty("modifyF3", ModifyF3Mod.isEnabled());
        json.addProperty("zoomify", ZoomifyMod.isEnabled());
        json.addProperty("freelook", FreelookMod.isEnabled());
        json.addProperty("allSchematics", AllSchematicsMod.isEnabled());
        ZoomifySettings.save(json);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GsonHelper.toStableString(json));
        } catch (IOException ignored) {
        }
    }
}
