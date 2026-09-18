package com.flowclient.mods.advancements;

import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.LightPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

final class ClientLocationMatcher {
    private ClientLocationMatcher() {
    }

    static boolean matches(Level level, Vec3 position, LocationPredicate predicate) {
        if (predicate == null) {
            return true;
        }

        if (predicate.dimension().isPresent()) {
            ResourceKey<Level> required = predicate.dimension().get();
            if (!level.dimension().equals(required)) {
                return false;
            }
        }

        BlockPos blockPos = BlockPos.containing(position);
        if (predicate.block().isPresent() && !matchesBlock(level, blockPos, predicate.block().get())) {
            return false;
        }

        if (predicate.biomes().isPresent() && !predicate.biomes().get().contains(level.getBiome(blockPos))) {
            return false;
        }

        if (predicate.light().isPresent() && !matchesLight(level, blockPos, predicate.light().get())) {
            return false;
        }

        if (predicate.canSeeSky().isPresent()) {
            boolean canSeeSky = level.canSeeSky(blockPos);
            if (canSeeSky != predicate.canSeeSky().get()) {
                return false;
            }
        }

        if (predicate.structures().isPresent()) {
            return false;
        }

        return true;
    }

    private static boolean matchesBlock(LevelReader level, BlockPos pos, BlockPredicate predicate) {
        return predicate.matches(new BlockInWorld(level, pos, false));
    }

    private static boolean matchesLight(Level level, BlockPos pos, LightPredicate predicate) {
        int combined = level.getMaxLocalRawBrightness(pos);
        return predicate.composite().matches(combined);
    }
}
