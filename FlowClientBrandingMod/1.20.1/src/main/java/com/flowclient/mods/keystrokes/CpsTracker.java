package com.flowclient.mods.keystrokes;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks left/right mouse click timestamps to calculate CPS (clicks per second)
 * over a 1-second rolling window.
 */
public final class CpsTracker {
    private static final long WINDOW_MS = 1000L;

    private static final Deque<Long> leftClicks  = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();

    private CpsTracker() {}

    public static void recordLeft()  { leftClicks.addLast(System.currentTimeMillis()); }
    public static void recordRight() { rightClicks.addLast(System.currentTimeMillis()); }

    public static int getLeftCps()  { return countRecent(leftClicks); }
    public static int getRightCps() { return countRecent(rightClicks); }

    private static int countRecent(Deque<Long> queue) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        while (!queue.isEmpty() && queue.peekFirst() < cutoff) {
            queue.pollFirst();
        }
        return queue.size();
    }
}
