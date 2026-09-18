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

        CompoundTag blocks = schematic.getCompoundOrEmpty("Blocks");
        return !blocks.isEmpty() && (blocks.contains("Palette") || blocks.contains("Data"));
    }

    static boolean isAlphaFormat(CompoundTag root) {
        CompoundTag schematic = SchematicNbt.resolveSchematicTag(root);
        if (schematic.isEmpty() || isSpongeFormat(root)) {
            return false;
        }

        byte[] blocks = schematic.getByteArray("Blocks").orElse(new byte[0]);
        if (blocks.length > 0) {
            return true;
        }

        return schematic.getIntArray("Blocks").orElse(new int[0]).length > 0;
    }
}
