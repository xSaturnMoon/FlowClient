package com.flowclient.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Set;

public final class NbtCompat {
    private NbtCompat() {}

    public static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : new CompoundTag();
    }

    public static ListTag getListOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LIST) ? tag.getList(key, Tag.TAG_COMPOUND) : new ListTag();
    }

    public static ListTag getListOrEmpty(CompoundTag tag, String key, int elementType) {
        return tag.contains(key, Tag.TAG_LIST) ? tag.getList(key, elementType) : new ListTag();
    }

    public static CompoundTag getCompoundOrEmpty(ListTag list, int index) {
        if (index < 0 || index >= list.size()) {
            return new CompoundTag();
        }
        return list.getCompound(index);
    }

    public static int getIntOr(CompoundTag tag, String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    public static int getShortOr(CompoundTag tag, String key, short fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getShort(key) : fallback;
    }

    public static String getStringOr(CompoundTag tag, String key, String fallback) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }

    public static int getIntOr(ListTag list, int index, int fallback) {
        if (index < 0 || index >= list.size()) {
            return fallback;
        }
        Tag tag = list.get(index);
        return tag instanceof NumericTag numeric ? numeric.getAsInt() : fallback;
    }

    public static Set<String> keySet(CompoundTag tag) {
        return tag.getAllKeys();
    }

    public static int intValue(NumericTag tag) {
        return tag.getAsInt();
    }

    public static String stringValue(StringTag tag) {
        return tag.getAsString();
    }

    public static int[] getIntArrayOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_INT_ARRAY) ? tag.getIntArray(key) : new int[0];
    }

    public static byte[] getByteArrayOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_BYTE_ARRAY) ? tag.getByteArray(key) : new byte[0];
    }

    public static long[] getLongArrayOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LONG_ARRAY) ? tag.getLongArray(key) : new long[0];
    }
}
