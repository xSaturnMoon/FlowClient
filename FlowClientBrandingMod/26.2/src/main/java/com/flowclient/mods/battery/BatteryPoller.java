package com.flowclient.mods.battery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class BatteryPoller {
    private static final BatteryPoller INSTANCE = new BatteryPoller();
    private static final long SYSTEM_POLL_INTERVAL_MS = 5000L;
    private static final long CONNECTED_POLL_INTERVAL_MS = 30000L;

    private final AtomicReference<List<BatteryDevice>> systemDevices = new AtomicReference<>(List.of());
    private final AtomicReference<List<BatteryDevice>> connectedDevices = new AtomicReference<>(List.of());
    private final AtomicBoolean systemPollComplete = new AtomicBoolean(false);
    private final AtomicBoolean connectedPollComplete = new AtomicBoolean(false);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "FlowClient-Battery");
        thread.setDaemon(true);
        return thread;
    });

    private volatile ScheduledFuture<?> systemTask;
    private volatile ScheduledFuture<?> connectedTask;

    private BatteryPoller() {
    }

    public static BatteryPoller get() {
        return INSTANCE;
    }

    public BatterySnapshot snapshot() {
        return new BatterySnapshot(mergedDevices(), System.currentTimeMillis());
    }

    public boolean isScanning() {
        if (!BatteryHudMod.isEnabled()) {
            return false;
        }
        BatteryHudSettings settings = BatteryHudSettings.get();
        if (settings.showSystemBattery() && !this.systemPollComplete.get()) {
            return true;
        }
        return settings.showConnectedDevices() && !this.connectedPollComplete.get();
    }

    public void start() {
        if (this.systemTask != null) {
            return;
        }
        resetPollState();
        requestPoll();
        this.systemTask = this.executor.scheduleAtFixedRate(
                this::pollSystemAsync,
                SYSTEM_POLL_INTERVAL_MS,
                SYSTEM_POLL_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        this.connectedTask = this.executor.scheduleAtFixedRate(
                this::pollConnectedAsync,
                CONNECTED_POLL_INTERVAL_MS,
                CONNECTED_POLL_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        if (this.systemTask != null) {
            this.systemTask.cancel(false);
            this.systemTask = null;
        }
        if (this.connectedTask != null) {
            this.connectedTask.cancel(false);
            this.connectedTask = null;
        }
        this.systemDevices.set(List.of());
        this.connectedDevices.set(List.of());
        resetPollState();
    }

    public void clear() {
        this.systemDevices.set(List.of());
        this.connectedDevices.set(List.of());
        resetPollState();
    }

    public void requestPoll() {
        resetPollState();
        this.executor.execute(() -> {
            pollSystemAsync();
            pollConnectedAsync();
        });
    }

    public void requestConnectedPoll() {
        this.connectedPollComplete.set(false);
        this.executor.execute(this::pollConnectedAsync);
    }

    private void pollSystemAsync() {
        if (!BatteryHudMod.isEnabled()) {
            this.systemDevices.set(List.of());
            return;
        }
        if (!BatteryHudSettings.get().showSystemBattery()) {
            this.systemDevices.set(List.of());
            this.systemPollComplete.set(true);
            return;
        }
        BatterySnapshot snapshot = BatteryReader.readSystem();
        this.systemDevices.set(snapshot.isEmpty() ? List.of() : snapshot.devices());
        this.systemPollComplete.set(true);
    }

    private void pollConnectedAsync() {
        if (!BatteryHudMod.isEnabled()) {
            this.connectedDevices.set(List.of());
            return;
        }
        if (!BatteryHudSettings.get().showConnectedDevices()) {
            this.connectedDevices.set(List.of());
            this.connectedPollComplete.set(true);
            return;
        }
        BatterySnapshot snapshot = BatteryReader.readConnected();
        this.connectedDevices.set(snapshot.isEmpty() ? List.of() : snapshot.devices());
        this.connectedPollComplete.set(true);
    }

    private void resetPollState() {
        this.systemPollComplete.set(false);
        this.connectedPollComplete.set(false);
    }

    private List<BatteryDevice> mergedDevices() {
        Map<String, BatteryDevice> merged = new LinkedHashMap<>();
        for (BatteryDevice device : this.systemDevices.get()) {
            merged.put(key(device), device);
        }
        for (BatteryDevice device : this.connectedDevices.get()) {
            merged.put(key(device), device);
        }
        return List.copyOf(new ArrayList<>(merged.values()));
    }

    private static String key(BatteryDevice device) {
        return device.type().name() + ":" + device.name().toLowerCase();
    }
}
