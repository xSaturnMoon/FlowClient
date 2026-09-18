package com.flowclient.modpanel;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

public final class ModEntry {
    private final String id;
    private final String displayName;
    private final ModCategory category;
    private final int priority;
    private final Item iconItem;
    @Nullable
    private final Identifier customIcon;
    private final BooleanSupplier enabled;
    private final Runnable toggle;
    @Nullable
    private final Function<Screen, Screen> settingsScreenFactory;

    public ModEntry(
            String id,
            String displayName,
            ModCategory category,
            int priority,
            Item iconItem,
            @Nullable Identifier customIcon,
            BooleanSupplier enabled,
            Runnable toggle,
            @Nullable Function<Screen, Screen> settingsScreenFactory
    ) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.priority = priority;
        this.iconItem = iconItem;
        this.customIcon = customIcon;
        this.enabled = enabled;
        this.toggle = toggle;
        this.settingsScreenFactory = settingsScreenFactory;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public ModCategory category() {
        return this.category;
    }

    public int priority() {
        return this.priority;
    }

    public Item iconItem() {
        return this.iconItem;
    }

    @Nullable
    public Identifier customIcon() {
        return this.customIcon;
    }

    public boolean isEnabled() {
        return this.enabled.getAsBoolean();
    }

    public void toggle() {
        this.toggle.run();
    }

    public Screen openSettings(Screen parent) {
        if (this.settingsScreenFactory != null) {
            return this.settingsScreenFactory.apply(parent);
        }
        return new ModSettingsPlaceholderScreen(parent, this.displayName);
    }

    public boolean hasDedicatedSettingsScreen() {
        return this.settingsScreenFactory != null;
    }
}
