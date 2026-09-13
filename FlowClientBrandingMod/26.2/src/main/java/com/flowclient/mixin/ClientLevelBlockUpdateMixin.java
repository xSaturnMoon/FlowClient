package com.flowclient.mixin;

import com.flowclient.mods.render.BlockAnimationController;
import com.flowclient.mods.render.BlockAnimationMod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelBlockUpdateMixin {
    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void flowclient$onBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        if (BlockAnimationMod.isEnabled() && (oldState.isAir() || oldState.canBeReplaced()) && !newState.isAir() && newState.getRenderShape() != RenderShape.INVISIBLE) {
            BlockAnimationController.onBlockPlaced(pos, newState);
        }
    }
}