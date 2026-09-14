package com.flowclient.mods.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls the Element X-style block placement falling animation.
 *
 * When a block is placed:
 *  1. A client-side FallingBlockEntity is spawned ~0.85 blocks above the target.
 *  2. The real block is hidden from chunk tessellation while the entity falls.
 *  3. On landing the entity is removed, particles burst out, and the chunk
 *     refreshes to reveal the now-visible static block.
 */
public final class BlockAnimationController {

    /** BlockPos → active animation tracking. */
    private static final Map<BlockPos, ActiveAnimation> ACTIVE = new ConcurrentHashMap<>();

    private BlockAnimationController() {}

    // ── Public API ─────────────────────────────────────────────────────────

    /** Called from ClientLevelBlockUpdateMixin on the render thread. */
    public static void onBlockPlaced(BlockPos pos, BlockState state) {
        if (!BlockAnimationMod.isEnabled()) return;
        if (state == null || state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (mc.player != null) {
            double distSq = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSq > 32.0 * 32.0) return;
        }

        // Cancel any previous animation at this position
        cancelAnimation(pos);

        // Spawn entity 0.85 blocks above target
        double startX = pos.getX() + 0.5;
        double startY = pos.getY() + 0.85;
        double startZ = pos.getZ() + 0.5;

        FallingBlockEntity entity = new FallingBlockEntity(EntityTypes.FALLING_BLOCK, mc.level);
        entity.setPos(startX, startY, startZ);
        // Set the block state via reflection (field is private in 26.2)
        if (!injectBlockState(entity, state)) return;

        // Prevent vanilla land-logic from touching the world
        entity.dropItem = false;
        entity.time = 1;

        mc.level.addEntity(entity);
        ACTIVE.put(pos.immutable(), new ActiveAnimation(entity, pos.immutable(), state));
    }

    /**
     * Tick called from a client-tick mixin every frame.
     * Detects when falling entities reach the target Y and finalises the animation.
     */
    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ACTIVE.clear(); return; }

        Iterator<Map.Entry<BlockPos, ActiveAnimation>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, ActiveAnimation> entry = it.next();
            ActiveAnimation anim = entry.getValue();

            if (!anim.entity.isAlive()) {
                it.remove();
                continue;
            }

            double entityY = anim.entity.getY();
            double targetY = anim.pos.getY();

            boolean landed = entityY <= targetY + 0.05;
            boolean timeout = anim.entity.tickCount > 200;

            if (landed || timeout) {
                anim.entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                if (landed) {
                    spawnLandingParticles(mc, anim.pos, anim.state);
                }
                it.remove();
                // Trigger a visual re-mesh so the real block appears
                mc.level.sendBlockUpdated(anim.pos, anim.state, anim.state, 0);
            }
        }
    }

    /**
     * Returns true while the given position has an active falling animation.
     * Used by ModelBlockRendererMixin to suppress tessellation of the static block.
     */
    public static boolean isAnimating(BlockPos pos) {
        return ACTIVE.containsKey(pos);
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private static void cancelAnimation(BlockPos pos) {
        ActiveAnimation old = ACTIVE.remove(pos);
        if (old != null && old.entity.isAlive()) {
            old.entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean injectBlockState(FallingBlockEntity entity, BlockState state) {
        try {
            java.lang.reflect.Field f = FallingBlockEntity.class.getDeclaredField("blockState");
            f.setAccessible(true);
            f.set(entity, state);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void spawnLandingParticles(Minecraft mc, BlockPos pos, BlockState state) {
        if (mc.level == null) return;
        RandomSource random = mc.level.getRandom();
        BlockParticleOption opt = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 22; i++) {
            double px = pos.getX() + 0.1 + random.nextDouble() * 0.8;
            double py = pos.getY() + 0.02;
            double pz = pos.getZ() + 0.1 + random.nextDouble() * 0.8;
            double vx = (random.nextDouble() - 0.5) * 0.24;
            double vy = random.nextDouble() * 0.14 + 0.05;
            double vz = (random.nextDouble() - 0.5) * 0.24;
            mc.level.addParticle(opt, px, py, pz, vx, vy, vz);
        }
    }

    // ── Inner class ─────────────────────────────────────────────────────────

    private static final class ActiveAnimation {
        final FallingBlockEntity entity;
        final BlockPos pos;
        final BlockState state;
        ActiveAnimation(FallingBlockEntity entity, BlockPos pos, BlockState state) {
            this.entity = entity;
            this.pos = pos;
            this.state = state;
        }
    }
}