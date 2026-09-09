using System.IO;
using System.Net.Http;
using System.Text.Json;

namespace Launcher.Services
{
    public static class ServerIconFetcher
    {
        private static readonly HttpClient Http = new()
        {
            Timeout = TimeSpan.FromSeconds(6)
        };

        public static string? TryFetch(string host, int port)
        {
            if (string.IsNullOrWhiteSpace(host))
                return null;

            var cached = ServerIconCache.GetIconPath(host, port);
            if (cached != null)
                return cached;

            var address = port == 25565 ? host.Trim() : $"{host.Trim()}:{port}";

            var bytes = TryDownloadIcon($"https://api.mcsrvstat.us/icon/{address}")
                ?? TryDownloadStatusIcon($"https://api.mcstatus.io/v2/status/java/{address}");

            if (bytes == null)
                return null;

            return ServerIconCache.SaveIcon(host, port, bytes);
        }

        private static byte[]? TryDownloadIcon(string url)
        {
            try
            {
                var bytes = Http.GetByteArrayAsync(url).ConfigureAwait(false).GetAwaiter().GetResult();
                return LooksLikePng(bytes) ? bytes : null;
            }
            catch
            {
                return null;
            }
        }

        private static byte[]? TryDownloadStatusIcon(string url)
        {
            try
            {
                var json = Http.GetStringAsync(url).ConfigureAwait(false).GetAwaiter().GetResult();
                using var doc = JsonDocument.Parse(json);
                if (!doc.RootElement.TryGetProperty("icon", out var iconProp))
                    return null;

                var dataUri = iconProp.GetString();
                if (string.IsNullOrWhiteSpace(dataUri) || !dataUri.StartsWith("data:image", StringComparison.OrdinalIgnoreCase))
                    return null;

                var comma = dataUri.IndexOf(',');
                if (comma < 0)
                    return null;

                var bytes = Convert.FromBase64String(dataUri[(comma + 1)..]);
                return LooksLikePng(bytes) ? bytes : null;
            }
            catch
            {
                return null;
            }
        }

        private static bool LooksLikePng(byte[] bytes)
            => bytes.Length >= 64
               && bytes[0] == 0x89
               && bytes[1] == 0x50
               && bytes[2] == 0x4E
               && bytes[3] == 0x47;
    }
}
