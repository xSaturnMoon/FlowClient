package com.flowclient.mods.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockAnimationController {
    private static final Map<BlockPos, AnimationEntry> ANIMATIONS = new ConcurrentHashMap<>();

    private static final long FALL_DURATION_MS = 180;
    private static final long SETTLE_DURATION_MS = 100;
    private static final double START_HEIGHT = 0.45;

    private BlockAnimationController() {}

    public static void onBlockPlaced(BlockPos pos, BlockState state) {
        if (!BlockAnimationMod.isEnabled()) return;
        if (state == null || state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            double distSq = client.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSq > 48.0 * 48.0) return;
        }

        BlockPos immPos = pos.immutable();
        ANIMATIONS.put(immPos, new AnimationEntry(immPos, state, System.currentTimeMillis()));
    }

    public static boolean isAnimating(BlockPos pos) {
        if (!BlockAnimationMod.isEnabled() || ANIMATIONS.isEmpty()) return false;
        AnimationEntry entry = ANIMATIONS.get(pos);
        return entry != null && !entry.landed;
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        if (!BlockAnimationMod.isEnabled() || ANIMATIONS.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            ANIMATIONS.clear();
            return;
        }

        long now = System.currentTimeMillis();
        BlockRenderDispatcher dispatcher = client.getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        Vec3 camPos = camera.getPosition();

        Iterator<Map.Entry<BlockPos, AnimationEntry>> it = ANIMATIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, AnimationEntry> mapEntry = it.next();
            AnimationEntry anim = mapEntry.getValue();

            long elapsed = now - anim.startTime;
            if (!anim.landed && elapsed >= FALL_DURATION_MS) {
                anim.landed = true;
                anim.landedTime = now;
                spawnLandingParticles(client, anim.pos, anim.state);
                if (client.levelRenderer != null) {
                    client.levelRenderer.setSectionDirtyWithNeighbors(
                        anim.pos.getX() >> 4,
                        anim.pos.getY() >> 4,
                        anim.pos.getZ() >> 4
                    );
                }
            }

            if (anim.landed && (now - anim.landedTime > SETTLE_DURATION_MS)) {
                it.remove();
                continue;
            }

            float progress = Math.min(1.0f, (float) elapsed / (float) FALL_DURATION_MS);
            // Ease-in gravitational acceleration
            double eased = progress * progress;
            double yOffset = anim.landed ? 0.0 : (1.0 - eased) * START_HEIGHT;

            poseStack.pushPose();
            poseStack.translate(
                anim.pos.getX() - camPos.x,
                anim.pos.getY() + yOffset - camPos.y,
                anim.pos.getZ() - camPos.z
            );

            int light = LevelRenderer.getLightColor(client.level, anim.pos);
            dispatcher.renderSingleBlock(anim.state, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        bufferSource.endBatch();
    }

    private static void spawnLandingParticles(Minecraft client, BlockPos pos, BlockState state) {
        if (client.level == null) return;
        RandomSource random = client.level.random;
        BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, state);

        for (int i = 0; i < 12; i++) {
            double px = pos.getX() + 0.12 + random.nextDouble() * 0.76;
            double py = pos.getY() + 0.04;
            double pz = pos.getZ() + 0.12 + random.nextDouble() * 0.76;

            double vx = (random.nextDouble() - 0.5) * 0.16;
            double vy = random.nextDouble() * 0.08 + 0.03;
            double vz = (random.nextDouble() - 0.5) * 0.16;

            client.level.addParticle(particleOption, px, py, pz, vx, vy, vz);
        }
    }

    private static final class AnimationEntry {
        final BlockPos pos;
        final BlockState state;
        final long startTime;
        boolean landed;
        long landedTime;

        AnimationEntry(BlockPos pos, BlockState state, long startTime) {
            this.pos = pos;
            this.state = state;
            this.startTime = startTime;
        }
    }
}
