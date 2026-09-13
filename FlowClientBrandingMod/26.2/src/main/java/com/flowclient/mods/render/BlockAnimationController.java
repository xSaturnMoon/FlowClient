package com.flowclient.mods.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockAnimationController {
    private BlockAnimationController() {}

    public static void onBlockPlaced(BlockPos pos, BlockState state) {
        if (!BlockAnimationMod.isEnabled()) return;
        if (state == null || state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            double distSq = client.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSq > 48.0 * 48.0) return;
        }

        spawnLandingParticles(client, pos, state);
    }

    private static void spawnLandingParticles(Minecraft client, BlockPos pos, BlockState state) {
        if (client.level == null) return;
        RandomSource random = client.level.getRandom();
        BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, state);

        for (int i = 0; i < 16; i++) {
            double px = pos.getX() + 0.12 + random.nextDouble() * 0.76;
            double py = pos.getY() + 0.04;
            double pz = pos.getZ() + 0.12 + random.nextDouble() * 0.76;

            double vx = (random.nextDouble() - 0.5) * 0.16;
            double vy = random.nextDouble() * 0.08 + 0.03;
            double vz = (random.nextDouble() - 0.5) * 0.16;

            client.level.addParticle(particleOption, px, py, pz, vx, vy, vz);
        }
    }
}