package com.flowclient.mods.battery;

public record BatteryDevice(String name, int percent, boolean charging, BatteryDeviceType type) {
}
