package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiChatFilter;
import com.flowclient.mods.jei.JeiDisableGuard;
import mezz.jei.library.startup.JeiStarter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JeiStarter.class, remap = false)
public abstract class JeiStarterMixin {
    @Inject(method = "verifyClientRecipes", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$skipRecipeWarningsWhenBlocked(Minecraft minecraft, CallbackInfo ci) {
        if (JeiDisableGuard.shouldBlock()) {
            ci.cancel();
        }
    }

    @Inject(method = "writeChatMessage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void flowclient$filterStarterChat(Minecraft minecraft, Component message, CallbackInfo ci) {
        if (JeiChatFilter.shouldSuppress(message)) {
            ci.cancel();
        }
    }
}
