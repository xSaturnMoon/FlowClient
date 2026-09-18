package com.flowclient.mods.schematics.parser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Set;

final class NbtCompat {
    private NbtCompat() {}

    static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : new CompoundTag();
    }

    static CompoundTag getCompoundOrEmpty(ListTag list, int index) {
        if (index < 0 || index >= list.size()) {
            return new CompoundTag();
        }
        Tag element = list.get(index);
        return element instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    static ListTag getListOrEmpty(CompoundTag tag, String key) {
        Tag value = tag.get(key);
        return value instanceof ListTag list ? list : new ListTag();
    }

    static String getStringOr(CompoundTag tag, String key, String defaultValue) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : defaultValue;
    }

    static int getIntOr(CompoundTag tag, String key, int defaultValue) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : defaultValue;
    }

    static short getShortOr(CompoundTag tag, String key, short defaultValue) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getShort(key) : defaultValue;
    }

    static int getIntOr(ListTag list, int index, int defaultValue) {
        if (index < 0 || index >= list.size()) {
            return defaultValue;
        }
        Tag element = list.get(index);
        return element instanceof NumericTag numeric ? numeric.getAsInt() : defaultValue;
    }

    static int[] getIntArrayOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_INT_ARRAY) ? tag.getIntArray(key) : new int[0];
    }

    static byte[] getByteArrayOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_BYTE_ARRAY) ? tag.getByteArray(key) : new byte[0];
    }

    static long[] getLongArrayOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LONG_ARRAY) ? tag.getLongArray(key) : new long[0];
    }

    static Set<String> keySet(CompoundTag tag) {
        return tag.getAllKeys();
    }

    static String stringValue(StringTag tag) {
        return tag.getAsString();
    }
}
