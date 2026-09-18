package com.flowclient.mods.schematics.model;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public final class PlacementTransform {
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private Rotation rotation = Rotation.NONE;
    private Mirror mirror = Mirror.NONE;

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public int getOffsetZ() {
        return offsetZ;
    }

    public void setOffsetZ(int offsetZ) {
        this.offsetZ = offsetZ;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation == null ? Rotation.NONE : rotation;
    }

    public Mirror getMirror() {
        return mirror;
    }

    public void setMirror(Mirror mirror) {
        this.mirror = mirror == null ? Mirror.NONE : mirror;
    }

    public void rotateClockwise() {
        rotation = rotation.getRotated(Rotation.CLOCKWISE_90);
    }

    public void rotateCounterClockwise() {
        rotation = rotation.getRotated(Rotation.COUNTERCLOCKWISE_90);
    }

    public void toggleMirror() {
        mirror = mirror == Mirror.NONE ? Mirror.FRONT_BACK : Mirror.NONE;
    }

    public BlockPos transformLocal(BlockPos local, int width, int length) {
        int x = local.getX();
        int y = local.getY();
        int z = local.getZ();

        if (mirror != Mirror.NONE) {
            z = mirror.mirror(z, length);
        }

        int rotatedX = x;
        int rotatedZ = z;
        switch (rotation) {
            case CLOCKWISE_90 -> {
                rotatedX = length - 1 - z;
                rotatedZ = x;
            }
            case CLOCKWISE_180 -> {
                rotatedX = width - 1 - x;
                rotatedZ = length - 1 - z;
            }
            case COUNTERCLOCKWISE_90 -> {
                rotatedX = z;
                rotatedZ = width - 1 - x;
            }
            default -> {
            }
        }

        return new BlockPos(rotatedX + offsetX, y + offsetY, rotatedZ + offsetZ);
    }
}
