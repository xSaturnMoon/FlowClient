package com.flowclient.mods.schematics.parser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class BlockStateReader {
    private BlockStateReader() {}

    static BlockState parse(String stateString) {
        if (stateString == null || stateString.isBlank()) {
            return Blocks.AIR.defaultBlockState();
        }

        int bracket = stateString.indexOf('[');
        String blockId = bracket > 0 ? stateString.substring(0, bracket) : stateString;
        ResourceLocation id = ResourceLocation.tryParse(blockId);
        if (id == null) {
            return Blocks.AIR.defaultBlockState();
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
        return block.defaultBlockState();
    }

    static BlockState fromPaletteEntry(CompoundTag entry) {
        String name = entry.contains("Name", net.minecraft.nbt.Tag.TAG_STRING)
                ? entry.getString("Name")
                : "minecraft:air";
        if (!entry.contains("Properties", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return parse(name);
        }

        CompoundTag properties = entry.getCompound("Properties");
        StringBuilder state = new StringBuilder(name).append('[');
        boolean first = true;
        for (String key : properties.getAllKeys()) {
            if (!first) {
                state.append(',');
            }
            state.append(key).append('=').append(properties.getString(key));
            first = false;
        }
        state.append(']');
        return parse(state.toString());
    }
}
