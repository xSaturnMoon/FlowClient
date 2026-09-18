package com.flowclient.mods.schematics.parser;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/**
 * Iterates VarInt values encoded in a byte array (Sponge schematic block data).
 */
final class VarIntIterator implements PrimitiveIterator.OfInt {
    private final byte[] source;
    private int index;
    private boolean hasNextInt;
    private int nextInt;

    VarIntIterator(byte[] source) {
        this.source = source;
    }

    @Override
    public boolean hasNext() {
        if (hasNextInt) {
            return true;
        }
        if (index >= source.length) {
            return false;
        }

        nextInt = readNextInt();
        hasNextInt = true;
        return true;
    }

    private int readNextInt() {
        int value = 0;
        for (int bitsRead = 0; ; bitsRead += 7) {
            if (index >= source.length) {
                throw new IllegalStateException("Ran out of bytes while reading VarInt");
            }
            byte next = source[index++];
            value |= (next & 0x7F) << bitsRead;
            if (bitsRead > 7 * 5) {
                throw new IllegalStateException("VarInt too big");
            }
            if ((next & 0x80) == 0) {
                break;
            }
        }
        return value;
    }

    @Override
    public int nextInt() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        hasNextInt = false;
        return nextInt;
    }
}
