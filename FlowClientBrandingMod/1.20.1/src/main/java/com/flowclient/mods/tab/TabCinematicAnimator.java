package com.flowclient.mods.tab;

final class TabCinematicAnimator {
    private float value;
    private float from;
    private float to;
    private float elapsed;
    private float duration = 0.2f;

    float value() {
        return this.value;
    }

    void snap(float newValue) {
        this.value = newValue;
        this.from = newValue;
        this.to = newValue;
        this.elapsed = this.duration;
    }

    void setTarget(float target, float newDuration) {
        if (Math.abs(this.to - target) < 0.0001f) {
            if (Math.abs(this.value - target) < 0.0001f) {
                this.value = target;
                this.elapsed = this.duration;
                return;
            }
            if (this.elapsed < this.duration) {
                return;
            }
        }

        this.from = this.value;
        this.to = target;
        this.duration = Math.max(0.05f, newDuration);
        this.elapsed = 0.0f;
    }

    void tick(float deltaSeconds) {
        if (this.elapsed >= this.duration) {
            this.value = this.to;
            return;
        }

        this.elapsed += deltaSeconds;
        float t = Math.min(1.0f, this.elapsed / this.duration);
        float eased = this.to > this.from ? easeOutCubic(t) : easeInCubic(t);
        this.value = this.from + (this.to - this.from) * eased;

        if (this.elapsed >= this.duration) {
            this.value = this.to;
        }
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static float easeOutCubic(float t) {
        float inverse = 1.0f - t;
        return 1.0f - inverse * inverse * inverse;
    }
}
