package com.flowclient.mods.schematics.parser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class LegacyBlockMapper {
    private static final Map<String, BlockState> LOOKUP = new HashMap<>();
    private static boolean loaded;

    private LegacyBlockMapper() {}

    static BlockState state(int id, int data) {
        ensureLoaded();

        BlockState exact = LOOKUP.get(key(id, data));
        if (exact != null) {
            return exact;
        }
        if (data != 0) {
            BlockState fallback = LOOKUP.get(key(id, 0));
            if (fallback != null) {
                return fallback;
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static String key(int id, int data) {
        return id + ":" + data;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;

        try (InputStream input = LegacyBlockMapper.class.getResourceAsStream("legacy.json")) {
            if (input == null) {
                return;
            }

            Type type = new TypeToken<LegacyDataFile>() {}.getType();
            LegacyDataFile dataFile = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), type);
            if (dataFile == null || dataFile.blocks == null) {
                return;
            }

            for (Map.Entry<String, String> entry : dataFile.blocks.entrySet()) {
                BlockState state = BlockStateReader.parse(entry.getValue());
                if (!state.isAir() || "minecraft:air".equals(entry.getValue())) {
                    LOOKUP.put(entry.getKey(), state);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static final class LegacyDataFile {
        private Map<String, String> blocks;
    }
}
