package com.flowclient.mods.explosion;

import com.flowclient.mods.preview.GizmoShapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CrystalBedPreviewRenderer {
    private static final int CRYSTAL_COLOR = 0xFFFF6688;
    private static final int BED_COLOR = 0xFFFFAA44;
    private static final double CRYSTAL_RADIUS = 6.0D;
    private static final double BED_RADIUS = 5.0D;

    private CrystalBedPreviewRenderer() {
    }

    public static boolean shouldRender() {
        return CrystalBedPreviewMod.isEnabled();
    }

    public static void render(Minecraft client) {
        if (!CrystalBedPreviewMod.isEnabled() || client.player == null || client.level == null) {
            return;
        }

        LocalPlayer player = client.player;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        if (isCrystal(main) || isCrystal(off)) {
            renderCrystal(client, player);
        }

        if (isBed(main) || isBed(off)) {
            renderBed(client, player);
        }
    }

    private static void renderCrystal(Minecraft client, LocalPlayer player) {
        BlockHitResult hit = resolveBlockHit(client, player);
        if (hit == null) {
            return;
        }

        BlockPos placePos = hit.getBlockPos().relative(hit.getDirection());
        drawPlacementBlock(placePos, 0x66FF6688);
        Vec3 center = Vec3.atCenterOf(placePos);
        GizmoShapes.wireframeSphere(center, CRYSTAL_RADIUS, CRYSTAL_COLOR, 1.75F);
        GizmoShapes.horizontalCircle(center, CRYSTAL_RADIUS, CRYSTAL_COLOR, 2.0F);
    }

    private static void renderBed(Minecraft client, LocalPlayer player) {
        Level level = client.level;
        if (level == null || !bedExplosionDimension(level)) {
            return;
        }

        BlockHitResult hit = resolveBlockHit(client, player);
        if (hit == null) {
            return;
        }

        BlockPos bedPos = hit.getBlockPos();
        if (!hit.getDirection().getAxis().isHorizontal()) {
            bedPos = bedPos.relative(hit.getDirection());
        }

        drawPlacementBlock(bedPos, 0x66FFAA44);
        Vec3 center = Vec3.atCenterOf(bedPos);
        GizmoShapes.wireframeSphere(center, BED_RADIUS, BED_COLOR, 1.75F);
        GizmoShapes.horizontalCircle(center, BED_RADIUS, BED_COLOR, 2.0F);
    }

    private static void drawPlacementBlock(BlockPos pos, int fillColor) {
        GizmoStyle style = GizmoStyle.strokeAndFill(fillColor | 0x55000000, 1.5F, fillColor);
        Gizmos.cuboid(pos, style);
    }

    private static BlockHitResult resolveBlockHit(Minecraft client, LocalPlayer player) {
        double reach = player.blockInteractionRange();
        HitResult hit = player.pick(reach, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit;
        }

        if (client.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit;
        }

        return null;
    }

    private static boolean bedExplosionDimension(Level level) {
        return level.dimension() == Level.NETHER || level.dimension() == Level.END;
    }

    private static boolean isCrystal(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.END_CRYSTAL);
    }

    private static boolean isBed(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BedItem;
    }
}
