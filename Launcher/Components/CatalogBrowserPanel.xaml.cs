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

namespace Launcher.Components
{
    public partial class CatalogBrowserPanel : UserControl
    {
        private readonly InstanceService _instances = new();
        private readonly ModrinthService _modrinth = new();
        private readonly ModInstallService _modInstall = new();
        private readonly GlobalContentService _global = new();
        private CancellationTokenSource? _cts;

        public event EventHandler? InstallCompleted;

        public CatalogBrowserPanel()
        {
            InitializeComponent();
            Loaded += (_, _) =>
            {
                UpdateCatalogTabChips();
                AttachWheelForwarding(DescriptionWebView);
                AttachWheelForwarding(ChangelogWebView);
            };
        }

        private void AttachWheelForwarding(Microsoft.Web.WebView2.Wpf.WebView2 webView)
        {
            webView.PreviewMouseWheel += (_, e) =>
            {
                e.Handled = true;
                var bubble = new MouseWheelEventArgs(e.MouseDevice, e.Timestamp, e.Delta)
                {
                    RoutedEvent = UIElement.MouseWheelEvent,
                    Source = webView
                };
                DetailScrollViewer.RaiseEvent(bubble);
            };
        }

        private ExploreViewModel Vm => (ExploreViewModel)DataContext;

        private async void CatalogSearch_TextChanged(object sender, TextChangedEventArgs e)
        {
            _cts?.Cancel();
            _cts = new CancellationTokenSource();
            var token = _cts.Token;
            try
            {
                await Task.Delay(300, token);
                if (!token.IsCancellationRequested)
                    await SearchCatalogAsync();
            }
            catch (OperationCanceledException) { }
        }

        private async void CatalogList_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (CatalogList.SelectedItem is not CatalogHitViewModel hit)
                return;

            await LoadCatalogProjectAsync(hit.ProjectId);
        }

        private async Task SearchCatalogAsync()
        {
            var vm = Vm;
            vm.IsBusy = true;
            try
            {
                _cts?.Cancel();
                _cts = new CancellationTokenSource();

                var isGlobal = vm.ShowGlobalPanel;
                var inst = _instances.GetById(vm.SelectedInstanceId ?? "");
                if (!isGlobal && inst == null) return;

                var type = isGlobal
                    ? (vm.MainMode == ExploreMainMode.GlobalTextures ? "resourcepack" : "shader")
                    : vm.CatalogType switch
                    {
                        CatalogType.ResourcePack => "resourcepack",
                        CatalogType.Shader => "shader",
                        _ => "mod"
                    };

                var hits = await _modrinth.SearchAsync(
                    vm.CatalogSearch,
                    type,
                    isGlobal ? null : inst!.MinecraftVersion,
                    isGlobal ? null : (inst!.Loader == "Vanilla" ? null : inst.Loader),
                    24,
                    _cts.Token);

                vm.CatalogHits.Clear();
                foreach (var h in hits)
                {
                    vm.CatalogHits.Add(new CatalogHitViewModel
                    {
                        ProjectId = h.ProjectId,
                        Slug = h.Slug,
                        Title = h.Title,
                        IconUrl = h.IconUrl,
                        DownloadsText = ModrinthFormatHelper.FormatCount(h.Downloads)
                    });
                }

                if (vm.CatalogHits.Count > 0)
                {
                    CatalogList.SelectedIndex = 0;
                }
                else
                {
                    vm.SelectedCatalogProjectId = null;
                    vm.CatalogVersions.Clear();
                }
            }
            catch (OperationCanceledException) { }
            catch (Exception ex) { vm.StatusMessage = ex.Message; }
            finally { vm.IsBusy = false; }
        }

        private async Task LoadCatalogProjectAsync(string projectId)
        {
            var vm = Vm;
            vm.SelectedCatalogProjectId = projectId;
            vm.IsBusy = true;
            try
            {
                var inst = _instances.GetById(vm.SelectedInstanceId ?? "");
                var project = await _modrinth.GetProjectAsync(projectId);
                if (project == null) return;

                vm.CatalogDetailTitle = project.Title;
                vm.CatalogDetailDesc = project.Description;
                vm.CatalogDetailBody = ModrinthMarkdownParser.StripToPlainText(project.Body);
                vm.CatalogDetailIcon = project.IconUrl;
                vm.CatalogDetailDownloadsText = ModrinthFormatHelper.FormatCount(project.Downloads);
                vm.CatalogDetailFollowersText = ModrinthFormatHelper.FormatCount(project.Followers);
                vm.CatalogDetailTab = CatalogDetailTab.Description;
                UpdateCatalogTabChips();

                vm.CatalogDetailCategories.Clear();
                foreach (var cat in project.Categories.Take(8))
                    vm.CatalogDetailCategories.Add(cat);

                var versions = await _modrinth.GetVersionsAsync(
                    projectId,
                    vm.ShowGlobalPanel ? null : inst?.MinecraftVersion,
                    vm.ShowGlobalPanel ? null : (inst?.Loader == "Vanilla" ? null : inst?.Loader));

                var installedVersionId = GetInstalledVersionId(vm, projectId);

                vm.CatalogVersions.Clear();
                foreach (var v in versions.Take(20))
                {
                    vm.CatalogVersions.Add(new CatalogVersionViewModel
                    {
                        Id = v.Id,
                        Name = v.Name,
                        VersionNumber = v.VersionNumber,
                        LoadersText = string.Join(", ", v.Loaders),
                        Changelog = v.Changelog,
                        IsInstalled = v.Id == installedVersionId
                    });
                }

                vm.CatalogLatestVersionId = versions.FirstOrDefault()?.Id;

                var changelogRaw = versions.FirstOrDefault()?.Changelog;
                await CatalogHtmlHelper.ShowAsync(DescriptionWebView, project.Body);
                await CatalogHtmlHelper.ShowAsync(ChangelogWebView, changelogRaw);

                UpdateInstalledState(vm, projectId);
            }
            catch (Exception ex) { vm.StatusMessage = ex.Message; }
            finally { vm.IsBusy = false; }
        }

        private void CatalogTab_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not Button btn || btn.Tag is not string tag) return;
            Vm.CatalogDetailTab = tag switch
            {
                "changelog" => CatalogDetailTab.Changelog,
                "versions" => CatalogDetailTab.Versions,
                _ => CatalogDetailTab.Description
            };
            UpdateCatalogTabChips();
        }

        private void UpdateCatalogTabChips()
        {
            if (!IsInitialized) return;
            var tab = Vm.CatalogDetailTab;
            TabDesc.Style = (Style)FindResource(tab == CatalogDetailTab.Description ? "ExploreCatalogTabActive" : "ExploreCatalogTab");
            TabChangelog.Style = (Style)FindResource(tab == CatalogDetailTab.Changelog ? "ExploreCatalogTabActive" : "ExploreCatalogTab");
            TabVersions.Style = (Style)FindResource(tab == CatalogDetailTab.Versions ? "ExploreCatalogTabActive" : "ExploreCatalogTab");
        }

        private void UpdatePrimaryActionButton()
        {
            if (!IsInitialized) return;
            var vm = Vm;
            PrimaryActionBtn.Content = vm.CatalogPrimaryActionLabel;
            PrimaryActionBtn.Style = (Style)FindResource(vm.CatalogIsInstalled ? "ExploreCatalogUninstallBtn" : "ExploreCatalogDownloadBtn");
        }

        private void UpdateInstalledState(ExploreViewModel vm, string projectId)
        {
            if (vm.ShowGlobalPanel)
            {
                var kind = vm.MainMode == ExploreMainMode.GlobalTextures
                    ? GlobalContentKind.ResourcePack
                    : GlobalContentKind.Shader;
                var store = _global.Load();
                var installed = _global.GetList(store, kind).FirstOrDefault(x => x.ProjectId == projectId);
                vm.CatalogIsInstalled = installed != null;
                vm.CatalogInstalledFileName = installed?.FileName;
            }
            else
            {
                var inst = _instances.GetById(vm.SelectedInstanceId ?? "");
                InstalledContent? installed = null;
                if (inst != null)
                {
                    installed = vm.CatalogType switch
                    {
                        CatalogType.ResourcePack => inst.ResourcePacks.FirstOrDefault(m => m.ProjectId == projectId),
                        CatalogType.Shader => inst.Shaders.FirstOrDefault(m => m.ProjectId == projectId),
                        _ => inst.Mods.FirstOrDefault(m => m.ProjectId == projectId)
                    };
                }

                vm.CatalogIsInstalled = installed != null;
                vm.CatalogInstalledFileName = installed?.FileName;
            }

            var installedVersionId = GetInstalledVersionId(vm, projectId);
            foreach (var ver in vm.CatalogVersions)
                ver.IsInstalled = ver.Id == installedVersionId;

            UpdatePrimaryActionButton();
        }

        private string? GetInstalledVersionId(ExploreViewModel vm, string projectId)
        {
            if (vm.ShowGlobalPanel)
            {
                var kind = vm.MainMode == ExploreMainMode.GlobalTextures
                    ? GlobalContentKind.ResourcePack
                    : GlobalContentKind.Shader;
                return _global.GetList(_global.Load(), kind).FirstOrDefault(x => x.ProjectId == projectId)?.VersionId;
            }

            var inst = _instances.GetById(vm.SelectedInstanceId ?? "");
            if (inst == null) return null;

            return vm.CatalogType switch
            {
                CatalogType.ResourcePack => inst.ResourcePacks.FirstOrDefault(m => m.ProjectId == projectId)?.VersionId,
                CatalogType.Shader => inst.Shaders.FirstOrDefault(m => m.ProjectId == projectId)?.VersionId,
                _ => inst.Mods.FirstOrDefault(m => m.ProjectId == projectId)?.VersionId
            };
        }

        private async void PrimaryAction_Click(object sender, RoutedEventArgs e)
        {
            if (Vm.CatalogIsInstalled)
            {
                UninstallCurrent();
                return;
            }

            if (!string.IsNullOrEmpty(Vm.CatalogLatestVersionId))
                await InstallVersionCore(Vm.CatalogLatestVersionId);
        }

        private async void VersionAction_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not Button btn || btn.Tag is not string versionId) return;
            var ver = Vm.CatalogVersions.FirstOrDefault(v => v.Id == versionId);
            if (ver?.IsInstalled == true)
            {
                UninstallCurrent();
                return;
            }

            await InstallVersionCore(versionId);
        }

        private async Task InstallVersionCore(string versionId)
        {
            if (Vm.ShowGlobalPanel)
            {
                await InstallGlobalVersionAsync(versionId);
                if (!string.IsNullOrEmpty(Vm.SelectedCatalogProjectId))
                    UpdateInstalledState(Vm, Vm.SelectedCatalogProjectId);
                return;
            }

            var inst = _instances.GetById(Vm.SelectedInstanceId ?? "");
            if (inst == null) return;

            var vm = Vm;
            vm.IsBusy = true;
            vm.StatusMessage = "Downloading…";
            try
            {
                _cts = new CancellationTokenSource();
                var progress = new Progress<string>(message => vm.StatusMessage = message);

                if (vm.CatalogType == CatalogType.Mod)
                {
                    var result = await _modInstall.InstallModAsync(inst, versionId, progress, _cts.Token);
                    if (result.Installed.Count == 0)
                    {
                        vm.StatusMessage = "Install failed.";
                        return;
                    }

                    if (result.MainMod != null)
                    {
                        var dependencyCount = result.Installed.Count - 1;
                        vm.StatusMessage = dependencyCount > 0
                            ? $"{result.MainMod.Title} installed with {dependencyCount} dependencies."
                            : $"{result.MainMod.Title} installed.";
                    }
                    else
                    {
                        vm.StatusMessage = $"Installed {result.Installed.Count} dependencies.";
                    }

                    UpdateInstalledState(vm, vm.SelectedCatalogProjectId!);
                    InstallCompleted?.Invoke(this, EventArgs.Empty);
                    return;
                }

                var versions = await _modrinth.GetVersionsAsync(
                    vm.SelectedCatalogProjectId!,
                    inst.MinecraftVersion,
                    inst.Loader == "Vanilla" ? null : inst.Loader,
                    _cts.Token);

                var version = versions.FirstOrDefault(v => v.Id == versionId);
                if (version == null) return;

                var file = version.Files.FirstOrDefault(f => f.Primary) ?? version.Files.FirstOrDefault();
                if (file == null) return;

                var bytes = await _modrinth.DownloadFileAsync(file.Url, _cts.Token);
                var targetDir = vm.CatalogType switch
                {
                    CatalogType.ResourcePack => _instances.GetResourcePacksPath(inst.Id),
                    CatalogType.Shader => _instances.GetShadersPath(inst.Id),
                    _ => _instances.GetModsPath(inst.Id)
                };
                Directory.CreateDirectory(targetDir);
                var dest = Path.Combine(targetDir, file.Filename);
                await File.WriteAllBytesAsync(dest, bytes, _cts.Token);

                var project = await _modrinth.GetProjectAsync(vm.SelectedCatalogProjectId!);
                var content = new InstalledContent
                {
                    ProjectId = vm.SelectedCatalogProjectId!,
                    Slug = project?.Slug ?? "",
                    Title = project?.Title ?? file.Filename,
                    Author = "",
                    VersionId = version.Id,
                    VersionNumber = version.VersionNumber,
                    FileName = file.Filename,
                    IconUrl = project?.IconUrl ?? ""
                };

                var list = vm.CatalogType switch
                {
                    CatalogType.ResourcePack => inst.ResourcePacks,
                    CatalogType.Shader => inst.Shaders,
                    _ => inst.Mods
                };
                list.RemoveAll(x => x.ProjectId == content.ProjectId);
                list.Add(content);
                _instances.Save(inst);

                vm.StatusMessage = $"{content.Title} installed.";
                UpdateInstalledState(vm, vm.SelectedCatalogProjectId!);
                InstallCompleted?.Invoke(this, EventArgs.Empty);
            }
            catch (Exception ex)
            {
                vm.StatusMessage = $"Error: {ex.Message}";
                MessageBox.Show(ex.Message, "Install", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
            finally { vm.IsBusy = false; }
        }

        private void UninstallCurrent()
        {
            var vm = Vm;
            var projectId = vm.SelectedCatalogProjectId;
            if (string.IsNullOrEmpty(projectId) || string.IsNullOrEmpty(vm.CatalogInstalledFileName))
                return;

            if (vm.ShowGlobalPanel)
                UninstallGlobal(projectId, vm.CatalogInstalledFileName);
            else
                UninstallInstance(projectId, vm.CatalogInstalledFileName);

            UpdateInstalledState(vm, projectId);
            vm.StatusMessage = $"{vm.CatalogDetailTitle} uninstalled.";
            InstallCompleted?.Invoke(this, EventArgs.Empty);
        }

        private void UninstallInstance(string projectId, string fileName)
        {
            var inst = _instances.GetById(Vm.SelectedInstanceId ?? "");
            if (inst == null) return;

            var dir = Vm.CatalogType switch
            {
                CatalogType.ResourcePack => _instances.GetResourcePacksPath(inst.Id),
                CatalogType.Shader => _instances.GetShadersPath(inst.Id),
                _ => _instances.GetModsPath(inst.Id)
            };

            TryDeleteFile(Path.Combine(dir, fileName));
            TryDeleteFile(Path.Combine(dir, fileName + ".disabled"));

            var list = Vm.CatalogType switch
            {
                CatalogType.ResourcePack => inst.ResourcePacks,
                CatalogType.Shader => inst.Shaders,
                _ => inst.Mods
            };
            list.RemoveAll(m => m.ProjectId == projectId || m.FileName == fileName);
            _instances.Save(inst);
        }

        private void UninstallGlobal(string projectId, string fileName)
        {
            var kind = Vm.MainMode == ExploreMainMode.GlobalTextures
                ? GlobalContentKind.ResourcePack
                : GlobalContentKind.Shader;

            var dir = _global.GetDirectory(kind);
            TryDeleteFile(Path.Combine(dir, fileName));

            var store = _global.Load();
            var list = _global.GetList(store, kind);
            list.RemoveAll(x => x.ProjectId == projectId || x.FileName == fileName);
            _global.Save(store);
            _global.RemoveFileFromAllInstances(_instances, kind, fileName);
        }

        private static void TryDeleteFile(string path)
        {
            try
            {
                if (File.Exists(path))
                    File.Delete(path);
            }
            catch { }
        }

        private async Task InstallGlobalVersionAsync(string versionId)
        {
            var vm = Vm;
            vm.IsBusy = true;
            vm.StatusMessage = "Downloading…";
            try
            {
                _cts = new CancellationTokenSource();
                var versions = await _modrinth.GetVersionsAsync(
                    vm.SelectedCatalogProjectId!,
                    null,
                    null,
                    _cts.Token);

                var version = versions.FirstOrDefault(v => v.Id == versionId);
                if (version == null) return;

                var file = version.Files.FirstOrDefault(f => f.Primary) ?? version.Files.FirstOrDefault();
                if (file == null) return;

                var bytes = await _modrinth.DownloadFileAsync(file.Url, _cts.Token);
                var kind = vm.MainMode == ExploreMainMode.GlobalTextures
                    ? GlobalContentKind.ResourcePack
                    : GlobalContentKind.Shader;
                var targetDir = _global.GetDirectory(kind);
                Directory.CreateDirectory(targetDir);
                var dest = Path.Combine(targetDir, file.Filename);
                await File.WriteAllBytesAsync(dest, bytes, _cts.Token);

                var project = await _modrinth.GetProjectAsync(vm.SelectedCatalogProjectId!);
                var content = new InstalledContent
                {
                    ProjectId = vm.SelectedCatalogProjectId!,
                    Slug = project?.Slug ?? "",
                    Title = project?.Title ?? file.Filename,
                    Author = "",
                    VersionId = version.Id,
                    VersionNumber = version.VersionNumber,
                    FileName = file.Filename,
                    IconUrl = project?.IconUrl ?? ""
                };

                var store = _global.Load();
                var list = _global.GetList(store, kind);
                list.RemoveAll(x => x.ProjectId == content.ProjectId);
                list.Add(content);
                _global.Save(store);
                _global.SyncToAllInstanceDirectories(_instances);

                vm.StatusMessage = $"{content.Title} installed globally.";
                if (!string.IsNullOrEmpty(vm.SelectedCatalogProjectId))
                    UpdateInstalledState(vm, vm.SelectedCatalogProjectId);
                InstallCompleted?.Invoke(this, EventArgs.Empty);
            }
            catch (Exception ex)
            {
                vm.StatusMessage = $"Error: {ex.Message}";
                MessageBox.Show(ex.Message, "Install", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
            finally { vm.IsBusy = false; }
        }

        public async Task RefreshAsync()
        {
            await SearchCatalogAsync();
        }
    }
}
