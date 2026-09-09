using System.Collections.Concurrent;
using System.IO;
using System.Net.Http;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace Launcher.Helpers
{
    public static class RemoteImageLoader
    {
        private static readonly HttpClient Http = new()
        {
            Timeout = TimeSpan.FromSeconds(20)
        };

        private static readonly ConcurrentDictionary<string, ImageSource?> Cache = new(StringComparer.OrdinalIgnoreCase);

        public static ImageSource? TryLoad(string? url)
        {
            if (string.IsNullOrWhiteSpace(url))
                return null;

            url = url.Trim();
            if (url.StartsWith("//", StringComparison.Ordinal))
                url = "https:" + url;

            return Cache.GetOrAdd(url, LoadCore);
        }

        private static ImageSource? LoadCore(string url)
        {
            try
            {
                if (url.StartsWith("data:", StringComparison.OrdinalIgnoreCase))
                    return LoadDataUri(url);

                if (Uri.TryCreate(url, UriKind.Absolute, out var absolute))
                {
                    if (absolute.Scheme == "pack" || absolute.Scheme == Uri.UriSchemeFile)
                        return LoadPackOrFile(absolute);

                    if (absolute.Scheme == Uri.UriSchemeHttp || absolute.Scheme == Uri.UriSchemeHttps)
                    {
                        var bytes = Http.GetByteArrayAsync(absolute).ConfigureAwait(false).GetAwaiter().GetResult();
                        if (bytes.Length == 0 || LooksLikeSvg(bytes))
                            return null;

                        return DecodeBitmap(bytes);
                    }
                }

                return null;
            }
            catch
            {
                return null;
            }
        }

        private static ImageSource LoadPackOrFile(Uri uri)
        {
            var bitmap = new BitmapImage();
            bitmap.BeginInit();
            bitmap.CacheOption = BitmapCacheOption.OnLoad;
            bitmap.UriSource = uri;
            bitmap.EndInit();
            bitmap.Freeze();
            return bitmap;
        }

        private static ImageSource? LoadDataUri(string dataUri)
        {
            var comma = dataUri.IndexOf(',');
            if (comma < 0)
                return null;

            var meta = dataUri[..comma];
            var payload = dataUri[(comma + 1)..];
            byte[] bytes;

            if (meta.Contains(";base64", StringComparison.OrdinalIgnoreCase))
            {
                bytes = Convert.FromBase64String(payload);
            }
            else
            {
                bytes = System.Text.Encoding.UTF8.GetBytes(Uri.UnescapeDataString(payload));
            }

            if (bytes.Length == 0 || LooksLikeSvg(bytes))
                return null;

            return DecodeBitmap(bytes);
        }

        private static BitmapImage DecodeBitmap(byte[] bytes)
        {
            using var stream = new MemoryStream(bytes);
            var bitmap = new BitmapImage();
            bitmap.BeginInit();
            bitmap.CacheOption = BitmapCacheOption.OnLoad;
            bitmap.StreamSource = stream;
            bitmap.EndInit();
            bitmap.Freeze();
            return bitmap;
        }

        private static bool LooksLikeSvg(byte[] bytes)
        {
            if (bytes.Length < 4)
                return false;

            var prefix = System.Text.Encoding.UTF8.GetString(bytes, 0, Math.Min(bytes.Length, 256)).TrimStart();
            return prefix.StartsWith("<svg", StringComparison.OrdinalIgnoreCase)
                || prefix.StartsWith("<?xml", StringComparison.OrdinalIgnoreCase) && prefix.Contains("<svg", StringComparison.OrdinalIgnoreCase);
        }
    }
}
