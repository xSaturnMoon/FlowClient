package com.flowclient.mods.schematics.parser;

import com.flowclient.mods.schematics.model.Schematic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

final class LitematicParser {
    private LitematicParser() {}

    static Schematic parse(CompoundTag root) {
        CompoundTag regions = NbtCompat.getCompoundOrEmpty(root, "Regions");
        if (regions.isEmpty()) {
            return Schematic.empty();
        }

        String firstKey = NbtCompat.keySet(regions).stream().sorted().findFirst().orElse(null);
        if (firstKey == null) {
            return Schematic.empty();
        }

        CompoundTag region = NbtCompat.getCompoundOrEmpty(regions, firstKey);
        CompoundTag sizeTag = NbtCompat.getCompoundOrEmpty(region, "Size");
        int width = SchematicNbt.readDimension(sizeTag, "x");
        int height = SchematicNbt.readDimension(sizeTag, "y");
        int length = SchematicNbt.readDimension(sizeTag, "z");
        if (width <= 0 || height <= 0 || length <= 0) {
            return Schematic.empty();
        }

        ListTag palette = NbtCompat.getListOrEmpty(region, "BlockStatePalette");
        Map<Integer, BlockState> paletteMap = new HashMap<>();
        for (int i = 0; i < palette.size(); i++) {
            paletteMap.put(i, BlockStateReader.fromPaletteEntry(NbtCompat.getCompoundOrEmpty(palette, i)));
        }
        paletteMap.putIfAbsent(0, Blocks.AIR.defaultBlockState());

        long[] states = NbtCompat.getLongArrayOrEmpty(region, "BlockStates");
        int totalBlocks = width * height * length;
        byte[] blockData = decodeLitematicStates(states, paletteMap.size(), totalBlocks);

        Map<BlockPos, BlockState> blocks = SchematicLoader.mapFromPalette(width, height, length, paletteMap, blockData);
        return new Schematic(width, height, length, blocks);
    }

    private static byte[] decodeLitematicStates(long[] data, int paletteSize, int totalBlocks) {
        if (paletteSize <= 1 || data.length == 0) {
            return new byte[totalBlocks];
        }

        int bitsPerEntry = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
        long mask = (1L << bitsPerEntry) - 1L;
        byte[] out = new byte[totalBlocks];

        for (int blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
            int bitStart = blockIndex * bitsPerEntry;
            int longIndex = bitStart / 64;
            int bitOffset = bitStart % 64;

            long value;
            if (longIndex >= data.length) {
                value = 0L;
            } else if (bitOffset + bitsPerEntry <= 64) {
                value = (data[longIndex] >>> bitOffset) & mask;
            } else {
                int bitsInFirst = 64 - bitOffset;
                int bitsInSecond = bitsPerEntry - bitsInFirst;
                long lowMask = (1L << bitsInFirst) - 1L;
                long highMask = (1L << bitsInSecond) - 1L;
                long low = (data[longIndex] >>> bitOffset) & lowMask;
                long high = longIndex + 1 < data.length ? data[longIndex + 1] & highMask : 0L;
                value = low | (high << bitsInFirst);
            }

            out[blockIndex] = (byte) value;
        }

        return out;
    }
}
