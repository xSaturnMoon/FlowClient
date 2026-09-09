using System;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Media.Media3D;

namespace Launcher.Components
{
    /// <summary>
    /// Minecraft skin 3D viewer with Nearest Neighbor upscaling for pixel-perfect clarity
    /// and full support for 1.8+ 64x64 overlays (hat, jacket, sleeves, pants).
    /// </summary>
    public partial class SkinViewer3D : UserControl
    {
        private static readonly HttpClient Http = new();

        // ── Model state ───────────────────────────────────────────────────────
        private bool _isSlim = false;
        private BitmapSource? _currentSkinTexture;
        private BitmapSource? _currentCapeTexture;

        // ── Rotation state ────────────────────────────────────────────────────
        private bool   _dragging;
        private Point  _lastPos;
        private double _angleY = 28.0;
        private double _angleX = -6.0;
        private double _cameraZ = 7.5;
        private const double ModelCenterY = 0.45;
        private bool _capePreviewMode;
        private int _idleTick;

        private readonly AxisAngleRotation3D _rotY = new(new Vector3D(0, 1, 0), 28);
        private readonly AxisAngleRotation3D _rotX = new(new Vector3D(1, 0, 0), -6);
        private System.Windows.Threading.DispatcherTimer? _idleTimer;

        public SkinViewer3D()
        {
            InitializeComponent();

            var tg = new Transform3DGroup();
            tg.Children.Add(new RotateTransform3D(_rotX));
            tg.Children.Add(new RotateTransform3D(_rotY));
            PlayerModel.Transform = tg;
            ResetView();

            Loaded += (_, _) => StartIdleAnimation();
            Unloaded += (_, _) => StopIdleAnimation();
        }

        public void ResetView()
        {
            _capePreviewMode = false;
            _angleY = 28;
            _angleX = -6;
            _cameraZ = 7.5;
            Camera.FieldOfView = 42;
            ApplyRotation();
            UpdateCamera();
        }

        public void FitAccountPreview(bool showCapeFromBack = false)
        {
            _capePreviewMode = showCapeFromBack;
            if (showCapeFromBack)
            {
                _angleY = 180;
                _angleX = -12;
                _cameraZ = 7.2;
                _idleTick = 0;
            }
            else
            {
                _angleY = 28;
                _angleX = -6;
                _cameraZ = 7.5;
            }

            Camera.FieldOfView = 42;
            ApplyRotation();
            UpdateCamera();
        }

        public void SetFrontView()
        {
            _angleY = 0;
            _angleX = -10;
            ApplyRotation();
        }

        public void SetBackView()
        {
            _angleY = 180;
            _angleX = -10;
            ApplyRotation();
        }

        public void ZoomIn()
        {
            _cameraZ = Math.Max(3.2, _cameraZ - 0.45);
            UpdateCamera();
        }

        public void ZoomOut()
        {
            _cameraZ = Math.Min(7.5, _cameraZ + 0.45);
            UpdateCamera();
        }

        private void ApplyRotation()
        {
            _rotY.Angle = _angleY;
            _rotX.Angle = _angleX;
        }

        private void UpdateCamera()
        {
            var position = new Point3D(0, ModelCenterY + 0.35, _cameraZ);
            var look = new Vector3D(0, ModelCenterY - position.Y, -_cameraZ);
            look.Normalize();

            Camera.Position = position;
            Camera.LookDirection = look;
            Camera.UpDirection = new Vector3D(0, 1, 0);
        }

        private void StartIdleAnimation()
        {
            if (_idleTimer != null) return;
            _idleTimer = new System.Windows.Threading.DispatcherTimer
            {
                Interval = TimeSpan.FromMilliseconds(42)
            };
            _idleTimer.Tick += (_, _) =>
            {
                if (_dragging) return;

                if (_capePreviewMode)
                {
                    _idleTick++;
                    _angleY = 180 + Math.Sin(_idleTick * 0.025) * 28;
                }
                else
                {
                    _angleY += 0.18;
                }

                _rotY.Angle = _angleY;
            };
            _idleTimer.Start();
        }

        private void StopIdleAnimation()
        {
            _idleTimer?.Stop();
            _idleTimer = null;
        }

        // ── Public API ────────────────────────────────────────────────────────

        public async Task LoadAppearanceAsync(string? skinUrl, string? capeUrl, bool isSlim, bool showCapeFromBack)
        {
            _isSlim = isSlim;
            _capePreviewMode = showCapeFromBack;

            var skinTask = DownloadTextureAsync(skinUrl, "https://mc-heads.net/skin/MHF_Steve");
            var capeTask = string.IsNullOrEmpty(capeUrl)
                ? Task.FromResult<BitmapSource?>(null)
                : DownloadTextureAsync(capeUrl, null);

            await Task.WhenAll(skinTask, capeTask);

            var skin = await skinTask;
            var cape = await capeTask;

            Dispatcher.Invoke(() =>
            {
                _currentCapeTexture = cape;
                if (skin != null)
                    BuildModel(skin);
                FitAccountPreview(showCapeFromBack);
            });
        }

        public async Task LoadSkinAsync(string? skinUrl)
        {
            var texture = await DownloadTextureAsync(skinUrl, "https://mc-heads.net/skin/MHF_Steve");
            if (texture != null)
                Dispatcher.Invoke(() => BuildModel(texture));
        }

        public async Task LoadCapeAsync(string? capeUrl)
        {
            _currentCapeTexture = string.IsNullOrEmpty(capeUrl)
                ? null
                : await DownloadTextureAsync(capeUrl, null);

            Dispatcher.Invoke(() =>
            {
                if (_currentSkinTexture != null)
                    BuildModel(_currentSkinTexture);
            });
        }

        private static async Task<BitmapSource?> DownloadTextureAsync(string? url, string? fallbackUrl)
        {
            if (!string.IsNullOrEmpty(url))
            {
                if (url.StartsWith("http://", StringComparison.OrdinalIgnoreCase))
                    url = "https://" + url.Substring(7);
                try
                {
                    var bytes = await Http.GetByteArrayAsync(url);
                    return await Task.Run(() => LoadBitmapFromBytes(bytes));
                }
                catch { /* try fallback */ }
            }

            if (string.IsNullOrEmpty(fallbackUrl))
                return null;

            try
            {
                var bytes = await Http.GetByteArrayAsync(fallbackUrl);
                return await Task.Run(() => LoadBitmapFromBytes(bytes));
            }
            catch
            {
                return null;
            }
        }

        public void LoadSkinFromFile(string filePath)
        {
            try
            {
                var bmp = new BitmapImage();
                bmp.BeginInit();
                bmp.UriSource   = new Uri(filePath, UriKind.Absolute);
                bmp.CacheOption = BitmapCacheOption.OnLoad;
                bmp.EndInit();
                bmp.Freeze();
                BuildModel(bmp);
                ResetView();
            }
            catch { }
        }

        public void SetModelVariant(bool isSlim)
        {
            _isSlim = isSlim;
            if (_currentSkinTexture != null)
                BuildModel(_currentSkinTexture);
        }

        public void ClearCape()
        {
            _currentCapeTexture = null;
            if (_currentSkinTexture != null)
                BuildModel(_currentSkinTexture);
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        private static BitmapImage LoadBitmapFromBytes(byte[] bytes)
        {
            var bmp = new BitmapImage();
            bmp.BeginInit();
            bmp.StreamSource = new MemoryStream(bytes);
            bmp.CacheOption  = BitmapCacheOption.OnLoad;
            bmp.EndInit();
            bmp.Freeze();
            return bmp;
        }

        // ── Model builder ─────────────────────────────────────────────────────

        private void BuildModel(BitmapSource rawSkin)
        {
            _currentSkinTexture = rawSkin;
            var skin = NormalizeSkin(rawSkin);

            // Upscale the normalized skin using C# Nearest Neighbor to bypass WPF bilinear rendering blurriness
            int upscaleFactor = 16;
            var upscaledSkin = UpscaleBitmapNearestNeighbor(skin, upscaleFactor);

            var g = new Model3DGroup();

            // ── HEAD (base) ──────────────────────────────────────────────────
            AddBox(g, upscaledSkin, upscaleFactor,
                xMin:-0.4, xMax:0.4, yMin:1.2, yMax:2.0, zMin:-0.4, zMax:0.4,
                front: R(8,8,16,16), back: R(24,8,32,16),
                left:  R(0,8,8,16),  right:R(16,8,24,16),
                top:   R(8,0,16,8),  bottom:R(16,0,24,8),
                isOverlay: false);

            // ── HEAD (overlay) ───────────────────────────────────────────────
            AddBox(g, upscaledSkin, upscaleFactor,
                xMin:-0.4, xMax:0.4, yMin:1.2, yMax:2.0, zMin:-0.4, zMax:0.4,
                front: R(40,8,48,16), back: R(56,8,64,16),
                left:  R(32,8,40,16), right:R(48,8,56,16),
                top:   R(40,0,48,8),  bottom:R(48,0,56,8),
                isOverlay: true);

            // ── BODY (base) ──────────────────────────────────────────────────
            AddBox(g, upscaledSkin, upscaleFactor,
                xMin:-0.4, xMax:0.4, yMin:0.0, yMax:1.2, zMin:-0.2, zMax:0.2,
                front: R(20,20,28,32), back: R(32,20,40,32),
                left:  R(16,20,20,32), right:R(28,20,32,32),
                top:   R(20,16,28,20), bottom:R(28,16,36,20),
                isOverlay: false);

            // ── BODY (overlay - jacket) ──────────────────────────────────────
            AddBox(g, upscaledSkin, upscaleFactor,
                xMin:-0.4, xMax:0.4, yMin:0.0, yMax:1.2, zMin:-0.2, zMax:0.2,
                front: R(20,36,28,48), back: R(32,36,40,48),
                left:  R(16,36,20,48), right:R(28,36,32,48),
                top:   R(20,32,28,36), bottom:R(28,32,36,36),
                isOverlay: true);

            if (_isSlim)
            {
                // ── RIGHT ARM (slim base - 3px wide) ─────────────────────────
                AddBox(g, upscaledSkin, upscaleFactor,
                    xMin:-0.7, xMax:-0.4, yMin:0.0, yMax:1.2, zMin:-0.2, zMax:0.2,
                    front: R(44,20,47,32), back: R(51,20,54,32),
                    left:  R(40,20,44,32), right:R(47,20,51,32),
                    top:   R(44,16,47,20), bottom:R(47,16,50,20),
                    isOverlay: false);

                // ── RIGHT ARM (slim overlay - sleeve) ────────────────────────
                AddBox(g, upscaledSkin, upscaleFactor,
                    xMin:-0.7, xMax:-0.4, yMin:0.0, yMax:1.2, zMin:-0.2, zMax:0.2,
                    front: R(44,36,47,48), back: R(51,36,54,48),
                    left:  R(40,36,44,48), right:R(47,36,51,48),
                    top:   R(44,32,47,36), bottom:R(47,32,50,36),
                    isOverlay: true);

                // ── LEFT ARM (slim base - 3px wide) ──────────────────────────
                AddBox(g, upscaledSkin, upscaleFactor,
                    xMin:0.4, xMax:0.7, yMin:0.0, yMax:1.2, zMin:-0.2, zMax:0.2,
                    front: R(36,52,39,64), back: R(43,52,46,64),
                    left:  R(32,52,36,64), right:R(39,52,43,64),
                    top:   R(36,48,39,52), bottom:R(39,48,42,52),
                    isOverlay: false);

                // ── LEFT ARM (slim overlay - sleeve) ─────────────────────────
                AddBox(g, upscaledSkin, upscaleFactor,
                    xMin:0.4, xMax:0.7, yMin:0.0, yMax:1.2, zMin:-0.2, zMax:0.2,
                    front: R(52,52,55,64), back: R(59,52,62,64),
                    left:  R(48,52,52,64), right:R(55,52,59,64),
                    top:   R(52,48,55,52), bottom:R(55,48,58,52),
                    isOverlay: true);
            }
            else
            {
                // ── RIGHT ARM (classic base - 4px wide) ──────────────────────
                AddBox(g, upscaledSkin, upscaleFactor,
                    xMin:-0.8, xMax:-0.4, yMin:0.0, yMax:1.2, zMin:-0.2, zMax:0.2,
                    front: R(44,20,48,32), back: R(52,20,56,32),
                    left:  R(40,20,44,32), right:R(48,20,52,32),
                    top:   R(44,16,48,20), bottom:R(48,16,52,20),
                    isOverlay: false);

                // ── RIGHT ARM (classic overlay - sleeve) ─────────────────────
                AddBox(g, upscaledSkin, upscaleFactor,
                    xMin:-0.8, xMax:-0.4, yMin:0.0, yMax:1.2, zMin:-0.2, zMax:0.2,
                    front: R(44,36,48,48), back: R(52,36,56,48),
                    left:  R(40,36,44,48), right:R(48,36,52,48),
                    top:   R(44,32,48,36), bottom:R(48,32,52,36),
                    isOverlay: true);

                // ── LEFT ARM (classic base - 4px wide) ───────────────────────
                AddBox(g, upscaledSkin, upscaleFactor,
                    xMin:0.4, xMax:0.8, yMin:0.0, yMax:1.2, zMin:-0.2, zMax:0.2,
                    front: R(36,52,40,64), back: R(44,52,48,64),
                    left:  R(32,52,36,64), right:R(40,52,44,64),
                    top:   R(36,48,40,52), bottom:R(40,48,44,52),
                    isOverlay: false);

                // ── LEFT ARM (classic overlay - sleeve) ──────────────────────
                AddBox(g, upscaledSkin, upscaleFactor,
                    xMin:0.4, xMax:0.8, yMin:0.0, yMax:1.2, zMin:-0.2, zMax:0.2,
                    front: R(52,52,56,64), back: R(60,52,64,64),
                    left:  R(48,52,52,64), right:R(56,52,60,64),
                    top:   R(52,48,56,52), bottom:R(56,48,60,52),
                    isOverlay: true);
            }

            // ── RIGHT LEG (base) ─────────────────────────────────────────────
            AddBox(g, upscaledSkin, upscaleFactor,
                xMin:-0.4, xMax:0.0, yMin:-1.2, yMax:0.0, zMin:-0.2, zMax:0.2,
                front: R(4,20,8,32),  back: R(12,20,16,32),
                left:  R(0,20,4,32),  right:R(8,20,12,32),
                top:   R(4,16,8,20),  bottom:R(8,16,12,20),
                isOverlay: false);

            // ── RIGHT LEG (overlay - pants) ──────────────────────────────────
            AddBox(g, upscaledSkin, upscaleFactor,
                xMin:-0.4, xMax:0.0, yMin:-1.2, yMax:0.0, zMin:-0.2, zMax:0.2,
                front: R(4,36,8,48),   back: R(12,36,16,48),
                left:  R(0,36,4,48),   right:R(8,36,12,48),
                top:   R(4,32,8,36),   bottom:R(8,32,12,36),
                isOverlay: true);

            // ── LEFT LEG (base) ──────────────────────────────────────────────
            AddBox(g, upscaledSkin, upscaleFactor,
                xMin:0.0, xMax:0.4, yMin:-1.2, yMax:0.0, zMin:-0.2, zMax:0.2,
                front: R(20,52,24,64), back: R(28,52,32,64),
                left:  R(16,52,20,64), right:R(24,52,28,64),
                top:   R(20,48,24,52), bottom:R(24,48,28,52),
                isOverlay: false);

            // ── LEFT LEG (overlay - pants) ───────────────────────────────────
            AddBox(g, upscaledSkin, upscaleFactor,
                xMin:0.0, xMax:0.4, yMin:-1.2, yMax:0.0, zMin:-0.2, zMax:0.2,
                front: R(4,52,8,64),   back: R(12,52,16,64),
                left:  R(0,52,4,64),   right:R(8,52,12,64),
                top:   R(4,48,8,52),   bottom:R(8,48,12,52),
                isOverlay: true);

            if (_currentCapeTexture != null)
                AddCape(g, NormalizeCape(_currentCapeTexture), upscaleFactor);

            PlayerModel.Content = g;
        }

        private static BitmapSource NormalizeCape(BitmapSource cape)
        {
            if (cape.PixelWidth == 64 && cape.PixelHeight == 32)
                return cape;

            if (cape.PixelWidth == 64 && cape.PixelHeight == 64)
            {
                var crop = new CroppedBitmap(cape, new Int32Rect(0, 0, 64, 32));
                crop.Freeze();
                return crop;
            }

            return cape;
        }

        private static void AddCape(Model3DGroup g, BitmapSource cape, int factor)
        {
            var upscaled = UpscaleBitmapNearestNeighbor(cape, factor);
            var main = Crop(upscaled, factor, R(1, 1, 11, 17), isOverlay: false);
            if (main == null)
                return;

            // Minecraft cape: 10×16 texels on a 64×32 sheet → 0.625 × 1.0 units, hangs from shoulders.
            const double halfW = 0.3125;
            const double topY = 1.17;
            const double bottomY = 0.17;
            // Slightly in front of the body back plane so it isn't hidden when viewed from behind.
            const double z = -0.175;

            // Single flat panel on the back (like in-game), drawn on both sides for preview rotation.
            AddFace(g, main,
                (-halfW, topY, z), (halfW, topY, z), (halfW, bottomY, z), (-halfW, bottomY, z));
            AddFace(g, main,
                (halfW, topY, z), (-halfW, topY, z), (-halfW, bottomY, z), (halfW, bottomY, z));
        }

        private static Int32Rect R(int x, int y, int x2, int y2)
            => new(x, y, x2 - x, y2 - y);

        // ── Normalizers & Format Conversions ──────────────────────────────────

        public static BitmapSource NormalizeSkin(BitmapSource skin)
        {
            if (skin.PixelWidth == 64 && skin.PixelHeight == 64)
                return skin;

            if (skin.PixelWidth == 64 && skin.PixelHeight == 32)
            {
                var visual = new DrawingVisual();
                using (var dc = visual.RenderOpen())
                {
                    dc.DrawImage(skin, new Rect(0, 0, 64, 32));

                    var rightLeg = new CroppedBitmap(skin, new Int32Rect(0, 16, 16, 16));
                    dc.DrawImage(rightLeg, new Rect(16, 48, 16, 16));

                    var rightArm = new CroppedBitmap(skin, new Int32Rect(40, 16, 16, 16));
                    dc.DrawImage(rightArm, new Rect(32, 48, 16, 16));
                }

                var rtb = new RenderTargetBitmap(64, 64, 96, 96, PixelFormats.Pbgra32);
                rtb.Render(visual);
                rtb.Freeze();
                return rtb;
            }

            if (skin.PixelWidth != 64)
            {
                var visual = new DrawingVisual();
                using (var dc = visual.RenderOpen())
                {
                    RenderOptions.SetBitmapScalingMode(visual, BitmapScalingMode.NearestNeighbor);
                    dc.DrawImage(skin, new Rect(0, 0, 64, 64));
                }
                var rtb = new RenderTargetBitmap(64, 64, 96, 96, PixelFormats.Pbgra32);
                rtb.Render(visual);
                rtb.Freeze();
                return rtb;
            }

            return skin;
        }

        // Perform programmatic Nearest Neighbor upscaling in C# memory (Bgra32 byte array)
        private static BitmapSource UpscaleBitmapNearestNeighbor(BitmapSource source, int factor)
        {
            BitmapSource bgraSource = source;
            if (bgraSource.Format != PixelFormats.Bgra32 && bgraSource.Format != PixelFormats.Pbgra32)
            {
                bgraSource = new FormatConvertedBitmap(bgraSource, PixelFormats.Bgra32, null, 0);
            }

            int srcW = bgraSource.PixelWidth;
            int srcH = bgraSource.PixelHeight;
            int dstW = srcW * factor;
            int dstH = srcH * factor;

            int srcStride = srcW * 4;
            byte[] srcPixels = new byte[srcH * srcStride];
            bgraSource.CopyPixels(srcPixels, srcStride, 0);

            int dstStride = dstW * 4;
            byte[] dstPixels = new byte[dstH * dstStride];

            for (int y = 0; y < dstH; y++)
            {
                int srcY = y / factor;
                int srcRowOffset = srcY * srcStride;
                int dstRowOffset = y * dstStride;

                for (int x = 0; x < dstW; x++)
                {
                    int srcX = x / factor;
                    int srcPixelIndex = srcRowOffset + srcX * 4;
                    int dstPixelIndex = dstRowOffset + x * 4;

                    dstPixels[dstPixelIndex]     = srcPixels[srcPixelIndex];     // B
                    dstPixels[dstPixelIndex + 1] = srcPixels[srcPixelIndex + 1]; // G
                    dstPixels[dstPixelIndex + 2] = srcPixels[srcPixelIndex + 2]; // R
                    dstPixels[dstPixelIndex + 3] = srcPixels[srcPixelIndex + 3]; // A
                }
            }

            var bitmap = BitmapSource.Create(
                dstW, dstH, 96, 96,
                PixelFormats.Bgra32, null,
                dstPixels, dstStride);
            
            bitmap.Freeze();
            return bitmap;
        }

        private static bool IsFaceTransparent(BitmapSource image)
        {
            int w = image.PixelWidth;
            int h = image.PixelHeight;
            int stroke = w * 4;
            byte[] pixels = new byte[w * h * 4];
            image.CopyPixels(pixels, stroke, 0);

            for (int i = 3; i < pixels.Length; i += 4)
            {
                if (pixels[i] > 10) return false;
            }
            return true;
        }

        private static ImageBrush? Crop(BitmapSource upscaledSkin, int factor, Int32Rect rect, bool isOverlay)
        {
            var scaledRect = new Int32Rect(rect.X * factor, rect.Y * factor, rect.Width * factor, rect.Height * factor);

            BitmapSource crop;
            try   { crop = new CroppedBitmap(upscaledSkin, scaledRect); }
            catch { return null; }

            if (isOverlay && IsFaceTransparent(crop))
                return null;

            return new ImageBrush(crop)
            {
                TileMode = TileMode.None,
                Stretch  = Stretch.Fill
            };
        }

        private static void AddBox(
            Model3DGroup g, BitmapSource upscaledSkin, int factor,
            double xMin, double xMax,
            double yMin, double yMax,
            double zMin, double zMax,
            Int32Rect front, Int32Rect back,
            Int32Rect left,  Int32Rect right,
            Int32Rect top,   Int32Rect bottom,
            bool isOverlay = false)
        {
            double offset = isOverlay ? 0.024 : 0.0;
            double x1 = xMin - offset;
            double x2 = xMax + offset;
            double y1 = yMin - offset;
            double y2 = yMax + offset;
            double z1 = zMin - offset;
            double z2 = zMax + offset;

            var fBrush = Crop(upscaledSkin, factor, front, isOverlay);
            if (fBrush != null) AddFace(g, fBrush, (x1,y2,z2), (x2,y2,z2), (x2,y1,z2), (x1,y1,z2));

            var bkBrush = Crop(upscaledSkin, factor, back, isOverlay);
            if (bkBrush != null) AddFace(g, bkBrush, (x2,y2,z1), (x1,y2,z1), (x1,y1,z1), (x2,y1,z1));

            var lBrush = Crop(upscaledSkin, factor, left, isOverlay);
            if (lBrush != null) AddFace(g, lBrush, (x1,y2,z1), (x1,y2,z2), (x1,y1,z2), (x1,y1,z1));

            var rBrush = Crop(upscaledSkin, factor, right, isOverlay);
            if (rBrush != null) AddFace(g, rBrush, (x2,y2,z2), (x2,y2,z1), (x2,y1,z1), (x2,y1,z2));

            var tpBrush = Crop(upscaledSkin, factor, top, isOverlay);
            if (tpBrush != null) AddFace(g, tpBrush, (x1,y2,z1), (x2,y2,z1), (x2,y2,z2), (x1,y2,z2));

            var btBrush = Crop(upscaledSkin, factor, bottom, isOverlay);
            if (btBrush != null) AddFace(g, btBrush, (x1,y1,z2), (x2,y1,z2), (x2,y1,z1), (x1,y1,z1));
        }

        private static void AddFace(
            Model3DGroup g, ImageBrush brush,
            (double x,double y,double z) p0,
            (double x,double y,double z) p1,
            (double x,double y,double z) p2,
            (double x,double y,double z) p3)
        {
            var mesh = new MeshGeometry3D();
            mesh.Positions.Add(new Point3D(p0.x, p0.y, p0.z));
            mesh.Positions.Add(new Point3D(p1.x, p1.y, p1.z));
            mesh.Positions.Add(new Point3D(p2.x, p2.y, p2.z));
            mesh.Positions.Add(new Point3D(p3.x, p3.y, p3.z));

            mesh.TextureCoordinates.Add(new Point(0, 0));
            mesh.TextureCoordinates.Add(new Point(1, 0));
            mesh.TextureCoordinates.Add(new Point(1, 1));
            mesh.TextureCoordinates.Add(new Point(0, 1));

            mesh.TriangleIndices.Add(0); mesh.TriangleIndices.Add(3); mesh.TriangleIndices.Add(2);
            mesh.TriangleIndices.Add(0); mesh.TriangleIndices.Add(2); mesh.TriangleIndices.Add(1);

            var mat = new DiffuseMaterial(brush) { AmbientColor = Colors.White };
            g.Children.Add(new GeometryModel3D(mesh, mat));
        }

        // ── Mouse drag rotation ───────────────────────────────────────────────

        private void OnMouseDown(object sender, MouseButtonEventArgs e)
        {
            if (e.LeftButton != MouseButtonState.Pressed) return;
            _dragging = true;
            _lastPos  = e.GetPosition(this);
            Viewport.CaptureMouse();
            e.Handled = true;
        }

        private void OnMouseMove(object sender, MouseEventArgs e)
        {
            if (!_dragging) return;
            var pos = e.GetPosition(this);
            _angleY += (pos.X - _lastPos.X) * 0.7;
            _angleX  = Math.Clamp(_angleX + (pos.Y - _lastPos.Y) * 0.35, -40, 40);
            _lastPos = pos;
            _rotY.Angle = _angleY;
            _rotX.Angle = _angleX;
        }

        private void OnMouseUp(object sender, MouseButtonEventArgs e)
        {
            _dragging = false;
            Viewport.ReleaseMouseCapture();
        }
    }
}
