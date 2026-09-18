package com.flowclient.mods.zoom;

final class ZoomAnimator {
  private float value;
  private float from;
  private float to;
  private float elapsed;
  private float duration;
  private ZoomTransition transition = ZoomTransition.LINEAR;

  float value() {
    return this.value;
  }

  void snap(float newValue) {
    this.value = newValue;
    this.from = newValue;
    this.to = newValue;
    this.elapsed = this.duration;
  }

  void setTarget(float target, float newDuration, ZoomTransition newTransition) {
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
    this.duration = Math.max(0.001f, newDuration);
    this.transition = newTransition;
    this.elapsed = 0.0f;
  }

  void tick(float deltaSeconds) {
    if (this.elapsed >= this.duration) {
      this.value = this.to;
      return;
    }

    this.elapsed += deltaSeconds;
    float t = Math.min(1.0f, this.elapsed / this.duration);
    float eased = (float) this.transition.apply(t);
    this.value = this.from + (this.to - this.from) * eased;

    if (this.elapsed >= this.duration) {
      this.value = this.to;
    }
  }
}
