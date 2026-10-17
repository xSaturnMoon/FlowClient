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
                }
            }
        }

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
                    MinecraftVersion = inst.MinecraftVersion,
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
                }
            }
        }

        public bool IsInfoTabSelected => _settingsTab == "Info";
        public bool IsModsTabSelected => _settingsTab.Equals("Mods", StringComparison.OrdinalIgnoreCase) || _settingsTab.Equals("Mod", StringComparison.OrdinalIgnoreCase);
        public bool IsModTabSelected => IsModsTabSelected;
        public bool IsTexturesTabSelected => _settingsTab.Equals("Textures", StringComparison.OrdinalIgnoreCase);
        public bool IsShadersTabSelected => _settingsTab.Equals("Shaders", StringComparison.OrdinalIgnoreCase);
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
            if (SelectedSettingsInstance == null) return;
            var dir = _instanceService.GetModsPath(SelectedSettingsInstance.Id);
            System.IO.Directory.CreateDirectory(dir);
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
            {
                FileName = dir,
                UseShellExecute = true
            });
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
            var inst = _instanceService.GetById(item.Id);
            if (inst != null)
            {
                _settingsInstanceName = inst.Name;
                _settingsRamMb = inst.RamMb > 0 ? inst.RamMb : 4096;
                _settingsJvmArgs = inst.JvmArgs ?? "";

                _instanceService.SyncModsFromDisk(inst);
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
}
