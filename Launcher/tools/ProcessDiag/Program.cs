using System.Diagnostics;
using System.Management;
using System.Text.Json;
using Launcher.Services;

static void DumpJava(string label)
{
    Console.WriteLine($"\n=== {label} ===");
    using var searcher = new ManagementObjectSearcher(
        "SELECT ProcessId, ParentProcessId, Name, ExecutablePath, CommandLine FROM Win32_Process WHERE Name='java.exe' OR Name='javaw.exe'");
    foreach (var obj in searcher.Get())
    {
        if (obj is not ManagementObject mo) continue;
        Console.WriteLine($"PID={mo["ProcessId"]} PPID={mo["ParentProcessId"]} Name={mo["Name"]}");
        Console.WriteLine($"  Exe: {mo["ExecutablePath"]}");
        var cmd = mo["CommandLine"]?.ToString() ?? "";
        Console.WriteLine($"  Cmd: {(cmd.Length > 300 ? cmd[..300] + "..." : cmd)}");
    }
}

var before = SnapshotJavaPids();
DumpJava("BEFORE LAUNCH");

var svc = new MinecraftLaunchService();
var result = await svc.LaunchAsync("26.2-FABRIC", new Progress<string>(Console.WriteLine));
Console.WriteLine($"Launch success={result.Success} msg={result.Message} trackedPid={result.Process?.Id}");

if (!result.Success || result.Process == null)
    return 1;

var tracked = result.Process;
for (var i = 0; i < 20; i++)
{
    await Task.Delay(2000);
    DumpJava($"T+{(i + 1) * 2}s (tracked HasExited={SafeExited(tracked)})");
}

Console.WriteLine("\nWaiting for tracked process to exit (close Minecraft now)...");
try
{
    tracked.WaitForExit();
}
catch (Exception ex)
{
    Console.WriteLine($"WaitForExit error: {ex.Message}");
}

Console.WriteLine($"Tracked exited. HasExited={SafeExited(tracked)}");
await Task.Delay(3000);
DumpJava("AFTER TRACKED EXIT (+3s)");

var sessionPath = Path.Combine(
    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
    "FlowLauncher", "FlowVersions", "26.2-FABRIC", ".flowclient-session.json");
if (File.Exists(sessionPath))
    Console.WriteLine($"Session file:\n{File.ReadAllText(sessionPath)}");
else
    Console.WriteLine("Session file MISSING");

MinecraftProcessTracker.Instance.KillProcess();
await Task.Delay(2000);
DumpJava("AFTER KillProcess (+2s)");

return 0;

static HashSet<int> SnapshotJavaPids()
{
    var pids = new HashSet<int>();
    using var searcher = new ManagementObjectSearcher(
        "SELECT ProcessId FROM Win32_Process WHERE Name='java.exe' OR Name='javaw.exe'");
    foreach (var obj in searcher.Get())
    {
        if (obj is ManagementObject mo)
            pids.Add(Convert.ToInt32(mo["ProcessId"]));
    }
    return pids;
}

static bool SafeExited(Process p)
{
    try { return p.HasExited; }
    catch { return true; }
}
