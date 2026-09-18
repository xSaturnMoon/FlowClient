package com.flowclient.mods.serveraddress;

public enum ServerAddressDisplayMode {
    SERVER_NAME("Server Name"),
    BRAND("Server Brand"),
    ADDRESS("IP Address");

    private final String label;

    ServerAddressDisplayMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ServerAddressDisplayMode next() {
        ServerAddressDisplayMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
