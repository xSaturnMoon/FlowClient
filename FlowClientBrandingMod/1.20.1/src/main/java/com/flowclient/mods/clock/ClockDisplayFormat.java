package com.flowclient.mods.clock;

public enum ClockDisplayFormat {
    HOURS_24("24h"),
    HOURS_24_SECONDS("24h + seconds"),
    HOURS_12("12h AM/PM"),
    HOURS_12_SECONDS("12h + seconds"),
    DATE_AND_TIME("Date + time");

    private final String label;

    ClockDisplayFormat(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }
}
