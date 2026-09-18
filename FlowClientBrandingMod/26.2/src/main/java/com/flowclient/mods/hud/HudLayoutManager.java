package com.flowclient.mods.hud;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.util.EnumMap;
import java.util.Map;

public final class HudLayoutManager implements HudLayout.MinecraftContext {
    private static final HudLayoutManager INSTANCE = new HudLayoutManager();
    private final Map<HudElement, HudLayout> layouts = new EnumMap<>(HudElement.class);

    private HudLayoutManager() {
        for (HudElement element : HudElement.values()) {
            this.layouts.put(element, new HudLayout());
        }
    }

    public static HudLayoutManager get() {
        return INSTANCE;
    }

    public HudLayout layout(HudElement element) {
        return this.layouts.get(element);
    }

    public int resolveX(HudElement element, int width, int height) {
        return this.layout(element).resolveX(this, element, width, height);
    }

    public int resolveY(HudElement element, int width, int height) {
        return this.layout(element).resolveY(this, element, width, height);
    }

    public float resolveScale(HudElement element) {
        return this.layout(element).getScale();
    }

    public void setPosition(HudElement element, int x, int y) {
        this.layout(element).setPosition(x, y);
        FlowModConfig.save();
    }

    public void setPositionQuiet(HudElement element, int x, int y) {
        this.layout(element).setPosition(x, y);
    }

    public void flushPosition(HudElement element) {
        FlowModConfig.flushSave();
    }

    public void setScale(HudElement element, float scale) {
        this.layout(element).setScale(scale);
        FlowModConfig.save();
    }

    public void setScaleQuiet(HudElement element, float scale) {
        this.layout(element).setScale(scale);
    }

    public void applyLayout(HudElement element, int x, int y, float scale) {
        HudLayout layout = this.layout(element);
        layout.setPosition(x, y);
        layout.setScale(scale);
        FlowModConfig.flushSave();
    }

    public void resetPosition(HudElement element) {
        this.layout(element).resetPosition();
        FlowModConfig.save();
    }

    public void resetScale(HudElement element) {
        this.layout(element).resetScale();
        FlowModConfig.save();
    }

    public void load(JsonObject root) {
        if (root == null || !root.has("hudLayouts")) {
            return;
        }
        JsonObject layoutsJson = root.getAsJsonObject("hudLayouts");
        for (HudElement element : HudElement.values()) {
            if (layoutsJson.has(element.getId())) {
                this.layout(element).load(layoutsJson.getAsJsonObject(element.getId()));
            }
        }
    }

    public void save(JsonObject root) {
        JsonObject layoutsJson = new JsonObject();
        for (HudElement element : HudElement.values()) {
            JsonObject elementJson = new JsonObject();
            this.layout(element).save(elementJson);
            layoutsJson.add(element.getId(), elementJson);
        }
        root.add("hudLayouts", layoutsJson);
    }

    @Override
    public Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
