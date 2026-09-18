package com.flowclient.mods.battery;

import java.util.List;

public record BatterySnapshot(List<BatteryDevice> devices, long updatedAtMs) {
    public static final BatterySnapshot EMPTY = new BatterySnapshot(List.of(), 0L);

    public boolean isEmpty() {
        return this.devices.isEmpty();
    }
}
