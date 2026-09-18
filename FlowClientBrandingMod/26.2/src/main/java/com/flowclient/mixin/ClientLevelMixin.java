package com.flowclient.mixin;

import com.flowclient.mods.environment.TimeChangerMod;
import com.flowclient.mods.environment.WeatherChangerMod;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class ClientLevelMixin {

    @Shadow public abstract boolean isClientSide();

    @Inject(method = "getOverworldClockTime", at = @At("RETURN"), cancellable = true)
    private void flowclient$overrideTime(CallbackInfoReturnable<Long> cir) {
        if (this.isClientSide() && TimeChangerMod.isEnabled()) {
            cir.setReturnValue(TimeChangerMod.getTargetTime());
        }
    }

    @Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
    private void flowclient$overrideRain(float delta, CallbackInfoReturnable<Float> cir) {
        if (this.isClientSide() && WeatherChangerMod.isEnabled()) {
            cir.setReturnValue(WeatherChangerMod.getRainLevel());
        }
    }

    @Inject(method = "getThunderLevel", at = @At("RETURN"), cancellable = true)
    private void flowclient$overrideThunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (this.isClientSide() && WeatherChangerMod.isEnabled()) {
            cir.setReturnValue(WeatherChangerMod.getThunderLevel());
        }
    }
}
