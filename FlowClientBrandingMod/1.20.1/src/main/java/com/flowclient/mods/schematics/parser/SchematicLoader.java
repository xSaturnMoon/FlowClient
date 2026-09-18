package com.flowclient.mods.schematics.parser;

import com.flowclient.compat.NbtCompat;

import com.flowclient.mods.schematics.model.Schematic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SchematicLoader {
    private SchematicLoader() {}

    public static Schematic load(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        CompoundTag root = SchematicNbt.readRoot(file);

        if (name.endsWith(".litematic")) {
            return LitematicParser.parse(root);
        }
        if (name.endsWith(".schematic")) {
            if (SchematicFormatDetector.isSpongeFormat(root)) {
                Schematic sponge = SpongeSchemParser.parse(root);
                if (sponge.blockCount() > 0) {
                    return sponge;
                }
            }
            if (SchematicFormatDetector.isAlphaFormat(root)) {
                Schematic classic = ClassicSchematicParser.parse(root);
                if (classic.blockCount() > 0) {
                    return classic;
                }
            }
            Schematic sponge = SpongeSchemParser.parse(root);
            if (sponge.blockCount() > 0) {
                return sponge;
            }
            return ClassicSchematicParser.parse(root);
        }
        if (name.endsWith(".schem")) {
            Schematic sponge = SpongeSchemParser.parse(root);
            if (sponge.blockCount() > 0) {
                return sponge;
            }
            return parseClassicWithoutSponge(root);
        }
        if (name.endsWith(".nbt")) {
            return StructureNbtParser.parse(root);
        }

        Schematic fromStructure = StructureNbtParser.parse(root);
        if (fromStructure.blockCount() > 0) {
            return fromStructure;
        }
        return SpongeSchemParser.parse(root);
    }

    static BlockState stateFromId(String id) {
        return BlockStateReader.parse(id);
    }

    static Map<BlockPos, BlockState> mapFromPalette(
            int width, int height, int length,
            Map<Integer, BlockState> palette,
            byte[] blockData
    ) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    if (index >= blockData.length) {
                        return blocks;
                    }
                    int paletteId = blockData[index++] & 0xFF;
                    BlockState state = palette.getOrDefault(paletteId, Blocks.AIR.defaultBlockState());
                    if (!state.isAir()) {
                        blocks.put(new BlockPos(x, y, z), state);
                    }
                }
            }
        }
        return blocks;
    }

    static Map<BlockPos, BlockState> mapFromPaletteInts(
            int width, int height, int length,
            Map<Integer, BlockState> palette,
            int[] blockData
    ) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    if (index >= blockData.length) {
                        return blocks;
                    }
                    int paletteId = blockData[index++];
                    BlockState state = palette.getOrDefault(paletteId, Blocks.AIR.defaultBlockState());
                    if (!state.isAir()) {
                        blocks.put(new BlockPos(x, y, z), state);
                    }
                }
            }
        }
        return blocks;
    }

    static Map<Integer, BlockState> paletteFromSponge(CompoundTag paletteTag) {
        Map<Integer, BlockState> palette = new HashMap<>();
        if (paletteTag.isEmpty()) {
            return palette;
        }

        boolean inverted = NbtCompat.keySet(paletteTag).stream().allMatch(SchematicLoader::isNumericPaletteKey);
        if (inverted) {
            for (String key : NbtCompat.keySet(paletteTag)) {
                try {
                    int id = Integer.parseInt(key);
                    String blockId = readPaletteBlockName(paletteTag.get(key));
                    if (blockId != null) {
                        palette.put(id, BlockStateReader.parse(blockId));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } else {
            for (String blockId : NbtCompat.keySet(paletteTag)) {
                Integer id = readPaletteId(paletteTag.get(blockId));
                if (id != null && id >= 0) {
                    palette.put(id, BlockStateReader.parse(blockId));
                }
            }
        }

        return palette;
    }

    private static Schematic parseClassicWithoutSponge(CompoundTag root) {
        CompoundTag schematic = SchematicNbt.resolveSchematicTag(root);
        if (schematic.contains("Palette") || schematic.contains("BlockData") || schematic.contains("Blocks")) {
            return Schematic.empty();
        }
        return ClassicSchematicParser.parse(root);
    }

    private static Integer readPaletteId(Tag tag) {
        if (tag instanceof NumericTag numeric) {
            return NbtCompat.intValue(numeric);
        }
        if (tag instanceof StringTag stringTag) {
            try {
                return Integer.parseInt(NbtCompat.stringValue(stringTag));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static String readPaletteBlockName(Tag tag) {
        if (tag instanceof StringTag stringTag) {
            return NbtCompat.stringValue(stringTag);
        }
        if (tag instanceof CompoundTag compound) {
            String name = NbtCompat.getStringOr(compound, "Name", "");
            if (!name.isBlank()) {
                return name;
            }
        }
        return null;
    }

    private static boolean isNumericPaletteKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        for (int i = 0; i < key.length(); i++) {
            if (!Character.isDigit(key.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
