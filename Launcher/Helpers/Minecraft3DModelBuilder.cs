using System;
using System.IO;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Media.Media3D;

namespace Launcher.Helpers
{
    /// <summary>
    /// Builds authentic Minecraft 3D voxel models (Grass Block, Paper, Fox Head, Anvil)
    /// rendered inside WPF Viewport3D with sharp NearestNeighbor pixel-art textures.
    /// Each fox face uses a pre-cropped, upscaled per-face texture for accurate UV rendering.
    /// </summary>
    public static class Minecraft3DModelBuilder
    {
        private static Material? _grassTopMat;
        private static Material? _grassSideMat;
        private static Material? _dirtMat;
        private static Material? _paperMat;
        private static Material? _anvilTopMat;
        private static Material? _anvilSideMat;

        // Fox head per-face materials (pre-cropped textures)
        private static Material? _foxHeadFront;
        private static Material? _foxHeadBack;
        private static Material? _foxHeadTop;
        private static Material? _foxHeadBottom;
        private static Material? _foxHeadLeft;
        private static Material? _foxHeadRight;

        // Fox snout per-face materials
        private static Material? _foxSnoutFront;
        private static Material? _foxSnoutTop;
        private static Material? _foxSnoutBottom;
        private static Material? _foxSnoutLeft;
        private static Material? _foxSnoutRight;

        // Fox ear materials
        private static Material? _foxEarFront;
        private static Material? _foxEarBack;
        private static Material? _foxEarSide;

        private static bool _materialsLoaded = false;

        private static void EnsureMaterials()
        {
            if (_materialsLoaded) return;
            _materialsLoaded = true;

            _grassTopMat  = LoadMaterial("grass_top.png");
            _grassSideMat = LoadMaterial("grass_side.png");
            _dirtMat      = LoadMaterial("dirt.png");
            _paperMat     = LoadMaterial("paper.png");
            _anvilTopMat  = LoadMaterial("anvil_top.png");
            _anvilSideMat = LoadMaterial("anvil_side.png");

            // Fox face materials
            _foxHeadFront  = LoadMaterial("fox_head_front.png");
            _foxHeadBack   = LoadMaterial("fox_head_back.png");
            _foxHeadTop    = LoadMaterial("fox_head_top.png");
            _foxHeadBottom = LoadMaterial("fox_head_bottom.png");
            _foxHeadLeft   = LoadMaterial("fox_head_left.png");
            _foxHeadRight  = LoadMaterial("fox_head_right.png");

            _foxSnoutFront  = LoadMaterial("fox_snout_front.png");
            _foxSnoutTop    = LoadMaterial("fox_snout_top.png");
            _foxSnoutBottom = LoadMaterial("fox_snout_bottom.png");
            _foxSnoutLeft   = LoadMaterial("fox_snout_left.png");
            _foxSnoutRight  = LoadMaterial("fox_snout_right.png");

            _foxEarFront = LoadMaterial("fox_ear_right.png");
            _foxEarBack  = LoadMaterial("fox_ear_back.png");
            _foxEarSide  = LoadMaterial("fox_ear_side.png");
        }

        private static Material LoadMaterial(string filename)
        {
            try
            {
                var uri = new Uri("pack://application:,,,/Assets/3d/" + filename, UriKind.RelativeOrAbsolute);
                var bmp = new BitmapImage();
                bmp.BeginInit();
                bmp.UriSource = uri;
                bmp.CacheOption = BitmapCacheOption.OnLoad;
                bmp.EndInit();
                bmp.Freeze();

                var brush = new ImageBrush(bmp);
                RenderOptions.SetBitmapScalingMode(brush, BitmapScalingMode.NearestNeighbor);
                RenderOptions.SetEdgeMode(brush, EdgeMode.Aliased);
                brush.Freeze();

                return new DiffuseMaterial(brush);
            }
            catch
            {
                try
                {
                    var localPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Assets", "3d", filename);
                    if (!File.Exists(localPath))
                        localPath = Path.Combine(Directory.GetCurrentDirectory(), "Launcher", "Assets", "3d", filename);

                    if (File.Exists(localPath))
                    {
                        var bmp = new BitmapImage();
                        bmp.BeginInit();
                        bmp.UriSource = new Uri(localPath, UriKind.Absolute);
                        bmp.CacheOption = BitmapCacheOption.OnLoad;
                        bmp.EndInit();
                        bmp.Freeze();

                        var brush = new ImageBrush(bmp);
                        RenderOptions.SetBitmapScalingMode(brush, BitmapScalingMode.NearestNeighbor);
                        RenderOptions.SetEdgeMode(brush, EdgeMode.Aliased);
                        brush.Freeze();

                        return new DiffuseMaterial(brush);
                    }
                }
                catch { }

                return new DiffuseMaterial(new SolidColorBrush(Colors.Gray));
            }
        }

        public static Model3D CreateModelForLoader(string? loader)
        {
            EnsureMaterials();

            var normalized = (loader ?? "").Trim().ToLowerInvariant();
            return normalized switch
            {
                "fabric" or "quilt" => CreatePaperModel(),
                "neoforge"          => CreateFoxHeadModel(),
                "forge"             => CreateAnvilModel(),
                _                   => CreateGrassBlockModel()
            };
        }

        #region Grass Block (Vanilla)

        public static Model3D CreateGrassBlockModel()
        {
            EnsureMaterials();
            var group = new Model3DGroup();

            const double s = 0.95; // half size → 1.9 total cube

            // Top (+Y)
            AddFace(group,
                new Point3D(-s, s, s), new Point3D(s, s, s), new Point3D(s, s, -s), new Point3D(-s, s, -s),
                _grassTopMat!);

            // Bottom (-Y)
            AddFace(group,
                new Point3D(-s, -s, -s), new Point3D(s, -s, -s), new Point3D(s, -s, s), new Point3D(-s, -s, s),
                _dirtMat!);

            // Front (+Z)
            AddFace(group,
                new Point3D(-s, -s, s), new Point3D(s, -s, s), new Point3D(s, s, s), new Point3D(-s, s, s),
                _grassSideMat!);

            // Back (-Z)
            AddFace(group,
                new Point3D(s, -s, -s), new Point3D(-s, -s, -s), new Point3D(-s, s, -s), new Point3D(s, s, -s),
                _grassSideMat!);

            // Left (-X)
            AddFace(group,
                new Point3D(-s, -s, -s), new Point3D(-s, -s, s), new Point3D(-s, s, s), new Point3D(-s, s, -s),
                _grassSideMat!);

            // Right (+X)
            AddFace(group,
                new Point3D(s, -s, s), new Point3D(s, -s, -s), new Point3D(s, s, -s), new Point3D(s, s, s),
                _grassSideMat!);

            return group;
        }

        #endregion

        #region Paper Item Sheet (Fabric/Quilt)
        // Authentic 3D Voxel Extrusion: each pixel has real depth (thickness),
        // with front/back faces and boundary edge quads sampled from paper.png.

        private static readonly ushort[] PaperOpaqueMask = new ushort[]
        {
            0x0000, // row  0
            0x0000, // row  1
            0x0060, // row  2
            0x00F0, // row  3
            0x03F8, // row  4
            0x07FC, // row  5
            0x1FFE, // row  6
            0x3FFF, // row  7
            0x7FFE, // row  8
            0x3FFC, // row  9
            0x1FF8, // row 10
            0x0FE0, // row 11
            0x07C0, // row 12
            0x0300, // row 13
            0x0000, // row 14
            0x0000  // row 15
        };

        public static Model3D CreatePaperModel()
        {
            EnsureMaterials();
            var group = new Model3DGroup();
            var mesh = new MeshGeometry3D();

            const double totalSize = 2.15;
            const double ps = totalSize / 16.0;
            const double halfDepth = 0.055; // tactile 3D thickness
            const double originOffset = totalSize / 2.0;

            bool IsOpaque(int x, int y) =>
                x >= 0 && x < 16 && y >= 0 && y < 16 &&
                (PaperOpaqueMask[y] & (1 << (15 - x))) != 0;

            void AddQuad(Point3D p0, Point3D p1, Point3D p2, Point3D p3,
                         Point uv0, Point uv1, Point uv2, Point uv3)
            {
                int baseIndex = mesh.Positions.Count;
                mesh.Positions.Add(p0);
                mesh.Positions.Add(p1);
                mesh.Positions.Add(p2);
                mesh.Positions.Add(p3);

                mesh.TextureCoordinates.Add(uv0);
                mesh.TextureCoordinates.Add(uv1);
                mesh.TextureCoordinates.Add(uv2);
                mesh.TextureCoordinates.Add(uv3);

                mesh.TriangleIndices.Add(baseIndex);
                mesh.TriangleIndices.Add(baseIndex + 1);
                mesh.TriangleIndices.Add(baseIndex + 2);

                mesh.TriangleIndices.Add(baseIndex);
                mesh.TriangleIndices.Add(baseIndex + 2);
                mesh.TriangleIndices.Add(baseIndex + 3);
            }

            for (int y = 0; y < 16; y++)
            {
                for (int x = 0; x < 16; x++)
                {
                    if (!IsOpaque(x, y)) continue;

                    double x0 = x * ps - originOffset;
                    double x1 = x0 + ps;
                    double y1 = -(y * ps - originOffset);
                    double y0 = y1 - ps;

                    double u0 = x / 16.0;
                    double u1 = (x + 1) / 16.0;
                    double v0 = y / 16.0;
                    double v1 = (y + 1) / 16.0;

                    var uvCenter = new Point((x + 0.5) / 16.0, (y + 0.5) / 16.0);

                    // Front face (+Z)
                    AddQuad(
                        new Point3D(x0, y0, halfDepth),
                        new Point3D(x1, y0, halfDepth),
                        new Point3D(x1, y1, halfDepth),
                        new Point3D(x0, y1, halfDepth),
                        new Point(u0, v1), new Point(u1, v1), new Point(u1, v0), new Point(u0, v0));

                    // Back face (-Z)
                    AddQuad(
                        new Point3D(x1, y0, -halfDepth),
                        new Point3D(x0, y0, -halfDepth),
                        new Point3D(x0, y1, -halfDepth),
                        new Point3D(x1, y1, -halfDepth),
                        new Point(u1, v1), new Point(u0, v1), new Point(u0, v0), new Point(u1, v0));

                    // Top (+Y)
                    if (!IsOpaque(x, y - 1))
                    {
                        AddQuad(
                            new Point3D(x0, y1, halfDepth),
                            new Point3D(x1, y1, halfDepth),
                            new Point3D(x1, y1, -halfDepth),
                            new Point3D(x0, y1, -halfDepth),
                            uvCenter, uvCenter, uvCenter, uvCenter);
                    }

                    // Bottom (-Y)
                    if (!IsOpaque(x, y + 1))
                    {
                        AddQuad(
                            new Point3D(x0, y0, -halfDepth),
                            new Point3D(x1, y0, -halfDepth),
                            new Point3D(x1, y0, halfDepth),
                            new Point3D(x0, y0, halfDepth),
                            uvCenter, uvCenter, uvCenter, uvCenter);
                    }

                    // Left (-X)
                    if (!IsOpaque(x - 1, y))
                    {
                        AddQuad(
                            new Point3D(x0, y0, -halfDepth),
                            new Point3D(x0, y0, halfDepth),
                            new Point3D(x0, y1, halfDepth),
                            new Point3D(x0, y1, -halfDepth),
                            uvCenter, uvCenter, uvCenter, uvCenter);
                    }

                    // Right (+X)
                    if (!IsOpaque(x + 1, y))
                    {
                        AddQuad(
                            new Point3D(x1, y0, halfDepth),
                            new Point3D(x1, y0, -halfDepth),
                            new Point3D(x1, y1, -halfDepth),
                            new Point3D(x1, y1, halfDepth),
                            uvCenter, uvCenter, uvCenter, uvCenter);
                    }
                }
            }

            group.Children.Add(new GeometryModel3D(mesh, _paperMat!));
            return group;
        }

        #endregion

        #region Fox Head (NeoForge)
        // Uses per-face pre-cropped textures for pixel-perfect UV mapping.
        // No full-texture UV slicing → no repetition/tiling artifacts.

        public static Model3D CreateFoxHeadModel()
        {
            EnsureMaterials();
            var group = new Model3DGroup();

            const double sc = 0.22;

            // ── 1. HEAD BOX: 8w × 6h × 6d (in Minecraft pixels × sc) ──
            double hx = 4 * sc, hy = 3 * sc, hz = 3 * sc;

            // Front (+Z)
            AddFace(group,
                new Point3D(-hx, -hy, hz), new Point3D(hx, -hy, hz), new Point3D(hx, hy, hz), new Point3D(-hx, hy, hz),
                _foxHeadFront!);
            // Back (-Z)
            AddFace(group,
                new Point3D(hx, -hy, -hz), new Point3D(-hx, -hy, -hz), new Point3D(-hx, hy, -hz), new Point3D(hx, hy, -hz),
                _foxHeadBack!);
            // Top (+Y)
            AddFace(group,
                new Point3D(-hx, hy, hz), new Point3D(hx, hy, hz), new Point3D(hx, hy, -hz), new Point3D(-hx, hy, -hz),
                _foxHeadTop!);
            // Bottom (-Y)
            AddFace(group,
                new Point3D(-hx, -hy, -hz), new Point3D(hx, -hy, -hz), new Point3D(hx, -hy, hz), new Point3D(-hx, -hy, hz),
                _foxHeadBottom!);
            // Left (-X)
            AddFace(group,
                new Point3D(-hx, -hy, -hz), new Point3D(-hx, -hy, hz), new Point3D(-hx, hy, hz), new Point3D(-hx, hy, -hz),
                _foxHeadLeft!);
            // Right (+X)
            AddFace(group,
                new Point3D(hx, -hy, hz), new Point3D(hx, -hy, -hz), new Point3D(hx, hy, -hz), new Point3D(hx, hy, hz),
                _foxHeadRight!);

            // ── 2. SNOUT BOX: 4w × 2h × 3d — protrudes forward from face ──
            double sx = 2 * sc, sy_bot = -3 * sc, sy_top = -1 * sc, sz_back = 3 * sc, sz_front = 6 * sc;

            AddFace(group,
                new Point3D(-sx, sy_bot, sz_front), new Point3D(sx, sy_bot, sz_front), new Point3D(sx, sy_top, sz_front), new Point3D(-sx, sy_top, sz_front),
                _foxSnoutFront!);
            AddFace(group,
                new Point3D(sx, sy_bot, sz_back), new Point3D(-sx, sy_bot, sz_back), new Point3D(-sx, sy_top, sz_back), new Point3D(sx, sy_top, sz_back),
                _foxSnoutBottom!); // back face (connects to head)
            AddFace(group,
                new Point3D(-sx, sy_top, sz_front), new Point3D(sx, sy_top, sz_front), new Point3D(sx, sy_top, sz_back), new Point3D(-sx, sy_top, sz_back),
                _foxSnoutTop!);
            AddFace(group,
                new Point3D(-sx, sy_bot, sz_back), new Point3D(sx, sy_bot, sz_back), new Point3D(sx, sy_bot, sz_front), new Point3D(-sx, sy_bot, sz_front),
                _foxSnoutBottom!);
            AddFace(group,
                new Point3D(-sx, sy_bot, sz_back), new Point3D(-sx, sy_bot, sz_front), new Point3D(-sx, sy_top, sz_front), new Point3D(-sx, sy_top, sz_back),
                _foxSnoutLeft!);
            AddFace(group,
                new Point3D(sx, sy_bot, sz_front), new Point3D(sx, sy_bot, sz_back), new Point3D(sx, sy_top, sz_back), new Point3D(sx, sy_top, sz_front),
                _foxSnoutRight!);

            // ── 3. RIGHT EAR: 2w × 2h × 1d (+X side) ──
            double erx0 = 2 * sc, erx1 = 4 * sc;
            double ery0 = 3 * sc, ery1 = 5 * sc;
            double erz0 = -0.5 * sc, erz1 = 0.5 * sc;

            AddFace(group,
                new Point3D(erx0, ery0, erz1), new Point3D(erx1, ery0, erz1), new Point3D(erx1, ery1, erz1), new Point3D(erx0, ery1, erz1),
                _foxEarFront!);
            AddFace(group,
                new Point3D(erx1, ery0, erz0), new Point3D(erx0, ery0, erz0), new Point3D(erx0, ery1, erz0), new Point3D(erx1, ery1, erz0),
                _foxEarBack!);
            AddFace(group,
                new Point3D(erx0, ery0, erz0), new Point3D(erx0, ery0, erz1), new Point3D(erx0, ery1, erz1), new Point3D(erx0, ery1, erz0),
                _foxEarSide!);
            AddFace(group,
                new Point3D(erx1, ery0, erz1), new Point3D(erx1, ery0, erz0), new Point3D(erx1, ery1, erz0), new Point3D(erx1, ery1, erz1),
                _foxEarSide!);

            // ── 4. LEFT EAR: 2w × 2h × 1d (-X side) ──
            double elx0 = -4 * sc, elx1 = -2 * sc;
            double ely0 = 3 * sc, ely1 = 5 * sc;
            double elz0 = -0.5 * sc, elz1 = 0.5 * sc;

            AddFace(group,
                new Point3D(elx0, ely0, elz1), new Point3D(elx1, ely0, elz1), new Point3D(elx1, ely1, elz1), new Point3D(elx0, ely1, elz1),
                _foxEarFront!);
            AddFace(group,
                new Point3D(elx1, ely0, elz0), new Point3D(elx0, ely0, elz0), new Point3D(elx0, ely1, elz0), new Point3D(elx1, ely1, elz0),
                _foxEarBack!);
            AddFace(group,
                new Point3D(elx0, ely0, elz0), new Point3D(elx0, ely0, elz1), new Point3D(elx0, ely1, elz1), new Point3D(elx0, ely1, elz0),
                _foxEarSide!);
            AddFace(group,
                new Point3D(elx1, ely0, elz1), new Point3D(elx1, ely0, elz0), new Point3D(elx1, ely1, elz0), new Point3D(elx1, ely1, elz1),
                _foxEarSide!);

            return group;
        }

        #endregion

        #region Anvil (Forge)

        public static Model3D CreateAnvilModel()
        {
            EnsureMaterials();
            var group = new Model3DGroup();

            // Base
            AddBoxSimple(group, -0.8, -0.85, -0.6, 0.8, -0.5, 0.6, _anvilSideMat!);

            // Waist
            AddBoxSimple(group, -0.25, -0.5, -0.25, 0.25, 0.1, 0.25, _anvilSideMat!);

            // Head
            const double minX = -1.1, minY = 0.1, minZ = -0.55;
            const double maxX =  1.1, maxY = 0.75, maxZ =  0.55;

            // Top (+Y)
            AddFace(group,
                new Point3D(minX, maxY, maxZ), new Point3D(maxX, maxY, maxZ), new Point3D(maxX, maxY, minZ), new Point3D(minX, maxY, minZ),
                _anvilTopMat!);

            // Bottom (-Y)
            AddFace(group,
                new Point3D(minX, minY, minZ), new Point3D(maxX, minY, minZ), new Point3D(maxX, minY, maxZ), new Point3D(minX, minY, maxZ),
                _anvilSideMat!);

            // Front (+Z)
            AddFace(group,
                new Point3D(minX, minY, maxZ), new Point3D(maxX, minY, maxZ), new Point3D(maxX, maxY, maxZ), new Point3D(minX, maxY, maxZ),
                _anvilSideMat!);

            // Back (-Z)
            AddFace(group,
                new Point3D(maxX, minY, minZ), new Point3D(minX, minY, minZ), new Point3D(minX, maxY, minZ), new Point3D(maxX, maxY, minZ),
                _anvilSideMat!);

            // Left (-X)
            AddFace(group,
                new Point3D(minX, minY, minZ), new Point3D(minX, minY, maxZ), new Point3D(minX, maxY, maxZ), new Point3D(minX, maxY, minZ),
                _anvilSideMat!);

            // Right (+X)
            AddFace(group,
                new Point3D(maxX, minY, maxZ), new Point3D(maxX, minY, minZ), new Point3D(maxX, maxY, minZ), new Point3D(maxX, maxY, maxZ),
                _anvilSideMat!);

            return group;
        }

        #endregion

        #region Geometry Utilities

        private static void AddFace(Model3DGroup group, Point3D p0, Point3D p1, Point3D p2, Point3D p3, Material mat)
        {
            var mesh = new MeshGeometry3D();
            mesh.Positions.Add(p0);
            mesh.Positions.Add(p1);
            mesh.Positions.Add(p2);
            mesh.Positions.Add(p3);

            mesh.TextureCoordinates.Add(new Point(0, 1));
            mesh.TextureCoordinates.Add(new Point(1, 1));
            mesh.TextureCoordinates.Add(new Point(1, 0));
            mesh.TextureCoordinates.Add(new Point(0, 0));

            mesh.TriangleIndices.Add(0);
            mesh.TriangleIndices.Add(1);
            mesh.TriangleIndices.Add(2);

            mesh.TriangleIndices.Add(0);
            mesh.TriangleIndices.Add(2);
            mesh.TriangleIndices.Add(3);

            group.Children.Add(new GeometryModel3D(mesh, mat));
        }

        private static void AddBoxSimple(Model3DGroup group, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Material mat)
        {
            AddFace(group, new Point3D(minX, maxY, maxZ), new Point3D(maxX, maxY, maxZ), new Point3D(maxX, maxY, minZ), new Point3D(minX, maxY, minZ), mat);
            AddFace(group, new Point3D(minX, minY, minZ), new Point3D(maxX, minY, minZ), new Point3D(maxX, minY, maxZ), new Point3D(minX, minY, maxZ), mat);
            AddFace(group, new Point3D(minX, minY, maxZ), new Point3D(maxX, minY, maxZ), new Point3D(maxX, maxY, maxZ), new Point3D(minX, maxY, maxZ), mat);
            AddFace(group, new Point3D(maxX, minY, minZ), new Point3D(minX, minY, minZ), new Point3D(minX, maxY, minZ), new Point3D(maxX, maxY, minZ), mat);
            AddFace(group, new Point3D(minX, minY, minZ), new Point3D(minX, minY, maxZ), new Point3D(minX, maxY, maxZ), new Point3D(minX, maxY, minZ), mat);
            AddFace(group, new Point3D(maxX, minY, maxZ), new Point3D(maxX, minY, minZ), new Point3D(maxX, maxY, minZ), new Point3D(maxX, maxY, maxZ), mat);
        }

        #endregion
    }
}
