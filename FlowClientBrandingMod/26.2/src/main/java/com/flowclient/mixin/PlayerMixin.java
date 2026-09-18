package com.flowclient.mixin;

import com.flowclient.mods.otherclient.ClientBadgeHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void flowclient$addClientBadgeToNametag(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        cir.setReturnValue(ClientBadgeHelper.decorateDisplayName(self, cir.getReturnValue()));
    }
}
