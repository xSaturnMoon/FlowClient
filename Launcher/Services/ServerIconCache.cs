using System.IO;

namespace Launcher.Services
{
    public static class ServerIconCache
    {
        private static readonly string IconsDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "server-icons");

        public static string? SaveIcon(string host, int port, byte[]? pngBytes)
        {
            if (pngBytes is not { Length: > 0 })
                return null;

            try
            {
                Directory.CreateDirectory(IconsDir);
                var path = Path.Combine(IconsDir, $"{SanitizeKey(host)}_{port}.png");
                File.WriteAllBytes(path, pngBytes);
                return path;
            }
            catch
            {
                return null;
            }
        }

        public static string? GetIconPath(string host, int port)
        {
            var path = Path.Combine(IconsDir, $"{SanitizeKey(host)}_{port}.png");
            return File.Exists(path) ? path : null;
        }

        private static string SanitizeKey(string host)
        {
            foreach (var c in Path.GetInvalidFileNameChars())
                host = host.Replace(c, '_');
            return host.ToLowerInvariant();
        }
    }
}
