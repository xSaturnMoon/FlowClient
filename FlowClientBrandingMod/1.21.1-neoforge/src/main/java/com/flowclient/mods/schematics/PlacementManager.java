package com.flowclient.mods.schematics;

import com.flowclient.mods.schematics.model.PlacementTransform;
import com.flowclient.mods.schematics.model.Schematic;
import com.flowclient.mods.schematics.parser.SchematicLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import java.io.IOException;

public final class PlacementManager {
    private static SchematicEntry loadedEntry;
    private static Schematic loadedSchematic;
    private static BlockPos anchor = BlockPos.ZERO;
    private static final PlacementTransform transform = new PlacementTransform();

    private PlacementManager() {}

    public static boolean hasPlacement() {
        return loadedSchematic != null && loadedSchematic.blockCount() > 0;
    }

    public static Schematic getSchematic() {
        return loadedSchematic;
    }

    public static SchematicEntry getEntry() {
        return loadedEntry;
    }

    public static BlockPos getAnchor() {
        return anchor;
    }

    public static void setAnchor(BlockPos anchor) {
        PlacementManager.anchor = anchor == null ? BlockPos.ZERO : anchor;
    }

    public static PlacementTransform getTransform() {
        return transform;
    }

    public static void load(SchematicEntry entry) throws IOException {
        loadedEntry = entry;
        loadedSchematic = SchematicLoader.load(entry.getFile());
        if (loadedSchematic.blockCount() == 0) {
            loadedEntry = null;
            loadedSchematic = null;
            throw new IOException("No blocks found in schematic file.");
        }
        anchor = defaultAnchor();
        transform.setOffsetX(0);
        transform.setOffsetY(0);
        transform.setOffsetZ(0);
        transform.setRotation(Rotation.NONE);
        transform.setMirror(Mirror.NONE);
    }

    public static void clear() {
        loadedEntry = null;
        loadedSchematic = null;
        anchor = BlockPos.ZERO;
    }

    public static BlockPos toWorldPos(BlockPos local) {
        if (!hasPlacement()) {
            return local;
        }
        BlockPos transformed = transform.transformLocal(local, loadedSchematic.getWidth(), loadedSchematic.getLength());
        return anchor.offset(transformed);
    }

    public static void nudge(int dx, int dy, int dz) {
        transform.setOffsetX(transform.getOffsetX() + dx);
        transform.setOffsetY(transform.getOffsetY() + dy);
        transform.setOffsetZ(transform.getOffsetZ() + dz);
    }

    private static BlockPos defaultAnchor() {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) {
            return BlockPos.ZERO;
        }
        return player.blockPosition();
    }
}
