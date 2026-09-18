package com.flowclient.mods.zoom;

public enum ZoomTransition {
    LINEAR("Linear"),
    EASE_OUT_EXPONENTIAL("Ease Out Exponential"),
    EASE_IN_EXPONENTIAL("Ease In Exponential"),
    EASE_IN_OUT_EXPONENTIAL("Ease In Out Exponential");

    private final String label;

    ZoomTransition(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public double apply(double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        return switch (this) {
            case LINEAR -> clamped;
            case EASE_OUT_EXPONENTIAL -> clamped >= 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * clamped);
            case EASE_IN_EXPONENTIAL -> clamped <= 0.0 ? 0.0 : Math.pow(2.0, 10.0 * (clamped - 1.0));
            case EASE_IN_OUT_EXPONENTIAL -> {
                if (clamped <= 0.0) {
                    yield 0.0;
                }
                if (clamped >= 1.0) {
                    yield 1.0;
                }
                if (clamped < 0.5) {
                    yield Math.pow(2.0, 20.0 * clamped - 10.0) / 2.0;
                }
                yield (2.0 - Math.pow(2.0, -20.0 * clamped + 10.0)) / 2.0;
            }
        };
    }

    public ZoomTransition next() {
        ZoomTransition[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
