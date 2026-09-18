package com.flowclient.mixin;

import com.flowclient.mods.advancements.AdvancementsUnlockController;
import com.flowclient.mods.advancements.AdvancementsUnlockMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void flowclient$trackBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!AdvancementsUnlockMod.isEnabled() || !cir.getReturnValue()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        AdvancementsUnlockController.get().onBlockBroken(client.level.getBlockState(pos));
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void flowclient$trackItemUsedOn(CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        if (!AdvancementsUnlockMod.isEnabled()) {
            return;
        }
        if (cir.getReturnValue().consumesAction()) {
            AdvancementsUnlockController.get().onItemUsed();
        }
    }
}
