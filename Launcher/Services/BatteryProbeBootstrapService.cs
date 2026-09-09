using System.IO;

namespace Launcher.Services
{
    public static class BatteryProbeBootstrapService
    {
        private static readonly string TargetDirectory = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher",
            "bin",
            "FlowBatteryProbe");

        public static void EnsureInstalled()
        {
            var source = Path.Combine(AppContext.BaseDirectory, "FlowBatteryProbe");
            if (!Directory.Exists(source))
                return;

            try
            {
                CopyDirectory(source, TargetDirectory);
            }
            catch
            {
                // Best effort; the mod will retry installation later.
            }
        }

        private static void CopyDirectory(string source, string target)
        {
            Directory.CreateDirectory(target);

            foreach (var file in Directory.GetFiles(source))
            {
                var destination = Path.Combine(target, Path.GetFileName(file));
                File.Copy(file, destination, overwrite: true);
            }
        }
    }
}
