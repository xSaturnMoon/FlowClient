package com.flowclient.mods.environment;

public enum TimePreset {
    SUNRISE("Sunrise", 0L),
    MORNING("Morning", 1000L),
    NOON("Noon", 6000L),
    SUNSET("Sunset", 12000L),
    NIGHT("Night", 13000L),
    MIDNIGHT("Midnight", 18000L);

    private final String label;
    private final long ticks;

    TimePreset(String label, long ticks) {
        this.label = label;
        this.ticks = ticks;
    }

    public String label() {
        return this.label;
    }

    public long ticks() {
        return this.ticks;
    }

    public TimePreset next() {
        TimePreset[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
