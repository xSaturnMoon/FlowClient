package com.flowclient.mods.fullbright;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

public final class FullbrightController {
    private static final double FULLBRIGHT_GAMMA = 16.0D;

    private static Double savedGamma;

    private FullbrightController() {
    }

    public static void sync() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        apply(client);
    }

    public static void apply(Minecraft client) {
        OptionInstance<Double> gamma = client.options.gamma();
        if (FullbrightMod.isEnabled()) {
            if (savedGamma == null) {
                savedGamma = gamma.get();
            }
            if (gamma.get() < FULLBRIGHT_GAMMA) {
                gamma.set(FULLBRIGHT_GAMMA);
            }
            return;
        }
        restore(gamma);
    }

    private static void restore(OptionInstance<Double> gamma) {
        if (savedGamma == null) {
            return;
        }
        gamma.set(savedGamma);
        savedGamma = null;
    }

    public static void onClientStopping() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            restore(client.options.gamma());
        }
        savedGamma = null;
    }
}
