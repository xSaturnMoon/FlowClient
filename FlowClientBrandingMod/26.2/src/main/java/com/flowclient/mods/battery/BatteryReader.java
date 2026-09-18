package com.flowclient.mods.battery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

final class BatteryReader {
    private static final long SYSTEM_TIMEOUT_SECONDS = 5L;
    private static final long CONNECTED_TIMEOUT_SECONDS = 60L;

    private static final String SYSTEM_SCRIPT = """
            $devices = @()
            $batteries = @(Get-CimInstance -ClassName Win32_Battery -ErrorAction SilentlyContinue)
            if ($batteries.Count -eq 0) {
              @{ devices = @() } | ConvertTo-Json -Compress -Depth 4
              return
            }
            $percent = $null
            $charging = $false
            $name = 'Battery'
            try {
              $status = Get-CimInstance -Namespace root/wmi -ClassName BatteryStatus -ErrorAction Stop | Select-Object -First 1
              $full = Get-CimInstance -Namespace root/wmi -ClassName BatteryFullChargedCapacity -ErrorAction Stop | Select-Object -First 1
              if ($status -and $full -and $full.FullChargedCapacity -gt 0) {
                $percent = [int][Math]::Round(($status.RemainingCapacity / $full.FullChargedCapacity) * 100)
                $percent = [Math]::Max(0, [Math]::Min(100, $percent))
              }
            } catch {}
            $readings = @()
            foreach ($bat in $batteries) {
              $pct = [int]$bat.EstimatedChargeRemaining
              if ($pct -ge 1 -and $pct -le 100 -and $pct -ne 255) {
                $readings += $pct
              }
              if (@(2,6,7,8,9) -contains [int]$bat.BatteryStatus) {
                $charging = $true
              }
              if ($bat.Name) { $name = $bat.Name.Trim() }
            }
            if ($readings.Count -gt 0) {
              $win32Percent = ($readings | Measure-Object -Maximum).Maximum
              if ($null -eq $percent -or $win32Percent -gt $percent) {
                $percent = [int]$win32Percent
              }
            }
            if ($null -ne $percent) {
              $devices += [ordered]@{
                name = $name
                percent = $percent
                charging = $charging
                type = 'system'
              }
            }
            @{ devices = @($devices) } | ConvertTo-Json -Compress -Depth 4
            """;

    private BatteryReader() {
    }

    static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    static BatterySnapshot readSystem() {
        return read(SYSTEM_SCRIPT, SYSTEM_TIMEOUT_SECONDS);
    }

    static BatterySnapshot readConnected() {
        if (!isSupported()) {
            return BatterySnapshot.EMPTY;
        }

        Path probe = BatteryProbeInstaller.ensureInstalled();
        if (probe == null) {
            return BatterySnapshot.EMPTY;
        }

        return readProcess(
                new ProcessBuilder(probe.toAbsolutePath().toString()),
                probe.getParent(),
                CONNECTED_TIMEOUT_SECONDS
        );
    }

    private static BatterySnapshot read(String script, long timeoutSeconds) {
        if (!isSupported()) {
            return BatterySnapshot.EMPTY;
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-Sta",
                    "-Command",
                    script
            );
            return readProcess(builder, null, timeoutSeconds);
        } catch (Exception ignored) {
            return BatterySnapshot.EMPTY;
        }
    }

    private static BatterySnapshot readProcess(ProcessBuilder builder, Path workingDirectory, long timeoutSeconds) {
        try {
            if (workingDirectory != null) {
                builder.directory(workingDirectory.toFile());
            }
            builder.redirectErrorStream(true);
            Process process = builder.start();

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder builderOutput = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        builderOutput.append(line);
                    }
                }
                output = builderOutput.toString();
            }

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return BatterySnapshot.EMPTY;
            }

            if (process.exitValue() != 0 || output.isBlank()) {
                return BatterySnapshot.EMPTY;
            }

            return parse(output);
        } catch (Exception ignored) {
            return BatterySnapshot.EMPTY;
        }
    }

    private static BatterySnapshot parse(String json) {
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return BatterySnapshot.EMPTY;
        }

        JsonObject root = JsonParser.parseString(json.substring(start, end + 1)).getAsJsonObject();
        if (!root.has("devices")) {
            return BatterySnapshot.EMPTY;
        }

        JsonElement devicesNode = root.get("devices");
        JsonArray array = new JsonArray();
        if (devicesNode.isJsonArray()) {
            array = devicesNode.getAsJsonArray();
        } else if (devicesNode.isJsonObject()) {
            array.add(devicesNode.getAsJsonObject());
        } else {
            return BatterySnapshot.EMPTY;
        }

        Map<String, BatteryDevice> unique = new LinkedHashMap<>();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) {
                continue;
            }
            JsonObject item = array.get(i).getAsJsonObject();
            String name = text(item, "name");
            if (name.isBlank()) {
                continue;
            }
            int percent = item.has("percent") ? item.get("percent").getAsInt() : -1;
            if (percent < 0 || percent > 100) {
                continue;
            }
            boolean charging = item.has("charging") && item.get("charging").getAsBoolean();
            BatteryDeviceType type = "system".equalsIgnoreCase(text(item, "type"))
                    ? BatteryDeviceType.SYSTEM
                    : BatteryDeviceType.CONNECTED;
            String key = type.name() + ":" + name.toLowerCase(Locale.ROOT);
            unique.put(key, new BatteryDevice(name, percent, charging, type));
        }

        List<BatteryDevice> devices = new ArrayList<>(unique.values());
        if (devices.isEmpty()) {
            return BatterySnapshot.EMPTY;
        }
        return new BatterySnapshot(List.copyOf(devices), System.currentTimeMillis());
    }

    private static String text(JsonObject root, String key) {
        if (!root.has(key) || root.get(key).isJsonNull()) {
            return "";
        }
        return root.get(key).getAsString();
    }
}
