using System;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace Launcher.Services
{
    public static class SkinAvatarService
    {
        private static readonly HttpClient Http = CreateClient();
        private const int AvatarSize = 56;

        private static HttpClient CreateClient()
        {
            var client = new HttpClient();
            client.DefaultRequestHeaders.UserAgent.ParseAdd("FlowLauncher/1.0");
            return client;
        }

        public static async Task<BitmapSource?> LoadHeadAsync(string? skinUrl, string? username, string? uuid)
        {
            if (!string.IsNullOrWhiteSpace(skinUrl))
            {
                try
                {
                    var bytes = await Http.GetByteArrayAsync(skinUrl);
                    var head = ExtractHead(bytes);
                    if (head != null)
                        return head;
                }
                catch
                {
                    // Fall through to mc-heads.
                }
            }

            var id = !string.IsNullOrWhiteSpace(uuid)
                ? uuid.Replace("-", "", StringComparison.Ordinal)
                : username;

            if (string.IsNullOrWhiteSpace(id))
                return null;

            try
            {
                var url = $"https://mc-heads.net/avatar/{Uri.EscapeDataString(id)}/{AvatarSize}?v={DateTime.UtcNow.Ticks}";
                var bytes = await Http.GetByteArrayAsync(url);
                return LoadBitmap(bytes);
            }
            catch
            {
                return null;
            }
        }

        private static BitmapSource? LoadBitmap(byte[] bytes)
        {
            using var stream = new MemoryStream(bytes);
            var bmp = new BitmapImage();
            bmp.BeginInit();
            bmp.StreamSource = stream;
            bmp.CacheOption = BitmapCacheOption.OnLoad;
            bmp.EndInit();
            bmp.Freeze();
            return bmp;
        }

        private static BitmapSource? ExtractHead(byte[] pngBytes)
        {
            var skin = LoadBitmap(pngBytes);
            if (skin == null || skin.PixelWidth < 64 || skin.PixelHeight < 8)
                return null;

            var factor = AvatarSize / 8;
            var head = UpscaleRegionNearest(skin, 8, 8, 8, 8, factor);
            if (head == null)
                return null;

            if (skin.PixelHeight < 32)
                return head;

            var hat = UpscaleRegionNearest(skin, 40, 8, 8, 8, factor);
            if (hat == null)
                return head;

            var drawing = new DrawingVisual();
            using (var ctx = drawing.RenderOpen())
            {
                ctx.DrawImage(head, new Rect(0, 0, AvatarSize, AvatarSize));
                ctx.DrawImage(hat, new Rect(0, 0, AvatarSize, AvatarSize));
            }

            var target = new RenderTargetBitmap(AvatarSize, AvatarSize, 96, 96, PixelFormats.Pbgra32);
            target.Render(drawing);
            target.Freeze();
            return target;
        }

        private static BitmapSource? UpscaleRegionNearest(BitmapSource source, int x, int y, int width, int height, int factor)
        {
            BitmapSource bgraSource = source;
            if (bgraSource.Format != PixelFormats.Bgra32 && bgraSource.Format != PixelFormats.Pbgra32)
                bgraSource = new FormatConvertedBitmap(bgraSource, PixelFormats.Bgra32, null, 0);

            int srcStride = bgraSource.PixelWidth * 4;
            byte[] srcPixels = new byte[bgraSource.PixelHeight * srcStride];
            bgraSource.CopyPixels(srcPixels, srcStride, 0);

            int dstW = width * factor;
            int dstH = height * factor;
            int dstStride = dstW * 4;
            byte[] dstPixels = new byte[dstH * dstStride];

            for (int row = 0; row < height; row++)
            {
                int srcRow = (y + row) * srcStride;
                int dstRowBase = row * factor;

                for (int col = 0; col < width; col++)
                {
                    int srcIndex = srcRow + (x + col) * 4;
                    byte b = srcPixels[srcIndex];
                    byte g = srcPixels[srcIndex + 1];
                    byte r = srcPixels[srcIndex + 2];
                    byte a = srcPixels[srcIndex + 3];

                    for (int fy = 0; fy < factor; fy++)
                    {
                        int dstRow = (dstRowBase + fy) * dstStride;
                        int dstColBase = col * factor;

                        for (int fx = 0; fx < factor; fx++)
                        {
                            int dstIndex = dstRow + (dstColBase + fx) * 4;
                            dstPixels[dstIndex] = b;
                            dstPixels[dstIndex + 1] = g;
                            dstPixels[dstIndex + 2] = r;
                            dstPixels[dstIndex + 3] = a;
                        }
                    }
                }
            }

            var bitmap = BitmapSource.Create(dstW, dstH, 96, 96, PixelFormats.Bgra32, null, dstPixels, dstStride);
            bitmap.Freeze();
            return bitmap;
        }
    }
}
