using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using Launcher.Helpers;
using Launcher.Models;
using Launcher.Services;
using Launcher.ViewModels;
using Microsoft.Win32;

namespace Launcher.Views
{
    public partial class ExploreView : UserControl
    {
        private static readonly CultureInfo EnUs = new("en-US");
        private static readonly string[] Loaders = ["Vanilla", "Fabric", "Forge", "NeoForge", "Quilt"];

        private readonly InstanceService _instances = new();
        private readonly MinecraftVersionService _versions = new();
        private readonly MinecraftLaunchService _launch = new();
        private readonly GlobalContentService _global = new();
        private readonly ModpackImportService _modpackImport = new();
        private List<MinecraftVersionInfo> _allVersions = new();
        private bool _isReady;

        public ExploreView()
        {
            DataContext = new ExploreViewModel();
            InitializeComponent();
            Loaded += OnLoaded;
        }

        private ExploreViewModel Vm => (ExploreViewModel)DataContext;

        private async void OnLoaded(object sender, RoutedEventArgs e)
        {
            _isReady = true;
            RefreshList();
            if (_allVersions == null || _allVersions.Count == 0)
            {
                await LoadVersionsAsync();
            }
        }

        private async Task LoadVersionsAsync()
        {
            Vm.IsLoadingVersions = true;
            try
            {
                _allVersions = (await _versions.GetAllAsync()).ToList();
                ApplyVersionFilter();
            }
            finally
            {
                Vm.IsLoadingVersions = false;
            }
        }

        private void ApplyVersionFilter()
        {
            var filtered = MinecraftVersionService.Filter(_allVersions, Vm.VersionTypeFilter, Vm.VersionSearch)
                .Select(v => v.Id)
                .ToList();
            Vm.FilteredVersions.Clear();
            foreach (var id in filtered)
                Vm.FilteredVersions.Add(id);
            if (!filtered.Contains(Vm.NewVersion) && filtered.Count > 0)
                Vm.NewVersion = filtered[0];
        }

        private void VersionType_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not Button btn || btn.Tag is not string tag) return;
            Vm.VersionTypeFilter = tag;
            UpdateVersionTypeChips(tag);
            ApplyVersionFilter();
        }

        private void UpdateVersionTypeChips(string active)
        {
            foreach (var (btn, tag) in new[] {
                (ChipRelease, "release"), (ChipSnapshot, "snapshot"),
                (ChipBeta, "old_beta"), (ChipAlpha, "old_alpha") })
            {
                btn.Style = (Style)FindResource(tag == active ? "ExploreNavPillActive" : "ExploreNavPill");
            }
        }

        private void VersionSearch_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (!_isReady) return;
            ApplyVersionFilter();
        }

        private void VersionList_SelectionChanged(object sender, SelectionChangedEventArgs e) { }

        private void LoaderChip_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not Button btn || btn.Tag is not string loader) return;
            Vm.NewLoader = loader;
            UpdateLoaderChips(loader);
        }

        private void UpdateLoaderChips(string active)
        {
            foreach (var (btn, tag) in new[] {
                (LoaderVanilla, "Vanilla"), (LoaderFabric, "Fabric"), (LoaderForge, "Forge"),
                (LoaderNeo, "NeoForge"), (LoaderQuilt, "Quilt") })
            {
                btn.Style = (Style)FindResource(tag == active ? "ExploreNavPillActive" : "ExploreNavPill");
            }
        }

        // ── List ──────────────────────────────────────────────────────────────

        private void RefreshList()
        {
            var vm = Vm;
            var query = vm.SearchQuery.Trim().ToLowerInvariant();
            var all = _instances.GetAll().AsEnumerable();

            if (!string.IsNullOrEmpty(query))
            {
                all = all.Where(i =>
                    i.Name.ToLowerInvariant().Contains(query) ||
                    i.MinecraftVersion.Contains(query, StringComparison.OrdinalIgnoreCase) ||
                    i.Loader.ToLowerInvariant().Contains(query) ||
                    i.Tags.Any(t => t.ToLowerInvariant().Contains(query)) ||
                    i.Mods.Any(m => m.Title.ToLowerInvariant().Contains(query)) ||
                    (query == "favorites" && i.IsFavorite));
            }

            all = vm.SortMode switch
            {
                ExploreSortMode.Name => all.OrderByDescending(i => i.IsFavorite).ThenBy(i => i.Name),
                ExploreSortMode.Version => all.OrderByDescending(i => i.IsFavorite).ThenByDescending(i => i.MinecraftVersion),
                ExploreSortMode.Loader => all.OrderByDescending(i => i.IsFavorite).ThenBy(i => i.Loader),
                ExploreSortMode.ModCount => all.OrderByDescending(i => i.IsFavorite).ThenByDescending(i => i.Mods.Count),
                ExploreSortMode.Favorites => all.OrderByDescending(i => i.IsFavorite).ThenByDescending(i => i.LastPlayedAt ?? DateTime.MinValue),
                _ => all.OrderByDescending(i => i.IsFavorite).ThenByDescending(i => i.LastPlayedAt ?? DateTime.MinValue)
            };

            var allList = all.ToList();
            vm.Instances.Clear();
            foreach (var inst in allList)
            {
                vm.Instances.Add(new InstanceListItemViewModel
                {
                    Id = inst.Id,
                    Name = inst.Name,
                    Version = inst.MinecraftVersion,
                    Loader = inst.Loader,
                    ModCount = inst.Mods.Count,
                    LastPlayedText = FormatLastPlayed(inst.LastPlayedAt),
                    SizeText = _instances.GetCachedFolderSizeText(inst.Id),
                    IsFavorite = inst.IsFavorite,
                    IconColor = inst.IconColor,
                    IconLetter = inst.IconLetter,
                    LoaderIconUri = LoaderBranding.GetIconUri(inst.Loader),
                    IsSelected = inst.Id == vm.SelectedInstanceId
                });
            }
            vm.OnPropertyChangedPublic(nameof(ExploreViewModel.HasInstances));

            // Background populate folder sizes asynchronously so UI never freezes
            _ = Task.Run(() =>
            {
                foreach (var inst in allList)
                {
                    var size = _instances.GetFolderSizeText(inst.Id);
                    Dispatcher.BeginInvoke(() =>
                    {
                        var target = vm.Instances.FirstOrDefault(x => x.Id == inst.Id);
                        if (target != null && target.SizeText != size) target.SizeText = size;
                    });
                }
            });
        }

        private void SearchBox_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (!_isReady) return;
            RefreshList();
        }

        private void SortCombo_SelectionChanged(object sender, SelectionChangedEventArgs e) { }

        private void InstanceCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is not FrameworkElement fe || fe.Tag is not string id) return;
            SelectInstance(id);
        }

        private void SelectInstance(string id)
        {
            var vm = Vm;
            vm.MainMode = ExploreMainMode.InstanceDetail;
            vm.SelectedInstanceId = id;
            vm.IsCreating = false;
            vm.ShowCatalog = false;
            LoadDetail(id);

            var inst = _instances.GetById(id);
            if (string.Equals(inst?.Loader, "Vanilla", StringComparison.OrdinalIgnoreCase) &&
                (vm.DetailTab == ExploreDetailTab.Mods || vm.DetailTab == ExploreDetailTab.Catalog))
            {
                vm.DetailTab = ExploreDetailTab.Settings;
            }
            UpdateDetailTabChips();
            UpdateGlobalSidebarButtons();
            RefreshList();
        }

        private void LoadDetail(string id)
        {
            var inst = _instances.GetById(id);
            var vm = Vm;
            if (inst == null) return;

            vm.DetailName = inst.Name;
            vm.DetailVersion = inst.MinecraftVersion;
            vm.DetailLoader = inst.Loader;
            vm.DetailSize = _instances.GetCachedFolderSizeText(id);
            _ = Task.Run(() =>
            {
                var size = _instances.GetFolderSizeText(id);
                Dispatcher.BeginInvoke(() => { if (Vm.SelectedInstanceId == id) Vm.DetailSize = size; });
            });
            vm.DetailPath = _instances.GetInstanceDirectory(id);
            vm.DetailCreated = inst.CreatedAt.ToLocalTime().ToString("MMM dd, yyyy", EnUs);
            vm.DetailLastPlayed = FormatLastPlayed(inst.LastPlayedAt);
            vm.DetailModCount = inst.Mods.Count.ToString();
            vm.DetailIconColor = inst.IconColor;
            vm.DetailIconLetter = inst.IconLetter;
            vm.DetailLoaderIconUri = LoaderBranding.GetIconUri(inst.Loader);

            vm.EditName = inst.Name;
            vm.EditRam = inst.RamMb;
            vm.EditJvmArgs = inst.JvmArgs;
            vm.EditWidth = inst.Width;
            vm.EditHeight = inst.Height;
            vm.EditFullscreen = inst.Fullscreen;

            _instances.SyncModsFromDisk(inst);
            RefreshMods(inst);
            RefreshWorlds(inst);

            vm.ResourcePacks.Clear();
            foreach (var p in inst.ResourcePacks)
                vm.ResourcePacks.Add(ToContentVm(p));
            vm.Shaders.Clear();
            foreach (var s in inst.Shaders)
                vm.Shaders.Add(ToContentVm(s));

            UpdateDetailTabChips();
        }

        private void RefreshMods(MinecraftInstance inst)
        {
            var vm = Vm;
            var q = vm.ModSearch.Trim().ToLowerInvariant();
            vm.Mods.Clear();
            foreach (var m in inst.Mods.Where(m => string.IsNullOrEmpty(q) ||
                m.Title.ToLowerInvariant().Contains(q) ||
                m.Author.ToLowerInvariant().Contains(q)))
            {
                vm.Mods.Add(ToContentVm(m));
            }
            vm.OnPropertyChangedPublic(nameof(ExploreViewModel.ModCount));
            vm.OnPropertyChangedPublic(nameof(ExploreViewModel.HasInstalledMods));
            vm.OnPropertyChangedPublic(nameof(ExploreViewModel.HasNoInstalledMods));
        }

        private void RefreshWorlds(MinecraftInstance inst)
        {
            Vm.Worlds.Clear();
            var saves = _instances.GetSavesPath(inst.Id);
            if (!Directory.Exists(saves)) return;
            foreach (var dir in Directory.GetDirectories(saves))
            {
                var name = Path.GetFileName(dir);
                var levelDat = Path.Combine(dir, "level.dat");
                var last = File.Exists(levelDat) ? File.GetLastWriteTime(levelDat) : Directory.GetLastWriteTime(dir);
                long size = 0;
                try { size = Directory.EnumerateFiles(dir, "*", SearchOption.AllDirectories).Sum(f => new FileInfo(f).Length); } catch { }
                Vm.Worlds.Add(new WorldItemViewModel
                {
                    Name = name,
                    SizeText = FormatBytes(size),
                    LastOpenedText = last.ToString("MMM dd, yyyy HH:mm", EnUs),
                    Path = dir
                });
            }
        }

        // ── Create ────────────────────────────────────────────────────────────

        private void NewInstance_Click(object sender, RoutedEventArgs e)
        {
            var vm = Vm;
            vm.MainMode = ExploreMainMode.Empty;
            vm.IsCreating = true;
            vm.ShowCatalog = false;
            vm.SelectedInstanceId = null;
            vm.NewName = "";
            vm.NewLoader = "Fabric";
            vm.VersionTypeFilter = "release";
            vm.VersionSearch = "";
            UpdateVersionTypeChips("release");
            UpdateLoaderChips("Fabric");
            ApplyVersionFilter();
            UpdateGlobalSidebarButtons();
            RefreshList();
        }

        private async void GlobalTextures_Click(object sender, RoutedEventArgs e)
        {
            var vm = Vm;
            vm.MainMode = ExploreMainMode.GlobalTextures;
            vm.IsCreating = false;
            vm.SelectedInstanceId = null;
            vm.CatalogType = CatalogType.ResourcePack;
            vm.SelectedCatalogProjectId = null;
            vm.CatalogSearch = "";
            vm.StatusMessage = null;
            UpdateGlobalSidebarButtons();
            RefreshList();
            LoadGlobalContent();
            await GlobalCatalogBrowser.RefreshAsync();
        }

        private async void GlobalShaders_Click(object sender, RoutedEventArgs e)
        {
            var vm = Vm;
            vm.MainMode = ExploreMainMode.GlobalShaders;
            vm.IsCreating = false;
            vm.SelectedInstanceId = null;
            vm.CatalogType = CatalogType.Shader;
            vm.SelectedCatalogProjectId = null;
            vm.CatalogSearch = "";
            vm.StatusMessage = null;
            UpdateGlobalSidebarButtons();
            RefreshList();
            LoadGlobalContent();
            await GlobalCatalogBrowser.RefreshAsync();
        }

        private void UpdateGlobalSidebarButtons()
        {
            var mode = Vm.MainMode;
            GlobalTexturesBtn.Style = (Style)FindResource(
                mode == ExploreMainMode.GlobalTextures ? "ExploreGlobalNavChipActive" : "ExploreGlobalNavChip");
            GlobalShadersBtn.Style = (Style)FindResource(
                mode == ExploreMainMode.GlobalShaders ? "ExploreGlobalNavChipActive" : "ExploreGlobalNavChip");
        }

        private void AttachModpack_Click(object sender, RoutedEventArgs e)
        {
            var dlg = new OpenFileDialog
            {
                Title = "Attach modpack",
                Filter = "Modpack archives (*.mrpack;*.zip)|*.mrpack;*.zip|All files (*.*)|*.*"
            };
            if (dlg.ShowDialog() != true)
                return;

            Vm.StatusMessage = "Importing modpack…";
            var result = _modpackImport.Import(dlg.FileName);
            if (!result.Success || result.Instance == null)
            {
                MessageBox.Show(result.Message, "Modpack", MessageBoxButton.OK, MessageBoxImage.Warning);
                Vm.StatusMessage = result.Message;
                return;
            }

            Vm.StatusMessage = result.Message;
            RefreshList();
            SelectInstance(result.Instance.Id);
        }

        private GlobalContentKind CurrentGlobalKind() =>
            Vm.MainMode == ExploreMainMode.GlobalTextures
                ? GlobalContentKind.ResourcePack
                : GlobalContentKind.Shader;

        private void LoadGlobalContent()
        {
            var kind = CurrentGlobalKind();
            var store = _global.Load();
            var list = _global.GetList(store, kind)
                .OrderBy(x => x.Title, StringComparer.OrdinalIgnoreCase)
                .ToList();

            Vm.GlobalInstalled.Clear();
            foreach (var item in list)
                Vm.GlobalInstalled.Add(ToContentVm(item));
            Vm.OnPropertyChangedPublic(nameof(ExploreViewModel.HasNoGlobalInstalled));
        }

        private void OpenGlobalFolder_Click(object sender, RoutedEventArgs e)
        {
            var dir = _global.GetDirectory(CurrentGlobalKind());
            Directory.CreateDirectory(dir);
            OpenPath(dir);
        }

        private void AddGlobalFile_Click(object sender, RoutedEventArgs e)
        {
            var dlg = new OpenFileDialog
            {
                Title = "Add pack",
                Filter = "Pack archives (*.zip)|*.zip|All files (*.*)|*.*"
            };
            if (dlg.ShowDialog() != true) return;

            var kind = CurrentGlobalKind();
            var dir = _global.GetDirectory(kind);
            Directory.CreateDirectory(dir);
            var fileName = Path.GetFileName(dlg.FileName);
            var dest = Path.Combine(dir, fileName);
            File.Copy(dlg.FileName, dest, overwrite: true);

            var store = _global.Load();
            var list = _global.GetList(store, kind);
            list.RemoveAll(x => string.Equals(x.FileName, fileName, StringComparison.OrdinalIgnoreCase));
            list.Add(new InstalledContent
            {
                Title = Path.GetFileNameWithoutExtension(fileName),
                FileName = fileName
            });
            _global.Save(store);
            _global.SyncToAllInstanceDirectories(_instances);
            LoadGlobalContent();
            Vm.StatusMessage = $"{fileName} added.";
        }

        private void RemoveGlobal_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not Button btn || btn.Tag is not string fileName) return;

            var kind = CurrentGlobalKind();
            var store = _global.Load();
            var list = _global.GetList(store, kind);
            list.RemoveAll(x => string.Equals(x.FileName, fileName, StringComparison.OrdinalIgnoreCase));
            _global.Save(store);

            var path = Path.Combine(_global.GetDirectory(kind), fileName);
            try
            {
                if (File.Exists(path))
                    File.Delete(path);
                else if (Directory.Exists(path))
                    Directory.Delete(path, recursive: true);
            }
            catch { }

            _global.RemoveFileFromAllInstances(_instances, kind, fileName);
            LoadGlobalContent();
            Vm.StatusMessage = $"{fileName} removed.";
        }

        private void CancelCreate_Click(object sender, RoutedEventArgs e)
        {
            Vm.IsCreating = false;
        }

        private void ConfirmCreate_Click(object sender, RoutedEventArgs e)
        {
            var name = Vm.NewName.Trim();
            if (string.IsNullOrEmpty(name))
            {
                MessageBox.Show("Enter a name for this installation.", "Flow", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }
            var version = Vm.NewVersion;
            var loader = Vm.NewLoader;
            if (string.IsNullOrEmpty(version))
            {
                MessageBox.Show("Select a Minecraft version.", "Flow", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }
            var inst = _instances.Create(name, version, loader);
            Vm.IsCreating = false;
            SelectInstance(inst.Id);
        }

        // ── Actions ───────────────────────────────────────────────────────────

        private async void Play_Click(object sender, RoutedEventArgs e)
        {
            var id = GetActionId(sender) ?? Vm.SelectedInstanceId;
            if (id == null) return;

            Vm.StatusMessage = "Preparing launch…";

            try
            {
                var progress = new Progress<string>(msg => Vm.StatusMessage = msg);
                var result = await _launch.LaunchAsync(id, progress);

                if (!result.Success)
                {
                    MessageBox.Show(result.Message, "Launch failed",
                        MessageBoxButton.OK, MessageBoxImage.Warning);
                    Vm.StatusMessage = result.Message;
                }
                else
                {
                    Vm.StatusMessage = "Minecraft is running.";
                    LoadDetail(id);
                    RefreshList();
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.Message, "Launch failed",
                    MessageBoxButton.OK, MessageBoxImage.Error);
                Vm.StatusMessage = ex.Message;
            }
        }

        private void Favorite_Click(object sender, RoutedEventArgs e)
        {
            var id = GetActionId(sender) ?? Vm.SelectedInstanceId;
            if (id == null) return;
            _instances.ToggleFavorite(id);
            if (Vm.SelectedInstanceId == id) LoadDetail(id);
            RefreshList();
        }

        private void OpenFolder_Click(object sender, RoutedEventArgs e)
        {
            var id = Vm.SelectedInstanceId;
            if (id == null) return;
            OpenPath(_instances.GetInstanceDirectory(id));
        }

        private void Duplicate_Click(object sender, RoutedEventArgs e)
        {
            var inst = _instances.GetById(Vm.SelectedInstanceId ?? "");
            if (inst == null) return;
            var copy = _instances.Duplicate(inst);
            SelectInstance(copy.Id);
        }

        private void Delete_Click(object sender, RoutedEventArgs e)
        {
            var id = Vm.SelectedInstanceId;
            if (id == null) return;
            var inst = _instances.GetById(id);
            if (inst == null) return;
            if (MessageBox.Show($"Delete \"{inst.Name}\"?", "Confirm", MessageBoxButton.YesNo, MessageBoxImage.Warning) != MessageBoxResult.Yes)
                return;
            _instances.Delete(id);
            Vm.SelectedInstanceId = null;
            RefreshList();
        }

        private void SaveSettings_Click(object sender, RoutedEventArgs e)
        {
            var inst = _instances.GetById(Vm.SelectedInstanceId ?? "");
            if (inst == null) return;
            inst.Name = Vm.EditName.Trim();
            inst.RamMb = int.TryParse(Vm.EditRam.ToString(), out var ram) ? ram : inst.RamMb;
            inst.JvmArgs = Vm.EditJvmArgs;
            inst.Width = Vm.EditWidth;
            inst.Height = Vm.EditHeight;
            inst.Fullscreen = Vm.EditFullscreen;
            inst.IconLetter = string.IsNullOrEmpty(inst.Name) ? "F" : inst.Name[..1].ToUpper();
            _instances.Save(inst);
            LoadDetail(inst.Id);
            if (!Vm.ShowModsTabs && (Vm.DetailTab == ExploreDetailTab.Mods || Vm.DetailTab == ExploreDetailTab.Catalog))
                Vm.DetailTab = ExploreDetailTab.Settings;
            UpdateDetailTabChips();
            RefreshList();
            Vm.StatusMessage = "Settings saved.";
        }

        private async void Tab_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not Button btn || btn.Tag is not string tab) return;

            Vm.DetailTab = tab switch
            {
                "mods" => ExploreDetailTab.Mods,
                "catalog" => ExploreDetailTab.Catalog,
                "packs" => ExploreDetailTab.ResourcePacks,
                "shaders" => ExploreDetailTab.Shaders,
                "worlds" => ExploreDetailTab.Worlds,
                "settings" => ExploreDetailTab.Settings,
                _ => ExploreDetailTab.Settings
            };

            UpdateDetailTabChips();

            if (Vm.DetailTab == ExploreDetailTab.Mods)
            {
                Vm.CatalogType = CatalogType.Mod;
                Vm.SelectedCatalogProjectId = null;
                await InstanceCatalogBrowser.RefreshAsync();
            }
        }

        private void CatalogBrowser_InstallCompleted(object? sender, EventArgs e)
        {
            if (Vm.ShowGlobalPanel)
            {
                LoadGlobalContent();
                return;
            }

            var inst = _instances.GetById(Vm.SelectedInstanceId ?? "");
            if (inst == null) return;

            _instances.Reload();
            inst = _instances.GetById(Vm.SelectedInstanceId ?? "");
            if (inst == null) return;

            Vm.DetailTab = ExploreDetailTab.Catalog;
            UpdateDetailTabChips();
            LoadDetail(inst.Id);
            RefreshList();
        }

        private void UpdateDetailTabChips()
        {
            if (!IsInitialized) return;
            var tab = Vm.DetailTab;
            TabSettings.Style = (Style)FindResource(tab == ExploreDetailTab.Settings ? "ExploreNavPillActive" : "ExploreNavPill");
            TabMods.Style = (Style)FindResource(tab == ExploreDetailTab.Mods ? "ExploreNavPillActive" : "ExploreNavPill");
            TabCatalog.Style = (Style)FindResource(tab == ExploreDetailTab.Catalog ? "ExploreNavPillActive" : "ExploreNavPill");
        }

        private void ModSearch_TextChanged(object sender, TextChangedEventArgs e)
        {
            var inst = _instances.GetById(Vm.SelectedInstanceId ?? "");
            if (inst != null) RefreshMods(inst);
        }

        private void ToggleMod_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not Button btn || btn.Tag is not string fileName) return;
            var inst = _instances.GetById(Vm.SelectedInstanceId ?? "");
            if (inst == null) return;
            var mod = inst.Mods.FirstOrDefault(m => m.FileName == fileName);
            if (mod == null) return;
            mod.Enabled = !mod.Enabled;
            var modsDir = _instances.GetModsPath(inst.Id);
            var enabled = Path.Combine(modsDir, mod.FileName);
            var disabled = Path.Combine(modsDir, mod.FileName + ".disabled");
            try
            {
                if (mod.Enabled && File.Exists(disabled)) File.Move(disabled, enabled);
                else if (!mod.Enabled && File.Exists(enabled)) File.Move(enabled, disabled);
            }
            catch { }
            _instances.Save(inst);
            RefreshMods(inst);
        }

        private void RemoveMod_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not Button btn || btn.Tag is not string fileName) return;
            var inst = _instances.GetById(Vm.SelectedInstanceId ?? "");
            if (inst == null) return;
            var mod = inst.Mods.FirstOrDefault(m => m.FileName == fileName);
            if (mod == null) return;
            try
            {
                var path = Path.Combine(_instances.GetModsPath(inst.Id), mod.FileName);
                if (File.Exists(path)) File.Delete(path);
                var dis = path + ".disabled";
                if (File.Exists(dis)) File.Delete(dis);
            }
            catch { }
            inst.Mods.RemoveAll(m => m.FileName == fileName);
            _instances.Save(inst);
            RefreshMods(inst);
            RefreshList();
        }

        private void OpenModrinth_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button { Tag: string slug } && !string.IsNullOrEmpty(slug))
                OpenUrl($"https://modrinth.com/mod/{slug}");
        }

        private void OpenWorldFolder_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button { Tag: string path }) OpenPath(path);
        }

        private void OpenModsFolder_Click(object sender, RoutedEventArgs e)
        {
            var id = Vm.SelectedInstanceId;
            if (id == null) return;
            var dir = _instances.GetModsPath(id);
            Directory.CreateDirectory(dir);
            OpenPath(dir);
        }

        private void RamPreset_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button { Tag: string s } && int.TryParse(s, out var mb))
                Vm.EditRam = mb;
        }

        private void ResPreset_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button { Tag: string s })
            {
                var parts = s.Split('x');
                if (parts.Length == 2 && int.TryParse(parts[0], out var w) && int.TryParse(parts[1], out var h))
                {
                    Vm.EditWidth = w;
                    Vm.EditHeight = h;
                }
            }
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        private static string? GetActionId(object sender) =>
            sender is FrameworkElement { Tag: string id } ? id : null;

        private static InstalledContentViewModel ToContentVm(InstalledContent m) => new()
        {
            ProjectId = m.ProjectId,
            Slug = m.Slug,
            Title = m.Title,
            Author = m.Author,
            VersionNumber = m.VersionNumber,
            IconUrl = m.IconUrl,
            Enabled = m.Enabled,
            IsFavorite = m.IsFavorite,
            UpdateAvailable = m.UpdateAvailable,
            FileName = m.FileName
        };

        private static string FormatLastPlayed(DateTime? dt)
        {
            if (dt == null) return "Never";
            var local = dt.Value.ToLocalTime();
            var diff = DateTime.Now - local;
            if (diff.TotalMinutes < 60) return $"{(int)diff.TotalMinutes}m ago";
            if (diff.TotalHours < 24) return $"{(int)diff.TotalHours}h ago";
            if (diff.TotalDays < 7) return $"{(int)diff.TotalDays}d ago";
            return local.ToString("MMM dd", EnUs);
        }

        private static string FormatBytes(long bytes)
        {
            if (bytes < 1024 * 1024) return $"{bytes / 1024.0:0.#} KB";
            if (bytes < 1024 * 1024 * 1024) return $"{bytes / (1024.0 * 1024):0.#} MB";
            return $"{bytes / (1024.0 * 1024 * 1024):0.#} GB";
        }

        private static void OpenPath(string path)
        {
            try { Process.Start(new ProcessStartInfo { FileName = path, UseShellExecute = true }); }
            catch { try { Process.Start("explorer.exe", path); } catch { } }
        }

        private static void OpenUrl(string url)
        {
            try { Process.Start(new ProcessStartInfo { FileName = url, UseShellExecute = true }); }
            catch { }
        }
    }
}
