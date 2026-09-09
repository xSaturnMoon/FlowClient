using Launcher.Services;
var svc = new MinecraftLaunchService();
var result = await svc.LaunchAsync("1.20.1-FABRIC", new Progress<string>(Console.WriteLine));
Console.WriteLine($"Success: {result.Success}");
Console.WriteLine($"Message: {result.Message}");
if (result.Process != null) {
  Console.WriteLine($"PID: {result.Process.Id}");
  for (int i = 0; i < 90; i++) {
    await Task.Delay(1000);
    if (result.Process.HasExited) {
      Console.WriteLine($"Exited: {result.Process.ExitCode}");
      break;
    }
  }
}
