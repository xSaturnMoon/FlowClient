package com.flowclient.mods.fps;

final class FpsSmoother {
    private static final int MAX_SAMPLES = 30;
    private static final long[] samples = new long[MAX_SAMPLES];
    private static int index;
    private static int count;

    private FpsSmoother() {
    }

    static int smooth(int fps) {
        samples[index] = fps;
        index = (index + 1) % MAX_SAMPLES;
        if (count < MAX_SAMPLES) {
            count++;
        }

        long total = 0;
        for (int i = 0; i < count; i++) {
            total += samples[i];
        }
        return (int) Math.round(total / (double) count);
    }

    static void reset() {
        index = 0;
        count = 0;
    }
}
