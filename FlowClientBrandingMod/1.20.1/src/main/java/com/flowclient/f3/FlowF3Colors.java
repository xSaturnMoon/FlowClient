package com.flowclient.f3;

import com.flowclient.compat.FlowArgb;

/**
 * Lunar F3 palette: each section uses a dark label tone and a bright value tone.
 */
public final class FlowF3Colors {
    public static final int HEADER_LABEL = 0xFFAA0000;   // §4 dark red
    public static final int HEADER_VALUE = 0xFFFF5555;   // §c light red

    public static final int LOC_LABEL = 0xFF00AA00;     // §2 dark green
    public static final int LOC_VALUE = 0xFF55FF55;     // §a light green

    public static final int STAT_LABEL = 0xFF5555FF;     // §9 blue
    public static final int STAT_VALUE = 0xFF55FFFF;     // §b aqua (Lunar stats)

    public static final int HW_LABEL = 0xFFAA00AA;       // §5 dark purple
    public static final int HW_VALUE = 0xFFFF55FF;       // §d bright pink

    public static final int TARGET_LABEL = 0xFFFFAA00;   // §6 gold / dark orange
    public static final int TARGET_VALUE = 0xFFFFFF55;   // §e light orange

    /** Lunar-style neutral dark gray per-line backdrop. */
    public static final int LINE_BACKGROUND = FlowArgb.color(185, 8, 8, 10);

    private FlowF3Colors() {
    }
}
