package com.flowclient.mods.inventory;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class InventorySortSettings {
    private static final InventorySortSettings DEFAULTS = new InventorySortSettings();

    private boolean includeHotbar = true;
    private boolean mergeStacks = true;

    private static InventorySortSettings current = copy(DEFAULTS);

    private InventorySortSettings() {
    }

    public static InventorySortSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("inventorySortSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "inventorySortSettings");
        InventorySortSettings settings = copy(DEFAULTS);
        settings.includeHotbar = GsonHelper.getAsBoolean(json, "includeHotbar", DEFAULTS.includeHotbar);
        settings.mergeStacks = GsonHelper.getAsBoolean(json, "mergeStacks", DEFAULTS.mergeStacks);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("includeHotbar", current.includeHotbar);
        json.addProperty("mergeStacks", current.mergeStacks);
        root.add("inventorySortSettings", json);
    }

    public static void reset(boolean persist) {
        current = copy(DEFAULTS);
        if (persist) {
            current.persist();
        }
    }

    public void persist() {
        FlowModConfig.save();
    }

    public boolean includeHotbar() {
        return includeHotbar;
    }

    public boolean mergeStacks() {
        return mergeStacks;
    }

    public void setIncludeHotbar(boolean includeHotbar) {
        this.includeHotbar = includeHotbar;
    }

    public void setMergeStacks(boolean mergeStacks) {
        this.mergeStacks = mergeStacks;
    }

    private static InventorySortSettings copy(InventorySortSettings source) {
        InventorySortSettings copy = new InventorySortSettings();
        copy.includeHotbar = source.includeHotbar;
        copy.mergeStacks = source.mergeStacks;
        return copy;
    }
}
