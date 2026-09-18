package com.flowclient.mods.media;

public record MediaSessionState(
        boolean active,
        String title,
        String artist,
        String album,
        String trackId,
        String artPath,
        double positionSeconds,
        double durationSeconds,
        boolean playing,
        String appId,
        long polledAtMs
) {
    public static final MediaSessionState INACTIVE = new MediaSessionState(
            false,
            "",
            "",
            "",
            "",
            "",
            0.0,
            0.0,
            false,
            "",
            System.currentTimeMillis()
    );

    public String trackKey() {
        if (!this.trackId.isBlank()) {
            return this.trackId;
        }
        return this.title + "|" + this.artist;
    }

    public double interpolatedPositionSeconds() {
        if (!this.playing) {
            return this.positionSeconds;
        }
        double elapsed = (System.currentTimeMillis() - this.polledAtMs) / 1000.0;
        double value = this.positionSeconds + elapsed;
        if (this.durationSeconds > 0.0) {
            return Math.min(value, this.durationSeconds);
        }
        return value;
    }

    public double progressRatio() {
        if (this.durationSeconds <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, this.interpolatedPositionSeconds() / this.durationSeconds));
    }

    public boolean hasProgress() {
        return this.durationSeconds > 0.0;
    }

    public boolean isNearEnd() {
        return this.hasProgress() && this.progressRatio() >= 0.88;
    }

    public boolean isFinished() {
        return this.hasProgress() && this.progressRatio() >= 0.995;
    }
}
