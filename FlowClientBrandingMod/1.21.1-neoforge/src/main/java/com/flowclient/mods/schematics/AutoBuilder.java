package com.flowclient.mods.schematics;

import com.flowclient.mods.schematics.model.Schematic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class AutoBuilder {
    private AutoBuilder() {}

    public static void build(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.isCreative() || !PlacementManager.hasPlacement()) {
            return;
        }

        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            player.sendSystemMessage(
                    Component.literal("AutoBuild works in single-player creative worlds.")
            );
            return;
        }

        Schematic schematic = PlacementManager.getSchematic();
        ServerLevel level = server.getLevel(player.level().dimension());
        if (level == null) {
            return;
        }

        server.execute(() -> {
            int placed = 0;
            for (Map.Entry<BlockPos, BlockState> entry : schematic.getBlocks().entrySet()) {
                BlockPos worldPos = PlacementManager.toWorldPos(entry.getKey());
                level.setBlockAndUpdate(worldPos, entry.getValue());
                placed++;
            }
            int finalPlaced = placed;
            player.sendSystemMessage(
                    Component.literal("AutoBuild placed " + finalPlaced + " blocks.")
            );
        });
    }
}
