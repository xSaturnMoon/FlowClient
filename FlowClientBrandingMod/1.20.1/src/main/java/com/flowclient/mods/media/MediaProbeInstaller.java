package com.flowclient.mods.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

final class MediaProbeInstaller {
    private static final String PROBE_DIR = "FlowMediaProbe";
    private static final String PROBE_EXE = "FlowMediaProbe.exe";

    private MediaProbeInstaller() {
    }

    static Path ensureInstalled() {
        Path installed = installDir().resolve(PROBE_EXE);
        Path sourceDir = resolveSourceDir();
        if (sourceDir != null && Files.isDirectory(sourceDir)) {
            Path sourceExe = sourceDir.resolve(PROBE_EXE);
            if (Files.isRegularFile(sourceExe) && shouldRefresh(installed, sourceExe)) {
                try {
                    copyDirectory(sourceDir, installDir());
                } catch (IOException ignored) {
                    // Keep the existing probe if refresh fails.
                }
            }
        }

        return Files.isRegularFile(installed) ? installed : null;
    }

    private static boolean shouldRefresh(Path installed, Path source) {
        if (!Files.isRegularFile(installed)) {
            return true;
        }

        try {
            return Files.getLastModifiedTime(source).compareTo(Files.getLastModifiedTime(installed)) > 0;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static Path installDir() {
        String appData = System.getenv("APPDATA");
        Path root = appData == null || appData.isBlank()
                ? Path.of(System.getProperty("user.home"), "FlowLauncher", "bin", PROBE_DIR)
                : Path.of(appData, "FlowLauncher", "bin", PROBE_DIR);
        return root;
    }

    private static Path resolveSourceDir() {
        String home = firstNonBlank(
                System.getProperty("flow.client.home"),
                System.getenv("FLOW_CLIENT_HOME")
        );
        if (home != null) {
            Path candidate = Path.of(home, PROBE_DIR);
            if (isProbeDir(candidate)) {
                return candidate;
            }
        }

        Path[] fallbacks = {
                Path.of("FlowMediaProbe"),
                Path.of("..", "FlowMediaProbe"),
                Path.of("..", "..", "FlowMediaProbe"),
                Path.of("..", "dist", "FlowMediaProbe"),
                Path.of("..", "Launcher", "bin", "Release", "net8.0-windows", "FlowMediaProbe"),
                Path.of("..", "Launcher", "bin", "Debug", "net8.0-windows", "FlowMediaProbe")
        };

        for (Path fallback : fallbacks) {
            Path resolved = fallback.toAbsolutePath().normalize();
            if (isProbeDir(resolved)) {
                return resolved;
            }
        }

        return null;
    }

    private static boolean isProbeDir(Path directory) {
        return Files.isDirectory(directory) && Files.isRegularFile(directory.resolve(PROBE_EXE));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    try (InputStream input = Files.newInputStream(path)) {
                        Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }
}
