package com.flowclient.mods.jei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicInteger;

public final class JeiDisableGuard {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowclient-jei");
    private static final AtomicInteger LIST_DISPLAY_LOG_COUNT = new AtomicInteger();

    private JeiDisableGuard() {
    }

    public static boolean shouldBlock() {
        return !JeiMod.isEnabled();
    }

    public static void logListDisplayedCheck() {
        int count = LIST_DISPLAY_LOG_COUNT.incrementAndGet();
        if (count <= 5 || count % 200 == 0) {
            boolean blocking = shouldBlock();
            LOGGER.info("[flowclient-jei] shouldBlock() called from isListDisplayed (#{}), blocking={} (enabled={}, jeiModLoaded={}, jeiPresent={})",
                    count,
                    blocking,
                    JeiMod.isEnabled(),
                    JeiRuntimeController.isJeiModLoaded(),
                    JeiRuntimeController.isJeiInternalPresent());
        }
    }

    public static void cancelVoid(CallbackInfo ci) {
        if (shouldBlock()) {
            ci.cancel();
        }
    }

    public static void cancelFalse(CallbackInfoReturnable<Boolean> cir) {
        if (shouldBlock()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    public static void cancelTrue(CallbackInfoReturnable<Boolean> cir) {
        if (shouldBlock()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
