package com.flowclient.f3;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public final class F3Fonts {
    public static final ResourceLocation F3 = ResourceLocation.fromNamespaceAndPath("flowclient", "f3");
    private static final Style F3_STYLE = Style.EMPTY.withFont(F3);

    private F3Fonts() {
    }

    public static Component text(String value) {
        return Component.literal(value).withStyle(F3_STYLE);
    }

    public static Component text(Component value) {
        return value.copy().withStyle(F3_STYLE);
    }
}
