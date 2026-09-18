package com.flowclient.mods.schematics.parser;

import com.flowclient.mods.schematics.model.Schematic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

final class StructureNbtParser {
    private StructureNbtParser() {}

    static Schematic parse(CompoundTag root) {
        if (!root.contains("size")) {
            return Schematic.empty();
        }
        return parseVanillaStructure(root);
    }

    private static Schematic parseVanillaStructure(CompoundTag root) {
        ListTag sizeList = NbtCompat.getListOrEmpty(root, "size");
        if (sizeList.size() < 3) {
            return Schematic.empty();
        }
        int width = NbtCompat.getIntOr(sizeList, 0, 0);
        int height = NbtCompat.getIntOr(sizeList, 1, 0);
        int length = NbtCompat.getIntOr(sizeList, 2, 0);
        if (width <= 0 || height <= 0 || length <= 0) {
            return Schematic.empty();
        }

        ListTag paletteList = NbtCompat.getListOrEmpty(root, "palette");
        Map<Integer, BlockState> palette = new HashMap<>();
        for (int i = 0; i < paletteList.size(); i++) {
            CompoundTag entry = NbtCompat.getCompoundOrEmpty(paletteList, i);
            palette.put(i, BlockStateReader.fromPaletteEntry(entry));
        }

        ListTag blocks = NbtCompat.getListOrEmpty(root, "blocks");
        Map<BlockPos, BlockState> map = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = NbtCompat.getCompoundOrEmpty(blocks, i);
            ListTag posList = NbtCompat.getListOrEmpty(block, "pos");
            if (posList.size() < 3) {
                continue;
            }
            int stateId = NbtCompat.getIntOr(block, "state", 0);
            BlockState state = palette.getOrDefault(stateId, Blocks.AIR.defaultBlockState());
            if (state.isAir()) {
                continue;
            }
            map.put(new BlockPos(
                    NbtCompat.getIntOr(posList, 0, 0),
                    NbtCompat.getIntOr(posList, 1, 0),
                    NbtCompat.getIntOr(posList, 2, 0)
            ), state);
        }
        return new Schematic(width, height, length, map);
    }
}
