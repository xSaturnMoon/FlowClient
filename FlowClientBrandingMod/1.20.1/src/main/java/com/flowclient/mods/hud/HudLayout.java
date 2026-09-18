package com.flowclient.mods.hud;

import com.google.gson.JsonObject;

public final class HudLayout {
    public static final int UNSET = Integer.MIN_VALUE;

    private int x = UNSET;
    private int y = UNSET;
    private float scale = HudScale.DEFAULT;

    public int getRawX() {
        return this.x;
    }

    public int getRawY() {
        return this.y;
    }

    public float getScale() {
        return this.scale;
    }

    public boolean usesDefaultPosition() {
        return this.x == UNSET || this.y == UNSET;
    }

    public int resolveX(MinecraftContext ctx, HudElement element, int width, int height) {
        if (this.x == UNSET) {
            return element.getDefaultX(ctx.minecraft(), width, height);
        }
        return this.x;
    }

    public int resolveY(MinecraftContext ctx, HudElement element, int width, int height) {
        if (this.y == UNSET) {
            return element.getDefaultY(ctx.minecraft(), width, height);
        }
        return this.y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setScale(float scale) {
        this.scale = HudScale.clamp(scale);
    }

    public void resetPosition() {
        this.x = UNSET;
        this.y = UNSET;
    }

    public void resetScale() {
        this.scale = HudScale.DEFAULT;
    }

    public void load(JsonObject json) {
        if (json.has("x")) {
            this.x = json.get("x").getAsInt();
        }
        if (json.has("y")) {
            this.y = json.get("y").getAsInt();
        }
        if (json.has("scale")) {
            this.setScale(json.get("scale").getAsFloat());
        }
    }

    public void save(JsonObject json) {
        if (this.x != UNSET) {
            json.addProperty("x", this.x);
        }
        if (this.y != UNSET) {
            json.addProperty("y", this.y);
        }
        json.addProperty("scale", this.scale);
    }

    public interface MinecraftContext {
        net.minecraft.client.Minecraft minecraft();
    }
}
