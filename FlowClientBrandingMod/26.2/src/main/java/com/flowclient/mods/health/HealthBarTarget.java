package com.flowclient.mods.health;

public enum HealthBarTarget {
    ALL("Everyone"),
    PLAYERS("Players"),
    ANIMALS("Animals"),
    MONSTERS("Monsters");

    private final String label;

    HealthBarTarget(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
