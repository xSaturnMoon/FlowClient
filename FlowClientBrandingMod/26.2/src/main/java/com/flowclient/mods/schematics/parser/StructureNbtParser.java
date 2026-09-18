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
        ListTag sizeList = root.getListOrEmpty("size");
        if (sizeList.size() < 3) {
            return Schematic.empty();
        }
        int width = sizeList.getIntOr(0, 0);
        int height = sizeList.getIntOr(1, 0);
        int length = sizeList.getIntOr(2, 0);
        if (width <= 0 || height <= 0 || length <= 0) {
            return Schematic.empty();
        }

        ListTag paletteList = root.getListOrEmpty("palette");
        Map<Integer, BlockState> palette = new HashMap<>();
        for (int i = 0; i < paletteList.size(); i++) {
            CompoundTag entry = paletteList.getCompoundOrEmpty(i);
            palette.put(i, BlockStateReader.fromPaletteEntry(entry));
        }

        ListTag blocks = root.getListOrEmpty("blocks");
        Map<BlockPos, BlockState> map = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompoundOrEmpty(i);
            ListTag posList = block.getListOrEmpty("pos");
            if (posList.size() < 3) {
                continue;
            }
            int stateId = block.getIntOr("state", 0);
            BlockState state = palette.getOrDefault(stateId, Blocks.AIR.defaultBlockState());
            if (state.isAir()) {
                continue;
            }
            map.put(new BlockPos(
                    posList.getIntOr(0, 0),
                    posList.getIntOr(1, 0),
                    posList.getIntOr(2, 0)
            ), state);
        }
        return new Schematic(width, height, length, map);
    }
}
