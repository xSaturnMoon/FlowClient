package com.flowclient.mixin;

import com.flowclient.mods.advancements.AdvancementsUnlockController;
import com.flowclient.mods.advancements.AdvancementsUnlockMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class AdvancementTrackingMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void flowclient$trackDeathAndKills(DamageSource source, CallbackInfo ci) {
        if (!AdvancementsUnlockMod.isEnabled()) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        if (self == client.player) {
            AdvancementsUnlockController.get().onDeath(source);
            return;
        }

        if (source.getEntity() == client.player) {
            AdvancementsUnlockController.get().onMobKill(self);
        }
    }

    @Inject(method = "completeUsingItem", at = @At("TAIL"))
    private void flowclient$trackConsume(CallbackInfo ci) {
        if (!AdvancementsUnlockMod.isEnabled()) {
            return;
        }

        if (!((Object) this instanceof LocalPlayer player)) {
            return;
        }

        ItemStack stack = player.getUseItem();
        if (!stack.isEmpty()) {
            AdvancementsUnlockController.get().onItemConsumed(stack);
        }
    }
}
