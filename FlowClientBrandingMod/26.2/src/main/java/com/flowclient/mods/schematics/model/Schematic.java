package com.flowclient.mods.schematics.model;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Schematic {
    private final int width;
    private final int height;
    private final int length;
    private final Map<BlockPos, BlockState> blocks;

    public Schematic(int width, int height, int length, Map<BlockPos, BlockState> blocks) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.blocks = Collections.unmodifiableMap(blocks);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public Map<BlockPos, BlockState> getBlocks() {
        return blocks;
    }

    public int blockCount() {
        return blocks.size();
    }

    public static Schematic empty() {
        return new Schematic(0, 0, 0, Map.of());
    }

    public static Builder builder(int width, int height, int length) {
        return new Builder(width, height, length);
    }

    public static final class Builder {
        private final int width;
        private final int height;
        private final int length;
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();

        private Builder(int width, int height, int length) {
            this.width = width;
            this.height = height;
            this.length = length;
        }

        public void put(int x, int y, int z, BlockState state) {
            if (state == null || state.isAir()) {
                return;
            }
            blocks.put(new BlockPos(x, y, z), state);
        }

        public Schematic build() {
            return new Schematic(width, height, length, blocks);
        }
    }
}
