using Launcher.Services;

var svc = new MinecraftLaunchService();
var result = await svc.LaunchAsync("26.2-FABRIC", new Progress<string>(Console.WriteLine));

Console.WriteLine($"Success: {result.Success}");
Console.WriteLine($"Message: {result.Message}");

if (!result.Success)
    return 1;

Console.WriteLine($"PID: {result.Process?.Id}");
await Task.Delay(60000);
return 0;
