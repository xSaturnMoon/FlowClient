using CmlLib.Core;
using CmlLib.Core.ModLoaders.FabricMC;
using System.Net.Http;
using System.Text.Json;

var minecraftRoot = Path.Combine(
    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
    ".minecraft");

var mcPath = new MinecraftPath(minecraftRoot);
var launcher = new MinecraftLauncher(mcPath);

// Representative vanilla versions across every major Java-runtime era.
var vanillaVersions = new[]
{
    "1.8.9",
    "1.12.2",
    "1.16.5",
    "1.17.1",
    "1.18.2",
    "1.19.4",
    "1.20.1",
    "1.20.4",
    "1.20.6",
    "1.21.1",
    "1.21.4",
};

var latestSnapshot = await GetLatestSnapshotAsync();
if (latestSnapshot != null)
    vanillaVersions = [.. vanillaVersions, latestSnapshot];

var cases = new List<(string Label, Func<Task<string>> Resolve)>();
foreach (var version in vanillaVersions)
    cases.Add(($"Vanilla {version}", () => Task.FromResult(version)));

// Mod loaders on a few different MC versions (not every combo — install is version-agnostic in launcher code).
cases.Add(("Fabric 1.8.9", () => EnsureFabricAsync("1.8.9")));
cases.Add(("Fabric 1.16.5", () => EnsureFabricAsync("1.16.5")));
cases.Add(("Fabric 1.20.1", () => EnsureFabricAsync("1.20.1")));
cases.Add(("Fabric 1.21.4", () => EnsureFabricAsync("1.21.4")));

var failed = 0;
var passed = 0;
foreach (var test in cases)
{
    Console.WriteLine($"=== {test.Label} ===");
    try
    {
        var versionName = await test.Resolve();
        Console.WriteLine($"Profile: {versionName}");

        var baseVersion = ResolveBaseVersion(mcPath, versionName);
        if (!string.Equals(baseVersion, versionName, StringComparison.OrdinalIgnoreCase))
        {
            Console.WriteLine($"Base: {baseVersion}");
            await launcher.InstallAsync(baseVersion);
        }

        Console.WriteLine("Installing files…");
        await launcher.InstallAsync(versionName);

        var version = await launcher.GetVersionAsync(versionName);
        var javaPath = launcher.GetJavaPath(version);
        var javaOk = !string.IsNullOrWhiteSpace(javaPath) && File.Exists(javaPath);
        var jsonOk = File.Exists(Path.Combine(mcPath.Versions, versionName, $"{versionName}.json"));

        Console.WriteLine($"  version json: {(jsonOk ? "OK" : "MISSING")}");
        Console.WriteLine($"  java runtime: {(javaOk ? "OK" : "MISSING")} ({javaPath ?? "null"})");

        if (!jsonOk || !javaOk)
        {
            Console.WriteLine("  RESULT: FAIL");
            failed++;
        }
        else
        {
            Console.WriteLine("  RESULT: OK");
            passed++;
        }
    }
    catch (Exception ex)
    {
        Console.WriteLine($"  RESULT: FAIL ({ex.Message})");
        failed++;
    }

    Console.WriteLine();
}

Console.WriteLine($"Passed: {passed}, Failed: {failed}");
Console.WriteLine(failed == 0
    ? "All version installs verified successfully."
    : $"{failed} version(s) failed verification.");

return failed == 0 ? 0 : 1;

async Task<string> EnsureFabricAsync(string mcVersion)
{
    var existing = FindProfile("fabric", mcVersion);
    if (existing != null)
        return existing;

    var installer = new FabricInstaller(new HttpClient());
    return await installer.Install(mcVersion, mcPath);
}

static async Task<string?> GetLatestSnapshotAsync()
{
    try
    {
        using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(20) };
        var json = await http.GetStringAsync("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
        using var doc = JsonDocument.Parse(json);
        foreach (var v in doc.RootElement.GetProperty("versions").EnumerateArray())
        {
            if (v.GetProperty("type").GetString() == "snapshot")
                return v.GetProperty("id").GetString();
        }
    }
    catch
    {
        // Optional — skip snapshot if manifest is unreachable.
    }

    return null;
}

static string ResolveBaseVersion(MinecraftPath path, string versionName)
{
    var current = versionName;
    var visited = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
    while (!string.IsNullOrWhiteSpace(current) && visited.Add(current))
    {
        var jsonPath = Path.Combine(path.Versions, current, $"{current}.json");
        if (!File.Exists(jsonPath))
            return versionName;

        using var doc = JsonDocument.Parse(File.ReadAllText(jsonPath));
        if (doc.RootElement.TryGetProperty("inheritsFrom", out var parent))
        {
            current = parent.GetString() ?? "";
            continue;
        }

        return current;
    }

    return versionName;
}

string? FindProfile(string loaderToken, string mcVersion)
{
    var versionsDir = mcPath.Versions;
    if (!Directory.Exists(versionsDir))
        return null;

    string? best = null;
    foreach (var dir in Directory.GetDirectories(versionsDir))
    {
        var name = Path.GetFileName(dir);
        if (!name.Contains(loaderToken, StringComparison.OrdinalIgnoreCase))
            continue;
        if (!name.Contains(mcVersion, StringComparison.OrdinalIgnoreCase))
            continue;
        if (!File.Exists(Path.Combine(dir, $"{name}.json")))
            continue;
        if (best == null || string.Compare(name, best, StringComparison.OrdinalIgnoreCase) > 0)
            best = name;
    }

    return best;
}
