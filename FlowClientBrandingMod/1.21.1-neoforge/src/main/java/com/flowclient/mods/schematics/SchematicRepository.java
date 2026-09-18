package com.flowclient.mods.schematics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SchematicRepository {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".schem", ".schematic", ".litematic", ".nbt"
    );

    private static final Path ROOT = Path.of(
            System.getenv("APPDATA") == null ? System.getProperty("user.home") : System.getenv("APPDATA"),
            "FlowLauncher",
            "AllSchematics"
    );

    private SchematicRepository() {}

    public static Path getRoot() {
        return ROOT;
    }

    public static void ensureRoot() throws IOException {
        Files.createDirectories(ROOT);
    }

    public static List<SchematicEntry> listAll() throws IOException {
        ensureRoot();
        List<SchematicEntry> entries = new ArrayList<>();
        try (var stream = Files.list(ROOT)) {
            stream.filter(Files::isRegularFile)
                    .filter(SchematicRepository::isSupported)
                    .forEach(path -> entries.add(toEntry(path)));
        }
        entries.sort(Comparator.comparing(e -> e.getDisplayName().toLowerCase(Locale.ROOT)));
        return entries;
    }

    public static List<SchematicEntry> search(String query) throws IOException {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return listAll();
        }
        return listAll().stream()
                .filter(entry -> entry.getDisplayName().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    public static SchematicEntry findById(String id) throws IOException {
        return listAll().stream()
                .filter(entry -> entry.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static void importFile(Path source) throws IOException {
        ensureRoot();
        String fileName = source.getFileName().toString();
        if (!isSupported(fileName)) {
            throw new IOException("Unsupported schematic format: " + fileName);
        }
        Files.copy(source, ROOT.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void rename(SchematicEntry entry, String newName) throws IOException {
        String sanitized = sanitizeName(newName);
        if (sanitized.isBlank()) {
            throw new IOException("Invalid name");
        }
        String extension = getExtension(entry.getFile().getFileName().toString());
        Path target = ROOT.resolve(sanitized + extension);
        Files.move(entry.getFile(), target, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void delete(SchematicEntry entry) throws IOException {
        Files.deleteIfExists(entry.getFile());
    }

    private static SchematicEntry toEntry(Path path) {
        String fileName = path.getFileName().toString();
        String baseName = fileName.substring(0, fileName.length() - getExtension(fileName).length());
        long size = 0;
        Instant modified = Instant.EPOCH;
        try {
            size = Files.size(path);
            modified = Files.getLastModifiedTime(path).toInstant();
        } catch (IOException ignored) {
        }
        return new SchematicEntry(fileName, baseName, path, size, modified);
    }

    private static boolean isSupported(Path path) {
        return isSupported(path.getFileName().toString());
    }

    private static boolean isSupported(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static String getExtension(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return ext;
            }
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }

    private static String sanitizeName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
