package com.flowclient.util;

public final class FlowArgb {
    private FlowArgb() {
    }

    public static int color(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
