package com.flowclient.mods.hud;

public final class HudEdgeGuides {
    private HudEdgeGuides() {
    }

    public static EdgeDistances distances(HudBounds.Bounds bounds, int screenWidth, int screenHeight) {
        int left = bounds.x();
        int top = bounds.y();
        int right = screenWidth - (bounds.x() + bounds.width());
        int bottom = screenHeight - (bounds.y() + bounds.height());
        return new EdgeDistances(left, top, right, bottom);
    }

    public record EdgeDistances(int left, int top, int right, int bottom) {
        public int nearest() {
            return Math.min(Math.min(this.left, this.top), Math.min(this.right, this.bottom));
        }

        public String nearestEdgeName() {
            int nearest = this.nearest();
            if (this.left == nearest) return "Left";
            if (this.top == nearest) return "Top";
            if (this.right == nearest) return "Right";
            return "Bottom";
        }
    }
}
