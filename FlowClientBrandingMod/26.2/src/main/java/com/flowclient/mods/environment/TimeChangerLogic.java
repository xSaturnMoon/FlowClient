package com.flowclient.mods.environment;

import java.time.LocalTime;

public final class TimeChangerLogic {
    private TimeChangerLogic() {
    }

    public static long resolveTime(TimeChangerSettings settings) {
        return switch (settings.mode()) {
            case REAL_TIME -> settings.lockCycle() ? settings.lockedTime() : computeRealTimeTicks();
            case PRESET -> settings.preset().ticks();
            case CUSTOM -> Math.floorMod(settings.customTime(), 24000L);
        };
    }

    public static long computeRealTimeTicks() {
        LocalTime now = LocalTime.now();
        long ticks = (now.getHour() * 1000L)
                + (now.getMinute() * 1000L / 60L)
                + (now.getSecond() * 1000L / 3600L)
                - 6000L;
        return Math.floorMod(ticks, 24000L);
    }

    public static String formatTicks(long ticks) {
        long normalized = Math.floorMod(ticks, 24000L);
        int hours = (int) ((normalized / 1000L + 6L) % 24L);
        int minutes = (int) ((normalized % 1000L) * 60L / 1000L);
        return String.format("%02d:%02d", hours, minutes);
    }
}
