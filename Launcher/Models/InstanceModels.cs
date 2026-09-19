using System;
using System.Collections.Generic;

namespace Launcher.Models
{
    public class MinecraftInstance
    {
        public string Id { get; set; } = Guid.NewGuid().ToString("N");
        public string Name { get; set; } = "New Instance";
        public string MinecraftVersion { get; set; } = "1.20.1";
        public string Loader { get; set; } = "Fabric";
        public string LoaderVersion { get; set; } = "";
        public string IconColor { get; set; } = "#0A84FF";
        public string IconLetter { get; set; } = "F";
        public bool IsFavorite { get; set; }
        public List<string> Tags { get; set; } = new();
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
        public DateTime? LastPlayedAt { get; set; }
        public string JavaPath { get; set; } = "";
        public int RamMb { get; set; } = 4096;
        public string JvmArgs { get; set; } = "";
        public int Width { get; set; } = 1920;
        public int Height { get; set; } = 1080;
        public bool Fullscreen { get; set; }
        public List<InstalledContent> Mods { get; set; } = new();
        public List<InstalledContent> ResourcePacks { get; set; } = new();
        public List<InstalledContent> Shaders { get; set; } = new();
    }

    public class InstalledContent
    {
        public string ProjectId { get; set; } = "";
        public string Slug { get; set; } = "";
        public string Title { get; set; } = "";
        public string Author { get; set; } = "";
        public string VersionId { get; set; } = "";
        public string VersionNumber { get; set; } = "";
        public string FileName { get; set; } = "";
        public string IconUrl { get; set; } = "";
        public bool Enabled { get; set; } = true;
        public bool IsFavorite { get; set; }
        public DateTime InstalledAt { get; set; } = DateTime.UtcNow;
        public string? UpdateAvailable { get; set; }
    }

    public enum ExploreSortMode
    {
        LastUsed,
        Name,
        Version,
        Loader,
        ModCount,
        Favorites
    }

    public enum ExploreDetailTab
    {
        Settings,
        Mods,
        Catalog,
        Overview,
        ResourcePacks,
        Shaders,
        Worlds
    }

    public enum ExploreMainMode
    {
        Empty,
        InstanceDetail,
        GlobalTextures,
        GlobalShaders
    }

    public enum CatalogType
    {
        Mod,
        ResourcePack,
        Shader
    }
}
