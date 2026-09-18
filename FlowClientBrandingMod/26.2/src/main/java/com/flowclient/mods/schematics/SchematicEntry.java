package com.flowclient.mods.schematics;

import java.nio.file.Path;
import java.time.Instant;

public final class SchematicEntry {
    private final String id;
    private final String displayName;
    private final Path file;
    private final long sizeBytes;
    private final Instant modifiedAt;

    public SchematicEntry(String id, String displayName, Path file, long sizeBytes, Instant modifiedAt) {
        this.id = id;
        this.displayName = displayName;
        this.file = file;
        this.sizeBytes = sizeBytes;
        this.modifiedAt = modifiedAt;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Path getFile() {
        return file;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public String getSizeLabel() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        }
        return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }
}
