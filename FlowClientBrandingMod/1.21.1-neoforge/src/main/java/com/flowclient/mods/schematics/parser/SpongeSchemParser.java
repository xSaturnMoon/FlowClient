package com.flowclient.mods.schematics.parser;

import com.flowclient.mods.schematics.model.Schematic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class SpongeSchemParser {
    private SpongeSchemParser() {}

    static Schematic parse(CompoundTag root) {
        CompoundTag schematic = SchematicNbt.resolveSchematicTag(root);

        int width = SchematicNbt.readDimension(schematic, "Width");
        int height = SchematicNbt.readDimension(schematic, "Height");
        int length = SchematicNbt.readDimension(schematic, "Length");
        if (width <= 0 || height <= 0 || length <= 0) {
            return Schematic.empty();
        }

        CompoundTag blockSection = resolveBlockSection(schematic);
        CompoundTag paletteTag = NbtCompat.getCompoundOrEmpty(blockSection, "Palette");
        if (paletteTag.isEmpty()) {
            paletteTag = NbtCompat.getCompoundOrEmpty(schematic, "Palette");
        }

        Map<Integer, BlockState> palette = SchematicLoader.paletteFromSponge(paletteTag);
        if (palette.isEmpty()) {
            return Schematic.empty();
        }

        int expected = width * height * length;
        byte[] blockBytes = readBlockBytes(blockSection, schematic);
        if (blockBytes.length == 0) {
            return Schematic.empty();
        }

        int[] blockData = decodeBlockIndices(blockBytes, expected);
        if (blockData.length == 0) {
            return Schematic.empty();
        }

        Map<BlockPos, BlockState> blocks = SchematicLoader.mapFromPaletteInts(
                width, height, length, palette, blockData
        );
        return new Schematic(width, height, length, blocks);
    }

    private static CompoundTag resolveBlockSection(CompoundTag schematic) {
        if (schematic.contains("Blocks")) {
            CompoundTag blocks = NbtCompat.getCompoundOrEmpty(schematic, "Blocks");
            if (!blocks.isEmpty()) {
                return blocks;
            }
        }
        return schematic;
    }

    private static byte[] readBlockBytes(CompoundTag blockSection, CompoundTag schematic) {
        byte[] data = NbtCompat.getByteArrayOrEmpty(blockSection, "Data");
        if (data.length > 0) {
            return data;
        }

        data = NbtCompat.getByteArrayOrEmpty(blockSection, "BlockData");
        if (data.length > 0) {
            return data;
        }

        return NbtCompat.getByteArrayOrEmpty(schematic, "BlockData");
    }

    private static int[] decodeBlockIndices(byte[] bytes, int expected) {
        List<Integer> values = new ArrayList<>(Math.min(expected, bytes.length));
        VarIntIterator iterator = new VarIntIterator(bytes);
        while (iterator.hasNext() && values.size() < expected) {
            values.add(iterator.nextInt());
        }

        if (values.isEmpty()) {
            return new int[0];
        }

        int[] out = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }
}
