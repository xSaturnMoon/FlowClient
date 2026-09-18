package com.flowclient.mods.schematics.parser;

import com.flowclient.mods.schematics.model.Schematic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

final class ClassicSchematicParser {
    private ClassicSchematicParser() {}

    static Schematic parse(CompoundTag root) {
        CompoundTag schematic = SchematicNbt.resolveSchematicTag(root);
        if (schematic.isEmpty()) {
            return Schematic.empty();
        }

        if (SchematicFormatDetector.isSpongeFormat(root)) {
            return SpongeSchemParser.parse(root);
        }

        int width = SchematicNbt.readDimension(schematic, "Width");
        int height = SchematicNbt.readDimension(schematic, "Height");
        int length = SchematicNbt.readDimension(schematic, "Length");
        if (width <= 0 || height <= 0 || length <= 0) {
            return Schematic.empty();
        }

        int volume = width * height * length;
        int[] blockIds = readBlockIds(schematic, volume);
        int[] blockData = readBlockData(schematic, volume);
        if (blockIds.length == 0) {
            return Schematic.empty();
        }

        Map<BlockPos, BlockState> map = new HashMap<>();
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    if (index >= blockIds.length) {
                        return new Schematic(width, height, length, map);
                    }
                    int blockId = blockIds[index];
                    int meta = index < blockData.length ? blockData[index] : 0;
                    BlockState state = LegacyBlockMapper.state(blockId, meta);
                    if (!state.isAir()) {
                        map.put(new BlockPos(x, y, z), state);
                    }
                    index++;
                }
            }
        }
        return new Schematic(width, height, length, map);
    }

    private static int[] readBlockIds(CompoundTag schematic, int volume) {
        int[] fromInts = NbtCompat.getIntArrayOrEmpty(schematic, "Blocks");
        if (fromInts.length > 0) {
            return fromInts;
        }

        byte[] blocks = NbtCompat.getByteArrayOrEmpty(schematic, "Blocks");
        byte[] addBlocks = NbtCompat.getByteArrayOrEmpty(schematic, "AddBlocks");
        if (blocks.length == 0) {
            return new int[0];
        }

        int[] ids = new int[Math.min(volume, blocks.length)];
        for (int i = 0; i < ids.length; i++) {
            int id = blocks[i] & 0xFF;
            if (addBlocks.length > 0) {
                int addIndex = i >> 1;
                if (addIndex < addBlocks.length) {
                    int add = addBlocks[addIndex] & 0xFF;
                    int nibble = (i & 1) == 0 ? (add & 0x0F) : ((add >> 4) & 0x0F);
                    id |= nibble << 8;
                }
            }
            ids[i] = id;
        }
        return ids;
    }

    private static int[] readBlockData(CompoundTag schematic, int volume) {
        int[] fromInts = NbtCompat.getIntArrayOrEmpty(schematic, "Data");
        if (fromInts.length > 0) {
            return fromInts;
        }

        byte[] data = NbtCompat.getByteArrayOrEmpty(schematic, "Data");
        byte[] addData = NbtCompat.getByteArrayOrEmpty(schematic, "AddData");
        if (data.length == 0) {
            return new int[0];
        }

        int[] values = new int[Math.min(volume, data.length)];
        for (int i = 0; i < values.length; i++) {
            int meta = data[i] & 0xFF;
            if (addData.length > 0) {
                int addIndex = i >> 1;
                if (addIndex < addData.length) {
                    int add = addData[addIndex] & 0xFF;
                    int nibble = (i & 1) == 0 ? (add & 0x0F) : ((add >> 4) & 0x0F);
                    meta |= nibble << 8;
                }
            }
            values[i] = meta;
        }
        return values;
    }
}
