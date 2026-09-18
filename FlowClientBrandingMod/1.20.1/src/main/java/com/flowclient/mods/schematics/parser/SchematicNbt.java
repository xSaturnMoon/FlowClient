package com.flowclient.mods.schematics.parser;

import com.flowclient.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

final class SchematicNbt {
    private SchematicNbt() {}

    static CompoundTag readRoot(Path file) throws IOException {
        byte[] raw = Files.readAllBytes(file);
        String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);

        if (lower.endsWith(".schem") || lower.endsWith(".litematic") || lower.endsWith(".nbt")) {
            return readPreferGzip(raw, file);
        }

        CompoundTag gzipped = tryReadGzip(raw);
        if (isUsable(gzipped)) {
            return gzipped;
        }

        CompoundTag plain = tryReadPlain(raw);
        if (isUsable(plain)) {
            return plain;
        }

        throw new IOException("Unable to read schematic NBT: " + file.getFileName());
    }

    private static CompoundTag readPreferGzip(byte[] raw, Path file) throws IOException {
        CompoundTag gzipped = tryReadGzip(raw);
        if (isUsable(gzipped)) {
            return gzipped;
        }

        CompoundTag plain = tryReadPlain(raw);
        if (isUsable(plain)) {
            return plain;
        }

        throw new IOException("Unable to read compressed schematic NBT: " + file.getFileName());
    }

    static CompoundTag resolveSchematicTag(CompoundTag root) {
        if (root.contains("Schematic")) {
            CompoundTag nested = NbtCompat.getCompoundOrEmpty(root, "Schematic");
            if (!nested.isEmpty()) {
                return nested;
            }
        }
        return root;
    }

    static int readDimension(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return 0;
        }

        int asInt = NbtCompat.getIntOr(tag, key, -1);
        if (asInt > 0) {
            return asInt;
        }

        int asShort = NbtCompat.getShortOr(tag, key, (short) -1) & 0xFFFF;
        return Math.max(asShort, 0);
    }

    private static boolean isUsable(CompoundTag root) {
        return root != null && !root.isEmpty();
    }

    private static CompoundTag tryReadPlain(byte[] raw) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(raw))) {
            return NbtIo.read(input, new NbtAccounter(0x20000000L));
        } catch (IOException ignored) {
            return null;
        }
    }

    private static CompoundTag tryReadGzip(byte[] raw) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(raw));
             DataInputStream input = new DataInputStream(gzip)) {
            return NbtIo.read(input, new NbtAccounter(0x20000000L));
        } catch (IOException ignored) {
            return null;
        }
    }
}
