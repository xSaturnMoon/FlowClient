package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiDisableGuard;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = RecipesGui.class, remap = false)
public abstract class JeiRecipesGuiMixin {
    @Inject(method = "show(Ljava/util/List;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockShow(List<IFocus<?>> focuses, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "showTypes(Ljava/util/List;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockShowTypes(List<IRecipeType<?>> recipeTypes, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "showRecipes", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void flowclient$blockShowRecipes(IRecipeCategory<T> recipeCategory, List<T> recipes,
            List<IFocus<?>> focuses, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "isOpen", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$hideOpen(CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.cancelFalse(cir);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.cancelFalse(cir);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockMouseClick(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.cancelFalse(cir);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockTick(CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }
}
