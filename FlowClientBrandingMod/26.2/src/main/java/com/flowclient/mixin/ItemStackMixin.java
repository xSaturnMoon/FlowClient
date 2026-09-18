package com.flowclient.mixin;

import com.flowclient.mods.tooltip.ShulkerBoxTooltip;
import com.flowclient.mods.tooltip.ShulkerTooltipHelper;
import com.flowclient.mods.tooltip.ShulkerTooltipMod;
import java.util.Optional;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void flowclient$injectShulkerTooltip(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (!ShulkerTooltipMod.isEnabled() || cir.getReturnValue().isPresent()) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        if (!ShulkerTooltipHelper.isShulkerBox(stack)) {
            return;
        }

        ItemContainerContents contents = ShulkerTooltipHelper.readContents(stack);
        if (contents == null) {
            return;
        }

        cir.setReturnValue(Optional.of(new ShulkerBoxTooltip(contents)));
    }
}
