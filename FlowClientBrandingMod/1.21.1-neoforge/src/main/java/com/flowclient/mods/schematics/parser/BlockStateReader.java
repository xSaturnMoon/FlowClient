package com.flowclient.mods.schematics.parser;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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

        try {
            return BlockStateParser.parseForBlock(resolveBlockLookup(), stateString, false).blockState();
        } catch (Exception ignored) {
        }

        int bracket = stateString.indexOf('[');
        String blockId = bracket > 0 ? stateString.substring(0, bracket) : stateString;
        ResourceLocation identifier = ResourceLocation.tryParse(blockId);
        if (identifier == null) {
            return Blocks.AIR.defaultBlockState();
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(identifier).orElse(Blocks.AIR);
        return block.defaultBlockState();
    }

    static BlockState fromPaletteEntry(CompoundTag entry) {
        String name = NbtCompat.getStringOr(entry, "Name", "minecraft:air");
        CompoundTag properties = NbtCompat.getCompoundOrEmpty(entry, "Properties");
        if (properties.isEmpty()) {
            return parse(name);
        }

        StringBuilder state = new StringBuilder(name).append('[');
        boolean first = true;
        for (String key : NbtCompat.keySet(properties)) {
            if (!first) {
                state.append(',');
            }
            state.append(key).append('=').append(NbtCompat.getStringOr(properties, key, ""));
            first = false;
        }
        state.append(']');
        return parse(state.toString());
    }

    private static HolderLookup<Block> resolveBlockLookup() {
        Minecraft client = Minecraft.getInstance();
        RegistryAccess access = client.level != null ? client.level.registryAccess() : RegistryAccess.EMPTY;
        return access.lookupOrThrow(Registries.BLOCK);
    }
}
