package com.flowclient.mixin;

import com.flowclient.mods.ModKeybindCategoryFilter;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;

@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin {
    @Redirect(
            method = "<init>(Lnet/minecraft/client/gui/screens/controls/KeyBindsScreen;Lnet/minecraft/client/Minecraft;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/commons/lang3/ArrayUtils;clone([Ljava/lang/Object;)[Ljava/lang/Object;"
            )
    )
    private Object[] flowclient$filterKeyMappingsForDisabledMods(Object[] array) {
        ModKeybindCategoryFilter.logKnownCategoriesOnce();
        KeyMapping[] original = (KeyMapping[]) array;
        return Arrays.stream((KeyMapping[]) ArrayUtils.clone(original))
                .filter(mapping -> !ModKeybindCategoryFilter.shouldHideKeyMapping(mapping))
                .toArray(KeyMapping[]::new);
    }
}
