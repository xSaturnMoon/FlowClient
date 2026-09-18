package com.flowclient.mods.media;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class MediaSessionPoller {
    private static final MediaSessionPoller INSTANCE = new MediaSessionPoller();
    private static final long POLL_INTERVAL_MS = 1000L;

    private final AtomicReference<MediaSessionState> current = new AtomicReference<>(MediaSessionState.INACTIVE);
    private final AtomicReference<MediaProbeStatus> probeStatus = new AtomicReference<>(MediaProbeStatus.MISSING);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "FlowClient-MediaSession");
        thread.setDaemon(true);
        return thread;
    });

    private volatile ScheduledFuture<?> task;

    private MediaSessionPoller() {
    }

    public static MediaSessionPoller get() {
        return INSTANCE;
    }

    public MediaSessionState state() {
        return this.current.get();
    }

    public MediaProbeStatus probeStatus() {
        return this.probeStatus.get();
    }

    public void start() {
        if (this.task != null) {
            return;
        }

        this.pollNow();
        this.task = this.executor.scheduleAtFixedRate(
                this::pollNow,
                POLL_INTERVAL_MS,
                POLL_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel(false);
            this.task = null;
        }
        this.current.set(MediaSessionState.INACTIVE);
        this.probeStatus.set(MediaProbeStatus.MISSING);
    }

    public void pollNow() {
        if (!MediaHudMod.isEnabled()) {
            this.current.set(MediaSessionState.INACTIVE);
            this.probeStatus.set(MediaProbeStatus.MISSING);
            return;
        }

        MediaSessionState state = WindowsMediaSessionReader.read(this.probeStatus);
        this.current.set(state);
    }
}
