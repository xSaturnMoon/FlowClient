package com.flowclient.mods.schematics;

import com.flowclient.mods.schematics.model.Schematic;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class SchematicRenderer {
    private static final int OUTLINE_COLOR = 0xAA00D4FF;
    private static final int FILL_COLOR = 0x3300D4FF;

    private SchematicRenderer() {}

    public static void render(Minecraft client) {
        if (!AllSchematicsMod.isEnabled() || !PlacementManager.hasPlacement() || client.level == null) {
            return;
        }

        Schematic schematic = PlacementManager.getSchematic();
        Map<BlockPos, BlockState> blocks = schematic.getBlocks();
        if (blocks.isEmpty()) {
            return;
        }

        GizmoStyle style = GizmoStyle.strokeAndFill(OUTLINE_COLOR, 1.5f, FILL_COLOR);
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos worldPos = PlacementManager.toWorldPos(entry.getKey());
            Gizmos.cuboid(worldPos, style);
        }
    }
}
