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
    /// </summary>
    public static class Minecraft3DModelBuilder
    {
        private static Material? _grassTopMat;
        private static Material? _grassSideMat;
        private static Material? _dirtMat;
        private static Material? _paperMat;
        private static Material? _paperEdgeMat;
        private static Material? _foxMat;
        private static Material? _anvilTopMat;
        private static Material? _anvilSideMat;

        private static void EnsureMaterials()
        {
            if (_grassTopMat != null) return;

            _grassTopMat = LoadMaterial("grass_top.png");
            _grassSideMat = LoadMaterial("grass_side.png");
            _dirtMat = LoadMaterial("dirt.png");
            _paperMat = LoadMaterial("paper.png");
            _paperEdgeMat = new DiffuseMaterial(new SolidColorBrush(Color.FromRgb(220, 215, 202)));
            _foxMat = LoadMaterial("fox.png");
            _anvilTopMat = LoadMaterial("anvil_top.png");
            _anvilSideMat = LoadMaterial("anvil_side.png");
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
                    {
                        localPath = Path.Combine(Directory.GetCurrentDirectory(), "Launcher", "Assets", "3d", filename);
                    }

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

            const double s = 0.95; // half size -> 1.9 total cube

            // Top (+Y)
            AddFace(group,
                new Point3D(-s, s, s), new Point3D(s, s, s), new Point3D(s, s, -s), new Point3D(-s, s, -s),
                _grassTopMat!);

            // Bottom (-Y)
            AddFace(group,
                new Point3D(-s, -s, -s), new Point3D(s, -s, -s), new Point3D(s, -s, s), new Point3D(-s, -s, s),
                _dirtMat!);

            // 4 Sides with grass_side
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

        #region Paper Item Sheet (Fabric)

        public static Model3D CreatePaperModel()
        {
            EnsureMaterials();
            var group = new Model3DGroup();

            const double hw = 0.95;  // half width
            const double hh = 1.25;  // half height
            const double hd = 0.025; // thin sheet depth

            // Front (+Z)
            AddFace(group,
                new Point3D(-hw, -hh, hd), new Point3D(hw, -hh, hd), new Point3D(hw, hh, hd), new Point3D(-hw, hh, hd),
                _paperMat!);

            // Back (-Z)
            AddFace(group,
                new Point3D(hw, -hh, -hd), new Point3D(-hw, -hh, -hd), new Point3D(-hw, hh, -hd), new Point3D(hw, hh, -hd),
                _paperMat!);

            // Thin Edges
            // Top (+Y)
            AddFace(group,
                new Point3D(-hw, hh, hd), new Point3D(hw, hh, hd), new Point3D(hw, hh, -hd), new Point3D(-hw, hh, -hd),
                _paperEdgeMat!);

            // Bottom (-Y)
            AddFace(group,
                new Point3D(-hw, -hh, -hd), new Point3D(hw, -hh, -hd), new Point3D(hw, -hh, hd), new Point3D(-hw, -hh, hd),
                _paperEdgeMat!);

            // Left (-X)
            AddFace(group,
                new Point3D(-hw, -hh, -hd), new Point3D(-hw, -hh, hd), new Point3D(-hw, hh, hd), new Point3D(-hw, hh, -hd),
                _paperEdgeMat!);

            // Right (+X)
            AddFace(group,
                new Point3D(hw, -hh, hd), new Point3D(hw, -hh, -hd), new Point3D(hw, hh, -hd), new Point3D(hw, hh, hd),
                _paperEdgeMat!);

            return group;
        }

        #endregion

        #region Fox Head (NeoForge)

        public static Model3D CreateFoxHeadModel()
        {
            EnsureMaterials();
            var group = new Model3DGroup();
            var mat = _foxMat!;

            const double tw = 48.0;
            const double th = 32.0;
            const double sc = 0.22;

            // 1. Head Box: 8 x 6 x 6
            AddBoxUV(group,
                -4 * sc, -3 * sc, -3 * sc,
                 4 * sc,  3 * sc,  3 * sc,
                mat,
                new Rect(7 / tw, 5 / th, 8 / tw, 6 / th),     // Top
                new Rect(15 / tw, 5 / th, 8 / tw, 6 / th),    // Bottom
                new Rect(7 / tw, 11 / th, 8 / tw, 6 / th),    // Front (+Z)
                new Rect(21 / tw, 11 / th, 8 / tw, 6 / th),   // Back (-Z)
                new Rect(1 / tw, 11 / th, 6 / tw, 6 / th),    // Left (-X)
                new Rect(15 / tw, 11 / th, 6 / tw, 6 / th)    // Right (+X)
            );

            // 2. Snout Box: 4 x 2 x 3
            AddBoxUV(group,
                -2 * sc, -3 * sc,  3 * sc,
                 2 * sc, -1 * sc,  6 * sc,
                mat,
                new Rect(9 / tw, 18 / th, 4 / tw, 3 / th),
                new Rect(13 / tw, 18 / th, 4 / tw, 3 / th),
                new Rect(9 / tw, 21 / th, 4 / tw, 2 / th),
                new Rect(16 / tw, 21 / th, 4 / tw, 2 / th),
                new Rect(6 / tw, 21 / th, 3 / tw, 2 / th),
                new Rect(13 / tw, 21 / th, 3 / tw, 2 / th)
            );

            // 3. Right Ear: 2 x 2 x 1 (+X, +Y)
            AddBoxUV(group,
                 2 * sc, 3 * sc, 0 * sc,
                 4 * sc, 5 * sc, 1 * sc,
                mat,
                new Rect(9 / tw, 1 / th, 2 / tw, 1 / th),
                new Rect(11 / tw, 1 / th, 2 / tw, 1 / th),
                new Rect(9 / tw, 2 / th, 2 / tw, 2 / th),
                new Rect(12 / tw, 2 / th, 2 / tw, 2 / th),
                new Rect(8 / tw, 2 / th, 1 / tw, 2 / th),
                new Rect(11 / tw, 2 / th, 1 / tw, 2 / th)
            );

            // 4. Left Ear: 2 x 2 x 1 (-X, +Y)
            AddBoxUV(group,
                -4 * sc, 3 * sc, 0 * sc,
                -2 * sc, 5 * sc, 1 * sc,
                mat,
                new Rect(16 / tw, 1 / th, 2 / tw, 1 / th),
                new Rect(18 / tw, 1 / th, 2 / tw, 1 / th),
                new Rect(16 / tw, 2 / th, 2 / tw, 2 / th),
                new Rect(19 / tw, 2 / th, 2 / tw, 2 / th),
                new Rect(15 / tw, 2 / th, 1 / tw, 2 / th),
                new Rect(18 / tw, 2 / th, 1 / tw, 2 / th)
            );

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

        private static void AddFaceUV(Model3DGroup group, Point3D p0, Point3D p1, Point3D p2, Point3D p3, Material mat, Rect uv)
        {
            var mesh = new MeshGeometry3D();
            mesh.Positions.Add(p0);
            mesh.Positions.Add(p1);
            mesh.Positions.Add(p2);
            mesh.Positions.Add(p3);

            mesh.TextureCoordinates.Add(new Point(uv.Left, uv.Bottom));
            mesh.TextureCoordinates.Add(new Point(uv.Right, uv.Bottom));
            mesh.TextureCoordinates.Add(new Point(uv.Right, uv.Top));
            mesh.TextureCoordinates.Add(new Point(uv.Left, uv.Top));

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

        private static void AddBoxUV(Model3DGroup group,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            Material mat,
            Rect topUV, Rect bottomUV, Rect frontUV, Rect backUV, Rect leftUV, Rect rightUV)
        {
            AddFaceUV(group, new Point3D(minX, maxY, maxZ), new Point3D(maxX, maxY, maxZ), new Point3D(maxX, maxY, minZ), new Point3D(minX, maxY, minZ), mat, topUV);
            AddFaceUV(group, new Point3D(minX, minY, minZ), new Point3D(maxX, minY, minZ), new Point3D(maxX, minY, maxZ), new Point3D(minX, minY, maxZ), mat, bottomUV);
            AddFaceUV(group, new Point3D(minX, minY, maxZ), new Point3D(maxX, minY, maxZ), new Point3D(maxX, maxY, maxZ), new Point3D(minX, maxY, maxZ), mat, frontUV);
            AddFaceUV(group, new Point3D(maxX, minY, minZ), new Point3D(minX, minY, minZ), new Point3D(minX, maxY, minZ), new Point3D(maxX, maxY, minZ), mat, backUV);
            AddFaceUV(group, new Point3D(minX, minY, minZ), new Point3D(minX, minY, maxZ), new Point3D(minX, maxY, maxZ), new Point3D(minX, maxY, minZ), mat, leftUV);
            AddFaceUV(group, new Point3D(maxX, minY, maxZ), new Point3D(maxX, minY, minZ), new Point3D(maxX, maxY, minZ), new Point3D(maxX, maxY, maxZ), mat, rightUV);
        }

        #endregion
    }
}
