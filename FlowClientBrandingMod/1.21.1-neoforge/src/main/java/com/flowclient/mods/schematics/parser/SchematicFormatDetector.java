package com.flowclient.mods.schematics.parser;

import net.minecraft.nbt.CompoundTag;

final class SchematicFormatDetector {
    private SchematicFormatDetector() {}

    static boolean isSpongeFormat(CompoundTag root) {
        CompoundTag schematic = SchematicNbt.resolveSchematicTag(root);
        if (schematic.isEmpty()) {
            return false;
        }

        if (schematic.contains("Version")
                || schematic.contains("DataVersion")
                || schematic.contains("Palette")
                || schematic.contains("BlockData")) {
            return true;
        }

        CompoundTag blocks = NbtCompat.getCompoundOrEmpty(schematic, "Blocks");
        return !blocks.isEmpty() && (blocks.contains("Palette") || blocks.contains("Data"));
    }

    static boolean isAlphaFormat(CompoundTag root) {
        CompoundTag schematic = SchematicNbt.resolveSchematicTag(root);
        if (schematic.isEmpty() || isSpongeFormat(root)) {
            return false;
        }

        byte[] blocks = NbtCompat.getByteArrayOrEmpty(schematic, "Blocks");
        if (blocks.length > 0) {
            return true;
        }

        return NbtCompat.getIntArrayOrEmpty(schematic, "Blocks").length > 0;
    }
}
