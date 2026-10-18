using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Windows.Media;
using Launcher.Helpers;
using Launcher.Models;
using Launcher.Services;

namespace Launcher.ViewModels
{
    public class LibraryViewModel : INotifyPropertyChanged
    {
        private readonly InstanceService _instanceService = new();
        private readonly ModrinthService _modrinthService = new();
        private readonly ModInstallService _modInstallService = new();
        private bool _isViewingSettings;
        private LibraryCardItemViewModel? _selectedSettingsInstance;
        private string _searchQuery = "";

        public ObservableCollection<LibraryCardItemViewModel> AllVersions { get; } = new();
        public ObservableCollection<LibraryCardItemViewModel> FilteredVersions { get; } = new();

        public bool IsViewingSettings
        {
            get => _isViewingSettings;
            set
            {
                if (_isViewingSettings != value)
                {
                    _isViewingSettings = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(IsViewingGrid));
                }
            }
        }

        public bool IsViewingGrid => !_isViewingSettings;

        public LibraryCardItemViewModel? SelectedSettingsInstance
        {
            get => _selectedSettingsInstance;
            set
            {
                if (_selectedSettingsInstance != value)
                {
                    _selectedSettingsInstance = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(SupportsMods));
                    OnPropertyChanged(nameof(SupportsShaders));
                    OnPropertyChanged(nameof(ModrinthStatusText));
                }
            }
        }

        public bool SupportsMods => !string.Equals(SelectedSettingsInstance?.Loader, "Vanilla", StringComparison.OrdinalIgnoreCase);
        public bool SupportsShaders => !string.Equals(SelectedSettingsInstance?.Loader, "Vanilla", StringComparison.OrdinalIgnoreCase);

        public string SearchQuery
        {
            get => _searchQuery;
            set
            {
                if (_searchQuery != value)
                {
                    _searchQuery = value;
                    OnPropertyChanged();
                    ApplyFilter();
                }
            }
        }

        public bool HasVersions => FilteredVersions.Count > 0;
        public bool IsEmpty => FilteredVersions.Count == 0;

        public event PropertyChangedEventHandler? PropertyChanged;

        public LibraryViewModel()
        {
            LoadInstances();
            InstanceService.InstancesChanged += OnInstancesChanged;
            InstanceSelectionService.SelectedInstanceChanged += OnSelectedInstanceChanged;
        }

        private void OnInstancesChanged()
        {
            System.Windows.Application.Current?.Dispatcher.BeginInvoke(LoadInstances);
        }

        private void OnSelectedInstanceChanged(string id)
        {
            System.Windows.Application.Current?.Dispatcher.BeginInvoke(() =>
            {
                foreach (var item in AllVersions)
                    item.IsSelected = item.Id == id;
            });
        }

        public void LoadInstances()
        {
            var instances = _instanceService.GetAll()
                .OrderByDescending(i => i.IsFavorite)
                .ThenByDescending(i => i.LastPlayedAt ?? DateTime.MinValue)
                .ToList();

            var currentSelected = InstanceSelectionService.SelectedInstanceId;
            if (string.IsNullOrEmpty(currentSelected) && instances.Count > 0)
            {
                currentSelected = instances[0].Id;
                InstanceSelectionService.ForceSelect(currentSelected);
            }

            AllVersions.Clear();
            foreach (var inst in instances)
            {
                AllVersions.Add(new LibraryCardItemViewModel
                {
                    Id = inst.Id,
                    Name = inst.Name,
                    MinecraftVersion = string.IsNullOrWhiteSpace(inst.MinecraftVersion) || inst.MinecraftVersion.Equals("Unknown", StringComparison.OrdinalIgnoreCase) ? "1.21.4" : inst.MinecraftVersion,
                    Loader = inst.Loader,
                    LoaderIconUri = LoaderBranding.GetIconUri(inst.Loader),
                    ArtworkUri = ResolveArtworkUri(inst.MinecraftVersion, inst.Name),
                    LastPlayedText = inst.LastPlayedAt.HasValue
                        ? inst.LastPlayedAt.Value.ToString("MMM dd, yyyy")
                        : "Never played",
                    IsFavorite = inst.IsFavorite,
                    IsSelected = inst.Id == currentSelected,
                    RamMb = inst.RamMb,
                    JavaPath = inst.JavaPath,
                    JvmArgs = inst.JvmArgs
                });
            }

            ApplyFilter();
        }

        public static string ResolveArtworkUri(string mcVersion, string name)
        {
            var v = (mcVersion ?? "").ToLowerInvariant().Trim();
            var n = (name ?? "").ToLowerInvariant().Trim();

            // 26.1 (The Garden Awakens / Pale Garden & Creaking)
            if (v.StartsWith("26.1") || n.Contains("26.1") || n.Contains("garden") || n.Contains("creaking") || n.Contains("pale"))
                return "pack://application:,,,/Assets/Versions/26_1.jpg";

            // 26.2 (Bundles of Bravery)
            if (v.StartsWith("26.2") || n.Contains("26.2") || n.Contains("bundle"))
                return "pack://application:,,,/Assets/Versions/26_2.jpg";

            // 26.3 (Spring to Life)
            if (v.StartsWith("26.3") || n.Contains("26.3") || n.Contains("spring") || n.Contains("mount"))
                return "pack://application:,,,/Assets/Versions/26_3.jpg";

            // Any other 26.x
            if (v.StartsWith("26.") || n.Contains("26."))
                return "pack://application:,,,/Assets/Versions/26_1.jpg";

            // 1.21 Tricky Trials (Breeze & Alex Lunar action crop)
            if (v.StartsWith("1.21") || n.Contains("1.21") || n.Contains("trial") || n.Contains("breeze"))
                return "pack://application:,,,/Assets/Versions/1_21.jpg";

            // 1.20 Trails & Tales (Sniffers & Cherry Blossom)
            if (v.StartsWith("1.20") || n.Contains("1.20") || n.Contains("trail") || n.Contains("sniffer"))
                return "pack://application:,,,/Assets/Versions/1_20.jpg";

            // 1.19 The Wild Update
            if (v.StartsWith("1.19") || n.Contains("1.19") || n.Contains("wild") || n.Contains("warden"))
                return "pack://application:,,,/Assets/Versions/1_19.jpg";

            // 1.18 & 1.17 Caves & Cliffs
            if (v.StartsWith("1.18") || v.StartsWith("1.17") || n.Contains("1.18") || n.Contains("1.17") || n.Contains("cave") || n.Contains("cliff"))
                return "pack://application:,,,/Assets/Versions/1_18.jpg";

            // 1.16 & 1.15 Nether Update
            if (v.StartsWith("1.16") || v.StartsWith("1.15") || n.Contains("1.16") || n.Contains("1.15") || n.Contains("nether"))
                return "pack://application:,,,/Assets/Versions/1_16.jpg";

            // 1.14 & 1.13 Village & Pillage
            if (v.StartsWith("1.14") || v.StartsWith("1.13") || n.Contains("1.14") || n.Contains("1.13") || n.Contains("village") || n.Contains("pillage"))
                return "pack://application:,,,/Assets/Versions/1_14.jpg";

            return "pack://application:,,,/Assets/Versions/default.jpg";
        }

        public void ApplyFilter()
        {
            FilteredVersions.Clear();
            var q = (_searchQuery ?? "").Trim().ToLowerInvariant();
            var matched = string.IsNullOrEmpty(q)
                ? AllVersions
                : AllVersions.Where(v => v.Name.ToLowerInvariant().Contains(q) ||
                                         v.MinecraftVersion.ToLowerInvariant().Contains(q) ||
                                         v.Loader.ToLowerInvariant().Contains(q));

            foreach (var item in matched)
                FilteredVersions.Add(item);

            OnPropertyChanged(nameof(HasVersions));
            OnPropertyChanged(nameof(IsEmpty));
        }

        public static event Action<bool, LibraryCardItemViewModel?>? VersionSettingsModeChanged;

        private string _settingsTab = "Info";
        public string SettingsTab
        {
            get => _settingsTab;
            set
            {
                if (_settingsTab != value)
                {
                    _settingsTab = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(IsInfoTabSelected));
                    OnPropertyChanged(nameof(IsModTabSelected));
                    OnPropertyChanged(nameof(IsModsTabSelected));
                    OnPropertyChanged(nameof(IsTexturesTabSelected));
                    OnPropertyChanged(nameof(IsShadersTabSelected));
                    OnPropertyChanged(nameof(IsModrinthTabSelected));
                    if (IsModsTabSelected)
                    {
                        LoadInstalledMods();
                    }
                    else if (IsTexturesTabSelected)
                    {
                        LoadInstalledTextures();
                    }
                    else if (IsShadersTabSelected)
                    {
                        LoadInstalledShaders();
                    }
                    else if (IsModrinthTabSelected)
                    {
                        LoadModrinthContent();
                    }
                }
            }
        }

        public bool IsInfoTabSelected => _settingsTab == "Info";
        public bool IsModsTabSelected => SupportsMods && (_settingsTab.Equals("Mods", StringComparison.OrdinalIgnoreCase) || _settingsTab.Equals("Mod", StringComparison.OrdinalIgnoreCase));
        public bool IsModTabSelected => IsModsTabSelected;
        public bool IsTexturesTabSelected => _settingsTab.Equals("Textures", StringComparison.OrdinalIgnoreCase);
        public bool IsShadersTabSelected => SupportsShaders && _settingsTab.Equals("Shaders", StringComparison.OrdinalIgnoreCase);
        public bool IsModrinthTabSelected => _settingsTab.Equals("Modrinth", StringComparison.OrdinalIgnoreCase);

        private string _settingsInstanceName = "";
        public string SettingsInstanceName
        {
            get => _settingsInstanceName;
            set
            {
                if (_settingsInstanceName != value)
                {
                    _settingsInstanceName = value;
                    OnPropertyChanged();
                    SaveCurrentSettings();
                }
            }
        }

        private int _settingsRamMb = 4096;
        public int SettingsRamMb
        {
            get => _settingsRamMb;
            set
            {
                if (_settingsRamMb != value)
                {
                    _settingsRamMb = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(SettingsRamText));
                    OnPropertyChanged(nameof(IsRam2Selected));
                    OnPropertyChanged(nameof(IsRam4Selected));
                    OnPropertyChanged(nameof(IsRam6Selected));
                    OnPropertyChanged(nameof(IsRam8Selected));
                    OnPropertyChanged(nameof(IsRam10Selected));
                    OnPropertyChanged(nameof(IsRam12Selected));
                    OnPropertyChanged(nameof(IsRam16Selected));
                    SaveCurrentSettings();
                }
            }
        }

        public string SettingsRamText => $"{_settingsRamMb} MB ({(double)_settingsRamMb / 1024.0:0.#} GB)";

        public bool IsRam2Selected => _settingsRamMb == 2048;
        public bool IsRam4Selected => _settingsRamMb == 4096;
        public bool IsRam6Selected => _settingsRamMb == 6144;
        public bool IsRam8Selected => _settingsRamMb == 8192;
        public bool IsRam10Selected => _settingsRamMb == 10240;
        public bool IsRam12Selected => _settingsRamMb == 12288;
        public bool IsRam16Selected => _settingsRamMb == 16384;

        private string _settingsJvmArgs = "";
        public string SettingsJvmArgs
        {
            get => _settingsJvmArgs;
            set
            {
                if (_settingsJvmArgs != value)
                {
                    _settingsJvmArgs = value;
                    OnPropertyChanged();
                    SaveCurrentSettings();
                }
            }
        }

        public ObservableCollection<InstalledContent> SettingsMods { get; } = new();
        public ObservableCollection<ModItemViewModel> InstalledMods { get; } = new();
        public bool HasInstalledMods => InstalledMods.Count > 0;
        public string ModsCountText => $"{InstalledMods.Count} {(InstalledMods.Count == 1 ? "mod" : "mods")} installed";

        public ObservableCollection<ModItemViewModel> InstalledTextures { get; } = new();
        public bool HasInstalledTextures => InstalledTextures.Count > 0;
        public string TexturesCountText => $"{InstalledTextures.Count} {(InstalledTextures.Count == 1 ? "texture pack" : "texture packs")} available";

        public ObservableCollection<ModItemViewModel> InstalledShaders { get; } = new();
        public bool HasInstalledShaders => InstalledShaders.Count > 0;
        public string ShadersCountText => $"{InstalledShaders.Count} {(InstalledShaders.Count == 1 ? "shader pack" : "shader packs")} available";

        public void LoadInstalledMods()
        {
            InstalledMods.Clear();
            if (SelectedSettingsInstance == null)
            {
                OnPropertyChanged(nameof(HasInstalledMods));
                OnPropertyChanged(nameof(ModsCountText));
                return;
            }

            var inst = _instanceService.GetById(SelectedSettingsInstance.Id);
            if (inst == null)
            {
                OnPropertyChanged(nameof(HasInstalledMods));
                OnPropertyChanged(nameof(ModsCountText));
                return;
            }

            _instanceService.SyncModsFromDisk(inst);
            var modsDir = _instanceService.GetModsPath(inst.Id);

            var list = new List<ModItemViewModel>();
            foreach (var mod in inst.Mods)
            {
                if (mod.FileName.StartsWith("flowclient-branding", StringComparison.OrdinalIgnoreCase))
                    continue;

                var info = ModMetadataService.GetModInfo(modsDir, mod.FileName, mod.IconUrl);
                var title = string.IsNullOrWhiteSpace(info.Title) ? mod.Title : info.Title;
                var vm = new ModItemViewModel(
                    title,
                    info.Subtitle,
                    info.IconPath,
                    info.CleanFileName,
                    info.IsEnabled);
                list.Add(vm);
            }

            foreach (var vm in list.OrderBy(m => m.Title, StringComparer.OrdinalIgnoreCase))
            {
                InstalledMods.Add(vm);
            }

            OnPropertyChanged(nameof(HasInstalledMods));
            OnPropertyChanged(nameof(ModsCountText));
        }

        public void ToggleMod(ModItemViewModel mod)
        {
            if (SelectedSettingsInstance == null) return;
            var inst = _instanceService.GetById(SelectedSettingsInstance.Id);
            if (inst == null) return;

            var modsDir = _instanceService.GetModsPath(inst.Id);
            var clean = mod.FileName;
            if (clean.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase))
                clean = clean[..^".disabled".Length];

            var enabledPath = Path.Combine(modsDir, clean);
            var disabledPath = Path.Combine(modsDir, clean + ".disabled");

            try
            {
                if (mod.IsEnabled)
                {
                    // Was enabled -> disable it: rename to .jar.disabled
                    if (File.Exists(enabledPath))
                    {
                        File.Move(enabledPath, disabledPath, overwrite: true);
                    }
                    mod.IsEnabled = false;
                }
                else
                {
                    // Was disabled -> enable it: rename to .jar
                    if (File.Exists(disabledPath))
                    {
                        File.Move(disabledPath, enabledPath, overwrite: true);
                    }
                    mod.IsEnabled = true;
                }

                var content = inst.Mods.FirstOrDefault(m => m.FileName.Equals(clean, StringComparison.OrdinalIgnoreCase)
                    || m.FileName.Equals(clean + ".disabled", StringComparison.OrdinalIgnoreCase));
                if (content != null)
                {
                    content.Enabled = mod.IsEnabled;
                    content.FileName = clean;
                }
                _instanceService.Save(inst);
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show($"Could not toggle mod: {ex.Message}", "Mod Toggle", System.Windows.MessageBoxButton.OK, System.Windows.MessageBoxImage.Warning);
            }
        }

        public bool HasSettingsMods => SettingsMods.Count > 0;

        public void SetRam(int mb) => SettingsRamMb = mb;

        public void OpenFolder()
        {
            if (SelectedSettingsInstance == null) return;
            var dir = _instanceService.GetInstanceDirectory(SelectedSettingsInstance.Id);
            if (System.IO.Directory.Exists(dir))
            {
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
                {
                    FileName = dir,
                    UseShellExecute = true
                });
            }
        }

        public void OpenModsFolder()
        {
            if (SelectedSettingsInstance == null || !SupportsMods) return;
            var dir = _instanceService.GetModsPath(SelectedSettingsInstance.Id);
            System.IO.Directory.CreateDirectory(dir);
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
            {
                FileName = dir,
                UseShellExecute = true
            });
        }

        public void LoadInstalledTextures()
        {
            InstalledTextures.Clear();
            var dir = GlobalContentService.ResourcePacksDirectory;
            Directory.CreateDirectory(dir);

            var list = new List<ModItemViewModel>();
            foreach (var entry in Directory.EnumerateFileSystemEntries(dir))
            {
                var rawName = Path.GetFileName(entry);
                if (string.IsNullOrEmpty(rawName) || rawName.StartsWith('.')) continue;

                var clean = rawName;
                if (clean.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase))
                    clean = clean[..^".disabled".Length];

                if (!clean.EndsWith(".zip", StringComparison.OrdinalIgnoreCase) && !Directory.Exists(entry))
                    continue;

                var info = PackMetadataService.GetResourcePackInfo(dir, rawName);
                var vm = new ModItemViewModel(
                    info.Title,
                    info.Subtitle,
                    info.IconPath,
                    info.CleanFileName,
                    info.IsEnabled);
                list.Add(vm);
            }

            foreach (var vm in list.OrderBy(m => m.Title, StringComparer.OrdinalIgnoreCase))
            {
                InstalledTextures.Add(vm);
            }

            OnPropertyChanged(nameof(HasInstalledTextures));
            OnPropertyChanged(nameof(TexturesCountText));
        }

        public void ToggleTexture(ModItemViewModel pack)
        {
            var dir = GlobalContentService.ResourcePacksDirectory;
            var clean = pack.FileName;
            if (clean.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase))
                clean = clean[..^".disabled".Length];

            var enabledPath = Path.Combine(dir, clean);
            var disabledPath = Path.Combine(dir, clean + ".disabled");

            try
            {
                if (pack.IsEnabled)
                {
                    if (File.Exists(enabledPath))
                        File.Move(enabledPath, disabledPath, overwrite: true);
                    else if (Directory.Exists(enabledPath))
                        Directory.Move(enabledPath, disabledPath);
                    pack.IsEnabled = false;
                }
                else
                {
                    if (File.Exists(disabledPath))
                        File.Move(disabledPath, enabledPath, overwrite: true);
                    else if (Directory.Exists(disabledPath))
                        Directory.Move(disabledPath, enabledPath);
                    pack.IsEnabled = true;
                }

                Task.Run(() =>
                {
                    try
                    {
                        var globalService = new GlobalContentService();
                        globalService.SyncToAllInstanceDirectories(_instanceService);
                    }
                    catch { }
                });
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show($"Could not toggle texture pack: {ex.Message}", "Texture Pack Toggle", System.Windows.MessageBoxButton.OK, System.Windows.MessageBoxImage.Warning);
            }
        }

        public void OpenTexturesFolder()
        {
            var dir = GlobalContentService.ResourcePacksDirectory;
            Directory.CreateDirectory(dir);
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
            {
                FileName = dir,
                UseShellExecute = true
            });
        }

        public void LoadInstalledShaders()
        {
            InstalledShaders.Clear();
            var dir = GlobalContentService.ShadersDirectory;
            Directory.CreateDirectory(dir);

            var list = new List<ModItemViewModel>();
            foreach (var entry in Directory.EnumerateFileSystemEntries(dir))
            {
                var rawName = Path.GetFileName(entry);
                if (string.IsNullOrEmpty(rawName) || rawName.StartsWith('.')) continue;
                if (rawName.EndsWith(".txt", StringComparison.OrdinalIgnoreCase)) continue;

                var clean = rawName;
                if (clean.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase))
                    clean = clean[..^".disabled".Length];

                if (!clean.EndsWith(".zip", StringComparison.OrdinalIgnoreCase) && !Directory.Exists(entry))
                    continue;

                var info = PackMetadataService.GetShaderPackInfo(dir, rawName);
                var vm = new ModItemViewModel(
                    info.Title,
                    info.Subtitle,
                    info.IconPath,
                    info.CleanFileName,
                    info.IsEnabled);
                list.Add(vm);
            }

            foreach (var vm in list.OrderBy(m => m.Title, StringComparer.OrdinalIgnoreCase))
            {
                InstalledShaders.Add(vm);
            }

            OnPropertyChanged(nameof(HasInstalledShaders));
            OnPropertyChanged(nameof(ShadersCountText));
        }

        public void ToggleShader(ModItemViewModel pack)
        {
            var dir = GlobalContentService.ShadersDirectory;
            var clean = pack.FileName;
            if (clean.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase))
                clean = clean[..^".disabled".Length];

            var enabledPath = Path.Combine(dir, clean);
            var disabledPath = Path.Combine(dir, clean + ".disabled");

            try
            {
                if (pack.IsEnabled)
                {
                    if (File.Exists(enabledPath))
                        File.Move(enabledPath, disabledPath, overwrite: true);
                    else if (Directory.Exists(enabledPath))
                        Directory.Move(enabledPath, disabledPath);
                    pack.IsEnabled = false;
                }
                else
                {
                    if (File.Exists(disabledPath))
                        File.Move(disabledPath, enabledPath, overwrite: true);
                    else if (Directory.Exists(disabledPath))
                        Directory.Move(disabledPath, enabledPath);
                    pack.IsEnabled = true;
                }

                Task.Run(() =>
                {
                    try
                    {
                        var globalService = new GlobalContentService();
                        globalService.SyncToAllInstanceDirectories(_instanceService);
                    }
                    catch { }
                });
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show($"Could not toggle shader pack: {ex.Message}", "Shader Pack Toggle", System.Windows.MessageBoxButton.OK, System.Windows.MessageBoxImage.Warning);
            }
        }

        public void OpenShadersFolder()
        {
            var dir = GlobalContentService.ShadersDirectory;
            Directory.CreateDirectory(dir);
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
            {
                FileName = dir,
                UseShellExecute = true
            });
        }

        public void DeleteMod(ModItemViewModel mod)
        {
            if (mod == null || !mod.CanDelete || SelectedSettingsInstance == null) return;
            var inst = _instanceService.GetById(SelectedSettingsInstance.Id);
            if (inst == null) return;

            var modsDir = _instanceService.GetModsPath(inst.Id);
            var clean = mod.FileName;
            if (clean.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase))
                clean = clean[..^".disabled".Length];

            try
            {
                var p1 = Path.Combine(modsDir, clean);
                var p2 = Path.Combine(modsDir, clean + ".disabled");
                if (File.Exists(p1)) File.Delete(p1);
                if (File.Exists(p2)) File.Delete(p2);

                inst.Mods.RemoveAll(m => m.FileName.Equals(clean, StringComparison.OrdinalIgnoreCase) ||
                                         m.FileName.Equals(clean + ".disabled", StringComparison.OrdinalIgnoreCase) ||
                                         m.Title.Equals(mod.Title, StringComparison.OrdinalIgnoreCase));
                _instanceService.Save(inst);

                InstalledMods.Remove(mod);
                OnPropertyChanged(nameof(HasInstalledMods));
                OnPropertyChanged(nameof(ModsCountText));
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show($"Could not delete mod: {ex.Message}", "Delete Mod", System.Windows.MessageBoxButton.OK, System.Windows.MessageBoxImage.Warning);
            }
        }

        public void DeleteTexture(ModItemViewModel pack)
        {
            if (pack == null || !pack.CanDelete) return;
            var dir = GlobalContentService.ResourcePacksDirectory;
            var clean = pack.FileName;
            if (clean.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase))
                clean = clean[..^".disabled".Length];

            try
            {
                var p1 = Path.Combine(dir, clean);
                var p2 = Path.Combine(dir, clean + ".disabled");
                if (File.Exists(p1)) File.Delete(p1);
                if (File.Exists(p2)) File.Delete(p2);
                if (Directory.Exists(p1)) Directory.Delete(p1, true);
                if (Directory.Exists(p2)) Directory.Delete(p2, true);

                InstalledTextures.Remove(pack);
                OnPropertyChanged(nameof(HasInstalledTextures));
                OnPropertyChanged(nameof(TexturesCountText));

                Task.Run(() =>
                {
                    try
                    {
                        var globalService = new GlobalContentService();
                        globalService.SyncToAllInstanceDirectories(_instanceService);
                    }
                    catch { }
                });
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show($"Could not delete texture pack: {ex.Message}", "Delete Texture Pack", System.Windows.MessageBoxButton.OK, System.Windows.MessageBoxImage.Warning);
            }
        }

        public void DeleteShader(ModItemViewModel pack)
        {
            if (pack == null || !pack.CanDelete) return;
            var dir = GlobalContentService.ShadersDirectory;
            var clean = pack.FileName;
            if (clean.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase))
                clean = clean[..^".disabled".Length];

            try
            {
                var p1 = Path.Combine(dir, clean);
                var p2 = Path.Combine(dir, clean + ".disabled");
                if (File.Exists(p1)) File.Delete(p1);
                if (File.Exists(p2)) File.Delete(p2);
                if (Directory.Exists(p1)) Directory.Delete(p1, true);
                if (Directory.Exists(p2)) Directory.Delete(p2, true);

                InstalledShaders.Remove(pack);
                OnPropertyChanged(nameof(HasInstalledShaders));
                OnPropertyChanged(nameof(ShadersCountText));

                Task.Run(() =>
                {
                    try
                    {
                        var globalService = new GlobalContentService();
                        globalService.SyncToAllInstanceDirectories(_instanceService);
                    }
                    catch { }
                });
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show($"Could not delete shader pack: {ex.Message}", "Delete Shader Pack", System.Windows.MessageBoxButton.OK, System.Windows.MessageBoxImage.Warning);
            }
        }

        public void ImportMod()
        {
            if (SelectedSettingsInstance == null) return;
            var dlg = new Microsoft.Win32.OpenFileDialog
            {
                Title = "Import Minecraft Mod",
                Filter = "Minecraft Mods (*.jar)|*.jar|All Files (*.*)|*.*",
                Multiselect = true
            };
            if (dlg.ShowDialog() == true && dlg.FileNames.Length > 0)
            {
                var inst = _instanceService.GetById(SelectedSettingsInstance.Id);
                if (inst == null) return;
                var modsDir = _instanceService.GetModsPath(inst.Id);
                Directory.CreateDirectory(modsDir);
                foreach (var src in dlg.FileNames)
                {
                    try
                    {
                        var dest = Path.Combine(modsDir, Path.GetFileName(src));
                        File.Copy(src, dest, overwrite: true);
                    }
                    catch { }
                }
                LoadInstalledMods();
            }
        }

        public void ImportTexture()
        {
            var dlg = new Microsoft.Win32.OpenFileDialog
            {
                Title = "Import Resource Pack",
                Filter = "Resource Packs (*.zip)|*.zip|All Files (*.*)|*.*",
                Multiselect = true
            };
            if (dlg.ShowDialog() == true && dlg.FileNames.Length > 0)
            {
                var dir = GlobalContentService.ResourcePacksDirectory;
                Directory.CreateDirectory(dir);
                foreach (var src in dlg.FileNames)
                {
                    try
                    {
                        var dest = Path.Combine(dir, Path.GetFileName(src));
                        File.Copy(src, dest, overwrite: true);
                    }
                    catch { }
                }
                Task.Run(() =>
                {
                    try
                    {
                        var globalService = new GlobalContentService();
                        globalService.SyncToAllInstanceDirectories(_instanceService);
                    }
                    catch { }
                });
                LoadInstalledTextures();
            }
        }

        public void ImportShader()
        {
            var dlg = new Microsoft.Win32.OpenFileDialog
            {
                Title = "Import Shader Pack",
                Filter = "Shader Packs (*.zip)|*.zip|All Files (*.*)|*.*",
                Multiselect = true
            };
            if (dlg.ShowDialog() == true && dlg.FileNames.Length > 0)
            {
                var dir = GlobalContentService.ShadersDirectory;
                Directory.CreateDirectory(dir);
                foreach (var src in dlg.FileNames)
                {
                    try
                    {
                        var dest = Path.Combine(dir, Path.GetFileName(src));
                        File.Copy(src, dest, overwrite: true);
                    }
                    catch { }
                }
                Task.Run(() =>
                {
                    try
                    {
                        var globalService = new GlobalContentService();
                        globalService.SyncToAllInstanceDirectories(_instanceService);
                    }
                    catch { }
                });
                LoadInstalledShaders();
            }
        }

        // ══════════════ MODRINTH CATEGORY VIEWMODEL PROPERTIES & METHODS ══════════════
        private string _modrinthSubtype = "mod"; // "mod", "resourcepack", "shader"
        public string ModrinthSubtype
        {
            get => _modrinthSubtype;
            set
            {
                var target = value;
                if (!SupportsMods && target == "mod") target = "resourcepack";
                if (!SupportsShaders && target == "shader") target = "resourcepack";

                if (_modrinthSubtype != target)
                {
                    _modrinthSubtype = target;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(IsModrinthModsSelected));
                    OnPropertyChanged(nameof(IsModrinthTexturesSelected));
                    OnPropertyChanged(nameof(IsModrinthShadersSelected));
                    OnPropertyChanged(nameof(ModrinthStatusText));
                    LoadModrinthContent();
                }
            }
        }

        public bool IsModrinthModsSelected => _modrinthSubtype == "mod";
        public bool IsModrinthTexturesSelected => _modrinthSubtype == "resourcepack";
        public bool IsModrinthShadersSelected => _modrinthSubtype == "shader";

        private string _modrinthSearchQuery = "";
        public string ModrinthSearchQuery
        {
            get => _modrinthSearchQuery;
            set
            {
                if (_modrinthSearchQuery != value)
                {
                    _modrinthSearchQuery = value;
                    OnPropertyChanged();
                }
            }
        }

        public ObservableCollection<ModrinthItemViewModel> ModrinthItems { get; } = new();

        private bool _isModrinthLoading;
        public bool IsModrinthLoading
        {
            get => _isModrinthLoading;
            set
            {
                if (_isModrinthLoading != value)
                {
                    _isModrinthLoading = value;
                    OnPropertyChanged();
                }
            }
        }

        public bool HasModrinthItems => ModrinthItems.Count > 0;

        public string ModrinthStatusText
        {
            get
            {
                var inst = SelectedSettingsInstance
                           ?? AllVersions.FirstOrDefault(v => v.Id == InstanceSelectionService.SelectedInstanceId)
                           ?? AllVersions.FirstOrDefault();
                var mc = string.IsNullOrWhiteSpace(inst?.MinecraftVersion) || inst.MinecraftVersion.Equals("Unknown", StringComparison.OrdinalIgnoreCase)
                    ? "1.21.4"
                    : inst.MinecraftVersion;
                var loader = inst?.Loader ?? "Vanilla";

                if (_modrinthSubtype == "mod")
                    return $"Minecraft {mc} • {loader}";
                if (_modrinthSubtype == "resourcepack")
                    return $"Minecraft {mc} • Textures";
                return $"Minecraft {mc} • Shaders";
            }
        }

        public void LoadModrinthContent()
        {
            _ = SearchModrinthAsync(_modrinthSearchQuery);
        }

        public async Task SearchModrinthAsync(string? query = null)
        {
            if (SelectedSettingsInstance == null) return;
            IsModrinthLoading = true;
            ModrinthItems.Clear();
            OnPropertyChanged(nameof(HasModrinthItems));

            var mcVersion = SelectedSettingsInstance.MinecraftVersion;
            var loader = _modrinthSubtype == "mod" ? SelectedSettingsInstance.Loader : null;
            if (loader == "Vanilla") loader = null;

            var q = query?.Trim() ?? "";

            try
            {
                var hits = await _modrinthService.SearchAsync(q, _modrinthSubtype, mcVersion, loader, limit: 20);

                var inst = _instanceService.GetById(SelectedSettingsInstance.Id);
                var installedModsList = inst?.Mods ?? new List<InstalledContent>();

                foreach (var hit in hits)
                {
                    var item = new ModrinthItemViewModel
                    {
                        ProjectId = hit.ProjectId,
                        Slug = hit.Slug,
                        Title = hit.Title,
                        Description = hit.Description,
                        Author = hit.Author,
                        IconUrl = hit.IconUrl,
                        Downloads = hit.Downloads,
                        ProjectType = _modrinthSubtype
                    };

                    // Check if already installed
                    if (_modrinthSubtype == "mod")
                    {
                        var isBundled = hit.Slug.Contains("flowclient") || hit.Title.Contains("Flow Client") ||
                                        hit.Slug.Contains("voicechat") || hit.Title.Contains("Voice Chat");
                        var isInstalled = installedModsList.Any(m => m.ProjectId == hit.ProjectId ||
                                                                     string.Equals(m.Slug, hit.Slug, StringComparison.OrdinalIgnoreCase))
                                          || InstalledMods.Any(m => m.Title.Equals(hit.Title, StringComparison.OrdinalIgnoreCase) ||
                                                                    m.FileName.Contains(hit.Slug, StringComparison.OrdinalIgnoreCase));
                        item.IsInstalled = isInstalled;
                        item.CanUninstall = !isBundled;
                    }
                    else if (_modrinthSubtype == "resourcepack")
                    {
                        var isInstalled = InstalledTextures.Any(t => t.Title.Equals(hit.Title, StringComparison.OrdinalIgnoreCase) ||
                                                                    t.FileName.Contains(hit.Slug, StringComparison.OrdinalIgnoreCase));
                        item.IsInstalled = isInstalled;
                        item.CanUninstall = true;
                    }
                    else if (_modrinthSubtype == "shader")
                    {
                        var isInstalled = InstalledShaders.Any(s => s.Title.Equals(hit.Title, StringComparison.OrdinalIgnoreCase) ||
                                                                   s.FileName.Contains(hit.Slug, StringComparison.OrdinalIgnoreCase));
                        item.IsInstalled = isInstalled;
                        item.CanUninstall = true;
                    }

                    ModrinthItems.Add(item);

                    // Fetch versions in background
                    _ = LoadVersionsForItemAsync(item, mcVersion, loader);
                }
            }
            catch { }
            finally
            {
                IsModrinthLoading = false;
                OnPropertyChanged(nameof(HasModrinthItems));
            }
        }

        private async Task LoadVersionsForItemAsync(ModrinthItemViewModel item, string mcVersion, string? loader)
        {
            try
            {
                var versions = await _modrinthService.GetVersionsAsync(item.ProjectId, mcVersion, loader);
                System.Windows.Application.Current?.Dispatcher.BeginInvoke(() =>
                {
                    item.AvailableVersions.Clear();
                    foreach (var v in versions)
                    {
                        var file = v.Files.FirstOrDefault(f => f.Primary) ?? v.Files.FirstOrDefault();
                        if (file != null)
                        {
                            item.AvailableVersions.Add(new ModrinthVersionItemViewModel
                            {
                                VersionId = v.Id,
                                VersionNumber = v.VersionNumber,
                                Name = v.Name,
                                DisplayName = string.IsNullOrWhiteSpace(v.VersionNumber) ? v.Name : v.VersionNumber,
                                DownloadUrl = file.Url,
                                FileName = file.Filename
                            });
                        }
                    }

                    if (item.AvailableVersions.Count > 0)
                    {
                        item.SelectedVersion = item.AvailableVersions[0];
                    }
                    item.OnPropertyChanged(nameof(item.HasMultipleVersions));
                });
            }
            catch { }
        }

        public async Task InstallModrinthItemAsync(ModrinthItemViewModel item)
        {
            if (item == null || item.IsInstalling || SelectedSettingsInstance == null) return;
            if (item.ProjectType == "mod" && !SupportsMods) return;
            if (item.ProjectType == "shader" && !SupportsShaders) return;
            item.IsInstalling = true;

            var inst = _instanceService.GetById(SelectedSettingsInstance.Id);
            if (inst == null)
            {
                item.IsInstalling = false;
                return;
            }

            try
            {
                if (item.SelectedVersion == null)
                {
                    var mc = inst.MinecraftVersion;
                    var ld = item.ProjectType == "mod" ? (inst.Loader == "Vanilla" ? null : inst.Loader) : null;
                    var vers = await _modrinthService.GetVersionsAsync(item.ProjectId, mc, ld);
                    if (vers.Count > 0)
                    {
                        var v = vers[0];
                        var file = v.Files.FirstOrDefault(f => f.Primary) ?? v.Files.FirstOrDefault();
                        if (file != null)
                        {
                            item.SelectedVersion = new ModrinthVersionItemViewModel
                            {
                                VersionId = v.Id,
                                VersionNumber = v.VersionNumber,
                                Name = v.Name,
                                DisplayName = v.VersionNumber,
                                DownloadUrl = file.Url,
                                FileName = file.Filename
                            };
                        }
                    }
                }

                if (item.SelectedVersion == null)
                {
                    System.Windows.MessageBox.Show("No compatible download file found for this item.", "Install", System.Windows.MessageBoxButton.OK, System.Windows.MessageBoxImage.Warning);
                    item.IsInstalling = false;
                    return;
                }

                if (item.ProjectType == "mod")
                {
                    await _modInstallService.InstallModAsync(inst, item.SelectedVersion.VersionId);
                    LoadInstalledMods();
                }
                else if (item.ProjectType == "resourcepack")
                {
                    var dir = GlobalContentService.ResourcePacksDirectory;
                    Directory.CreateDirectory(dir);
                    var bytes = await _modrinthService.DownloadFileAsync(item.SelectedVersion.DownloadUrl);
                    var targetFile = Path.Combine(dir, item.SelectedVersion.FileName);
                    await File.WriteAllBytesAsync(targetFile, bytes);
                    _ = Task.Run(() =>
                    {
                        try
                        {
                            var globalService = new GlobalContentService();
                            globalService.SyncToAllInstanceDirectories(_instanceService);
                        }
                        catch { }
                    });
                    LoadInstalledTextures();
                }
                else if (item.ProjectType == "shader")
                {
                    var dir = GlobalContentService.ShadersDirectory;
                    Directory.CreateDirectory(dir);
                    var bytes = await _modrinthService.DownloadFileAsync(item.SelectedVersion.DownloadUrl);
                    var targetFile = Path.Combine(dir, item.SelectedVersion.FileName);
                    await File.WriteAllBytesAsync(targetFile, bytes);
                    _ = Task.Run(() =>
                    {
                        try
                        {
                            var globalService = new GlobalContentService();
                            globalService.SyncToAllInstanceDirectories(_instanceService);
                        }
                        catch { }
                    });
                    LoadInstalledShaders();
                }

                item.IsInstalled = true;
                item.CanUninstall = true;
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show($"Failed to install {item.Title}: {ex.Message}", "Install Failed", System.Windows.MessageBoxButton.OK, System.Windows.MessageBoxImage.Error);
            }
            finally
            {
                item.IsInstalling = false;
            }
        }

        public void UninstallModrinthItem(ModrinthItemViewModel item)
        {
            if (item == null || !item.CanUninstall || SelectedSettingsInstance == null) return;
            var inst = _instanceService.GetById(SelectedSettingsInstance.Id);
            if (inst == null) return;

            try
            {
                if (item.ProjectType == "mod")
                {
                    var modsDir = _instanceService.GetModsPath(inst.Id);
                    var entry = inst.Mods.FirstOrDefault(m => m.ProjectId == item.ProjectId ||
                                                              string.Equals(m.Slug, item.Slug, StringComparison.OrdinalIgnoreCase) ||
                                                              string.Equals(m.Title, item.Title, StringComparison.OrdinalIgnoreCase));
                    if (entry != null)
                    {
                        var p1 = Path.Combine(modsDir, entry.FileName);
                        var p2 = Path.Combine(modsDir, entry.FileName + ".disabled");
                        if (File.Exists(p1)) File.Delete(p1);
                        if (File.Exists(p2)) File.Delete(p2);
                        inst.Mods.Remove(entry);
                        _instanceService.Save(inst);
                    }
                    else
                    {
                        foreach (var f in Directory.GetFiles(modsDir, "*.jar*"))
                        {
                            var fn = Path.GetFileName(f).ToLowerInvariant();
                            if (fn.Contains(item.Slug.ToLowerInvariant()) || (!string.IsNullOrEmpty(item.SelectedVersion?.FileName) && fn.Contains(item.SelectedVersion.FileName.ToLowerInvariant())))
                            {
                                File.Delete(f);
                            }
                        }
                    }
                    LoadInstalledMods();
                }
                else if (item.ProjectType == "resourcepack")
                {
                    var dir = GlobalContentService.ResourcePacksDirectory;
                    foreach (var f in Directory.GetFiles(dir))
                    {
                        var fn = Path.GetFileName(f).ToLowerInvariant();
                        if (fn.Contains(item.Slug.ToLowerInvariant()) || fn.Contains(item.Title.ToLowerInvariant()) || (!string.IsNullOrEmpty(item.SelectedVersion?.FileName) && fn.Contains(item.SelectedVersion.FileName.ToLowerInvariant())))
                        {
                            File.Delete(f);
                        }
                    }
                    Task.Run(() =>
                    {
                        try
                        {
                            var globalService = new GlobalContentService();
                            globalService.SyncToAllInstanceDirectories(_instanceService);
                        }
                        catch { }
                    });
                    LoadInstalledTextures();
                }
                else if (item.ProjectType == "shader")
                {
                    var dir = GlobalContentService.ShadersDirectory;
                    foreach (var f in Directory.GetFiles(dir))
                    {
                        var fn = Path.GetFileName(f).ToLowerInvariant();
                        if (fn.Contains(item.Slug.ToLowerInvariant()) || fn.Contains(item.Title.ToLowerInvariant()) || (!string.IsNullOrEmpty(item.SelectedVersion?.FileName) && fn.Contains(item.SelectedVersion.FileName.ToLowerInvariant())))
                        {
                            File.Delete(f);
                        }
                    }
                    Task.Run(() =>
                    {
                        try
                        {
                            var globalService = new GlobalContentService();
                            globalService.SyncToAllInstanceDirectories(_instanceService);
                        }
                        catch { }
                    });
                    LoadInstalledShaders();
                }

                item.IsInstalled = false;
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show($"Could not uninstall {item.Title}: {ex.Message}", "Uninstall", System.Windows.MessageBoxButton.OK, System.Windows.MessageBoxImage.Warning);
            }
        }

        public void DuplicateCurrentInstance()
        {
            if (SelectedSettingsInstance == null) return;
            var inst = _instanceService.GetById(SelectedSettingsInstance.Id);
            if (inst != null)
            {
                _instanceService.Duplicate(inst);
                LoadInstances();
            }
        }

        public void DeleteCurrentInstance()
        {
            if (SelectedSettingsInstance == null) return;
            var id = SelectedSettingsInstance.Id;
            var name = SelectedSettingsInstance.Name;
            var confirm = System.Windows.MessageBox.Show(
                $"Are you sure you want to delete '{name}'? This cannot be undone.",
                "Delete Instance",
                System.Windows.MessageBoxButton.YesNo,
                System.Windows.MessageBoxImage.Warning);

            if (confirm == System.Windows.MessageBoxResult.Yes)
            {
                _instanceService.Delete(id);
                CloseSettings();
                LoadInstances();
            }
        }

        private void SaveCurrentSettings()
        {
            if (SelectedSettingsInstance == null) return;
            var inst = _instanceService.GetById(SelectedSettingsInstance.Id);
            if (inst != null)
            {
                inst.Name = string.IsNullOrWhiteSpace(_settingsInstanceName) ? inst.Name : _settingsInstanceName.Trim();
                inst.RamMb = _settingsRamMb;
                inst.JvmArgs = _settingsJvmArgs ?? "";
                _instanceService.Save(inst);

                SelectedSettingsInstance.Name = inst.Name;
                SelectedSettingsInstance.RamMb = inst.RamMb;
                SelectedSettingsInstance.JvmArgs = inst.JvmArgs;
            }
        }

        public void OpenSettings(LibraryCardItemViewModel item)
        {
            SelectedSettingsInstance = item;
            _settingsTab = "Info";

            var isVanilla = string.Equals(item.Loader, "Vanilla", StringComparison.OrdinalIgnoreCase);
            if (isVanilla)
            {
                _modrinthSubtype = "resourcepack";
            }
            else if (_modrinthSubtype != "mod" && _modrinthSubtype != "shader")
            {
                _modrinthSubtype = "mod";
            }

            var inst = _instanceService.GetById(item.Id);
            if (inst != null)
            {
                _settingsInstanceName = inst.Name;
                _settingsRamMb = inst.RamMb > 0 ? inst.RamMb : 4096;
                _settingsJvmArgs = inst.JvmArgs ?? "";

                if (!isVanilla)
                {
                    _instanceService.SyncModsFromDisk(inst);
                }
                SettingsMods.Clear();
                foreach (var mod in inst.Mods)
                {
                    SettingsMods.Add(mod);
                }
            }
            else
            {
                _settingsInstanceName = item.Name;
                _settingsRamMb = item.RamMb > 0 ? item.RamMb : 4096;
                _settingsJvmArgs = item.JvmArgs ?? "";
                SettingsMods.Clear();
            }

            OnPropertyChanged(nameof(SettingsTab));
            OnPropertyChanged(nameof(IsInfoTabSelected));
            OnPropertyChanged(nameof(IsModTabSelected));
            OnPropertyChanged(nameof(IsModsTabSelected));
            OnPropertyChanged(nameof(IsTexturesTabSelected));
            OnPropertyChanged(nameof(IsShadersTabSelected));
            OnPropertyChanged(nameof(IsModrinthTabSelected));
            OnPropertyChanged(nameof(SettingsInstanceName));
            OnPropertyChanged(nameof(SettingsRamMb));
            OnPropertyChanged(nameof(SettingsRamText));
            OnPropertyChanged(nameof(IsRam2Selected));
            OnPropertyChanged(nameof(IsRam4Selected));
            OnPropertyChanged(nameof(IsRam6Selected));
            OnPropertyChanged(nameof(IsRam8Selected));
            OnPropertyChanged(nameof(IsRam10Selected));
            OnPropertyChanged(nameof(IsRam12Selected));
            OnPropertyChanged(nameof(IsRam16Selected));
            OnPropertyChanged(nameof(SettingsJvmArgs));
            OnPropertyChanged(nameof(HasSettingsMods));

            LoadInstalledMods();
            LoadInstalledTextures();
            LoadInstalledShaders();
            if (IsModrinthTabSelected)
                LoadModrinthContent();

            IsViewingSettings = true;
            VersionSettingsModeChanged?.Invoke(true, item);
        }

        public void CloseSettings()
        {
            IsViewingSettings = false;
            SelectedSettingsInstance = null;
            VersionSettingsModeChanged?.Invoke(false, null);
        }

        protected void OnPropertyChanged([CallerMemberName] string? propName = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propName));
    }

    public class LibraryCardItemViewModel : INotifyPropertyChanged
    {
        private bool _isSelected;
        private bool _isFavorite;

        public string Id { get; set; } = "";
        public string Name { get; set; } = "Instance";
        public string MinecraftVersion { get; set; } = "1.20.1";
        public string Loader { get; set; } = "Fabric";
        public string? LoaderIconUri { get; set; }
        public string ArtworkUri { get; set; } = "pack://application:,,,/Assets/Versions/default.jpg";
        public string LastPlayedText { get; set; } = "Never played";
        public int RamMb { get; set; } = 4096;
        public string JavaPath { get; set; } = "";
        public string JvmArgs { get; set; } = "";

        public string DisplayTitle
        {
            get
            {
                var n = (Name ?? "").Trim();
                // If it's a distinctive custom name like "Beauty" or "new build", display it prominently
                if (!string.IsNullOrWhiteSpace(n) &&
                    !n.Equals("New Instance", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals(MinecraftVersion, StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals($"Fabric {MinecraftVersion}", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals($"Forge {MinecraftVersion}", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals($"NeoForge {MinecraftVersion}", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals($"Quilt {MinecraftVersion}", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals($"Vanilla {MinecraftVersion}", StringComparison.OrdinalIgnoreCase))
                {
                    return n.ToUpperInvariant();
                }

                // Otherwise display FLOW {Major.Minor}
                var v = MinecraftVersion;
                if (v.StartsWith("1."))
                {
                    var parts = v.Split('.');
                    if (parts.Length >= 2) v = parts[0] + "." + parts[1];
                }
                return $"FLOW {v}";
            }
        }

        public string SubtitleText
        {
            get
            {
                var n = (Name ?? "").Trim();
                if (!string.IsNullOrWhiteSpace(n) &&
                    !n.Equals("New Instance", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals(MinecraftVersion, StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals($"Fabric {MinecraftVersion}", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals($"Forge {MinecraftVersion}", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals($"NeoForge {MinecraftVersion}", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals($"Quilt {MinecraftVersion}", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals($"Vanilla {MinecraftVersion}", StringComparison.OrdinalIgnoreCase))
                {
                    return $"{Loader} • {MinecraftVersion}";
                }
                return Loader;
            }
        }

        public bool IsSelected
        {
            get => _isSelected;
            set
            {
                if (_isSelected != value)
                {
                    _isSelected = value;
                    OnPropertyChanged();
                }
            }
        }

        public bool IsFavorite
        {
            get => _isFavorite;
            set
            {
                if (_isFavorite != value)
                {
                    _isFavorite = value;
                    OnPropertyChanged();
                }
            }
        }

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? propName = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propName));
    }

    public class ModItemViewModel : INotifyPropertyChanged
    {
        private bool _isEnabled;

        public ModItemViewModel(string title, string subtitle, string iconPath, string fileName, bool isEnabled)
        {
            Title = title;
            Subtitle = subtitle;
            IconUrl = iconPath;
            FileName = fileName;
            _isEnabled = isEnabled;
        }

        public string Title { get; set; }
        public string Subtitle { get; set; }
        public string IconUrl { get; set; }
        public string FileName { get; set; }

        public bool CanDelete
        {
            get
            {
                var fn = (FileName ?? "").ToLowerInvariant();
                var t = (Title ?? "").ToLowerInvariant();
                if (fn.Contains("flowclient") || t.Contains("flow client") || t.Contains("flowclient"))
                    return false;
                if (fn.Contains("voicechat") || t.Contains("voice chat") || fn.Contains("voice-chat"))
                    return false;
                return true;
            }
        }

        public bool HasIcon => !string.IsNullOrWhiteSpace(IconUrl) && (
            IconUrl.StartsWith("pack:", StringComparison.OrdinalIgnoreCase) ||
            IconUrl.StartsWith("http", StringComparison.OrdinalIgnoreCase) ||
            File.Exists(IconUrl));

        public bool IsEnabled
        {
            get => _isEnabled;
            set
            {
                if (_isEnabled != value)
                {
                    _isEnabled = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(StatusText));
                    OnPropertyChanged(nameof(StatusForeground));
                }
            }
        }

        public string StatusText => IsEnabled ? "Active" : "Disabled";
        public Brush StatusForeground => IsEnabled ? Brushes.White : new SolidColorBrush(Color.FromRgb(0x7E, 0x7E, 0x84));

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? propName = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propName));
    }

    public class ModrinthVersionItemViewModel : INotifyPropertyChanged
    {
        public string VersionId { get; set; } = "";
        public string VersionNumber { get; set; } = "";
        public string Name { get; set; } = "";
        public string DisplayName { get; set; } = "";
        public string DownloadUrl { get; set; } = "";
        public string FileName { get; set; } = "";

        public override string ToString() =>
            !string.IsNullOrWhiteSpace(DisplayName) ? DisplayName :
            (!string.IsNullOrWhiteSpace(VersionNumber) ? VersionNumber :
            (!string.IsNullOrWhiteSpace(Name) ? Name : "Latest"));

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? propName = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propName));
    }

    public class ModrinthItemViewModel : INotifyPropertyChanged
    {
        private bool _isInstalled;
        private bool _isInstalling;
        private string _statusMessage = "";
        private ModrinthVersionItemViewModel? _selectedVersion;

        public string ProjectId { get; set; } = "";
        public string Slug { get; set; } = "";
        public string Title { get; set; } = "";
        public string Description { get; set; } = "";
        public string Author { get; set; } = "";
        public string? IconUrl { get; set; }
        public long Downloads { get; set; }
        public string ProjectType { get; set; } = "mod";
        public bool CanUninstall { get; set; } = true;

        public bool HasIcon => !string.IsNullOrWhiteSpace(IconUrl) &&
            (IconUrl.StartsWith("http", StringComparison.OrdinalIgnoreCase) ||
             IconUrl.StartsWith("pack:", StringComparison.OrdinalIgnoreCase) ||
             File.Exists(IconUrl));

        public string DownloadsText
        {
            get
            {
                if (Downloads >= 1_000_000) return $"{(Downloads / 1_000_000.0):0.#}M";
                if (Downloads >= 1_000) return $"{(Downloads / 1_000.0):0.#}K";
                return Downloads.ToString();
            }
        }

        public string SubtitleText => string.IsNullOrWhiteSpace(Author)
            ? $"{DownloadsText} downloads"
            : $"by {Author} • {DownloadsText} downloads";

        public ObservableCollection<ModrinthVersionItemViewModel> AvailableVersions { get; } = new();

        public ModrinthVersionItemViewModel? SelectedVersion
        {
            get => _selectedVersion;
            set
            {
                if (_selectedVersion != value)
                {
                    _selectedVersion = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(SelectedVersionNumberText));
                }
            }
        }

        public string SelectedVersionNumberText => SelectedVersion?.DisplayName ?? "Latest";

        public bool HasMultipleVersions => AvailableVersions.Count > 1;

        public bool IsInstalled
        {
            get => _isInstalled;
            set
            {
                if (_isInstalled != value)
                {
                    _isInstalled = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(InstallButtonText));
                    OnPropertyChanged(nameof(ShowInstallButton));
                    OnPropertyChanged(nameof(ShowUninstallButton));
                    OnPropertyChanged(nameof(ShowIncludedBadge));
                }
            }
        }

        public bool IsInstalling
        {
            get => _isInstalling;
            set
            {
                if (_isInstalling != value)
                {
                    _isInstalling = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(InstallButtonText));
                }
            }
        }

        public string StatusMessage
        {
            get => _statusMessage;
            set
            {
                if (_statusMessage != value)
                {
                    _statusMessage = value;
                    OnPropertyChanged();
                }
            }
        }

        public string InstallButtonText => IsInstalling ? "Installing…" : (IsInstalled ? "Installed" : "Install");

        public bool ShowInstallButton => !IsInstalled;
        public bool ShowUninstallButton => IsInstalled && CanUninstall;
        public bool ShowIncludedBadge => IsInstalled && !CanUninstall;

        public event PropertyChangedEventHandler? PropertyChanged;
        public void OnPropertyChanged([CallerMemberName] string? propName = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propName));
    }
}
