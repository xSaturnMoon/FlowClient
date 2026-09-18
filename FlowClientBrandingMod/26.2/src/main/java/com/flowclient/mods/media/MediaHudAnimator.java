package com.flowclient.mods.media;

final class MediaHudAnimator {
    private static final long TRANSITION_MS = 420L;
    private static final float FULL_SPIN_DEG_PER_SEC = 50.0F;

    private String visibleTrackKey = "";
    private String incomingTrackKey = "";
    private float discRotation;
    private float textOffset;
    private float textAnimFrom;
    private float textAnimTo;
    private long textAnimStartMs;
    private long textAnimDurationMs;
    private boolean textAnimating;
    private Phase phase = Phase.HIDDEN;
    private long lastFrameMs = System.currentTimeMillis();

    void reset() {
        this.visibleTrackKey = "";
        this.incomingTrackKey = "";
        this.discRotation = 0.0F;
        this.textOffset = 0.0F;
        this.textAnimating = false;
        this.phase = Phase.HIDDEN;
        this.lastFrameMs = System.currentTimeMillis();
    }

    void tick(MediaSessionState state, float spinMultiplier) {
        long now = System.currentTimeMillis();
        float delta = Math.min(0.05F, (now - this.lastFrameMs) / 1000.0F);
        this.lastFrameMs = now;

        this.updateTextAnimation(now);

        if (!state.active() || state.title().isBlank()) {
            if (this.phase != Phase.HIDDEN && this.phase != Phase.EXITING) {
                this.beginExit();
            }
            this.updateDiscRotation(state, delta, 0.0F);
            return;
        }

        String trackKey = state.trackKey();
        if (this.phase == Phase.HIDDEN && !trackKey.isBlank()) {
            this.beginEnter(trackKey);
        }

        boolean trackChanged = !trackKey.isBlank() && !trackKey.equals(this.visibleTrackKey) && !trackKey.equals(this.incomingTrackKey);

        if (state.isFinished() && this.phase == Phase.PLAYING) {
            this.beginExit();
        } else if (trackChanged) {
            if (this.phase == Phase.HIDDEN || this.phase == Phase.EXITING) {
                if (this.phase == Phase.EXITING && this.textAnimating) {
                    this.incomingTrackKey = trackKey;
                } else {
                    this.beginEnter(trackKey);
                }
            } else {
                this.beginExit();
                this.incomingTrackKey = trackKey;
            }
        }

        float spinFactor = switch (this.phase) {
            case HIDDEN, EXITING -> easeOut(this.textAnimating ? 1.0F - this.textAnimProgress(now) : 0.0F);
            case ENTERING -> Math.max(0.25F, this.textAnimProgress(now));
            case PLAYING -> state.playing() ? (state.isNearEnd() ? slowdown(state.progressRatio()) : 1.0F) : 0.0F;
        };

        this.updateDiscRotation(state, delta, spinFactor * Math.max(1.0F, spinMultiplier));

        if (this.phase == Phase.EXITING && !this.textAnimating) {
            this.visibleTrackKey = "";
            this.phase = Phase.HIDDEN;
            if (!this.incomingTrackKey.isBlank()) {
                String next = this.incomingTrackKey;
                this.incomingTrackKey = "";
                this.beginEnter(next);
            }
        }

        if (this.phase == Phase.ENTERING && !this.textAnimating) {
            this.phase = Phase.PLAYING;
        }
    }

    float discRotation() {
        return this.discRotation;
    }

    float textOffset() {
        return this.textOffset;
    }

    Phase phase() {
        return this.phase;
    }

    String visibleTrackKey() {
        return this.visibleTrackKey;
    }

    private void beginEnter(String trackKey) {
        this.visibleTrackKey = trackKey;
        this.incomingTrackKey = "";
        this.phase = Phase.ENTERING;
        this.startTextAnimation(-1.0F, 0.0F);
    }

    private void beginExit() {
        if (this.phase == Phase.HIDDEN || this.phase == Phase.EXITING) {
            return;
        }
        this.phase = Phase.EXITING;
        this.startTextAnimation(this.textOffset, -1.0F);
    }

    private void startTextAnimation(float from, float to) {
        this.textAnimFrom = from;
        this.textAnimTo = to;
        this.textAnimStartMs = System.currentTimeMillis();
        this.textAnimDurationMs = TRANSITION_MS;
        this.textAnimating = true;
    }

    private void updateTextAnimation(long now) {
        if (!this.textAnimating) {
            return;
        }

        float progress = this.textAnimProgress(now);
        this.textOffset = lerp(this.textAnimFrom, this.textAnimTo, progress);
        if (progress >= 1.0F) {
            this.textAnimating = false;
            this.textOffset = this.textAnimTo;
        }
    }

    private float textAnimProgress(long now) {
        if (this.textAnimDurationMs <= 0L) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (now - this.textAnimStartMs) / (float) this.textAnimDurationMs));
    }

    private void updateDiscRotation(MediaSessionState state, float delta, float spinFactor) {
        if (spinFactor <= 0.0F) {
            return;
        }
        if (!state.active()) {
            return;
        }
        this.discRotation = (this.discRotation + FULL_SPIN_DEG_PER_SEC * spinFactor * delta) % 360.0F;
    }

    private static float slowdown(double progress) {
        double endWindow = Math.max(0.0, (progress - 0.88) / 0.12);
        return (float) Math.max(0.12, 1.0 - endWindow * 0.85);
    }

    private static float easeOut(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return 1.0F - (1.0F - clamped) * (1.0F - clamped);
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    enum Phase {
        HIDDEN,
        ENTERING,
        PLAYING,
        EXITING
    }
}
