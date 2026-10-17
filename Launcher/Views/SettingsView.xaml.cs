using System;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using Microsoft.Win32;
using Launcher.Services;
using Launcher.ViewModels;

namespace Launcher.Views
{
    public partial class SettingsView : UserControl
    {
        private readonly FlowClientBrandingService _branding = new();
        private readonly InstanceService _instances = new();
        private bool _loading;

        public SettingsView()
        {
            InitializeComponent();
            Loaded += OnLoaded;
            Vm.PropertyChanged += OnSettingPropertyChanged;
        }

        private SettingsViewModel Vm => (SettingsViewModel)DataContext;

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            _loading = true;
            var settings = LauncherSettingsService.Instance.Current;

            Vm.PostLaunchAction = settings.PostLaunchAction;
            Vm.EnableFlowClientMod = settings.EnableFlowClientMod;
            Vm.EnableVoiceChatMod = settings.EnableVoiceChatMod;
            Vm.UiAnimations = settings.UiAnimations;
            Vm.AutoCheckUpdates = settings.AutoCheckUpdates;
            Vm.ConfirmBeforeStop = settings.ConfirmBeforeStop;
            Vm.EnableDiscordRpc = settings.EnableDiscordRpc;
            Vm.DefaultRamMb = settings.DefaultRamMb > 0 ? settings.DefaultRamMb : 4096;
            Vm.CustomJvmArgs = settings.CustomJvmArgs;
            Vm.CustomJavaPath = settings.CustomJavaPath;
            Vm.GameWidth = settings.GameWidth > 0 ? settings.GameWidth : 1280;
            Vm.GameHeight = settings.GameHeight > 0 ? settings.GameHeight : 720;
            Vm.IsGameFullscreen = settings.IsGameFullscreen;

            Vm.LoadSystemInfo();
            _ = AutoCheckUpdateSilentlyAsync();
            _ = Vm.RefreshStorageAnalysisAsync();

            _loading = false;
        }

        private async Task AutoCheckUpdateSilentlyAsync()
        {
            try
            {
                var updates = new UpdateService();
                var res = await updates.CheckAsync();
                if (res.UpdateAvailable)
                {
                    _latestUpdateResult = res;
                    Vm.LatestVersion = res.LatestVersion ?? "Nuova versione";
                    Vm.IsUpdateAvailable = true;
                    Vm.UpdateCheckMessage = $"Nuova versione disponibile: v{res.LatestVersion}!";
                }
            }
            catch { }
        }

        private void OnSettingPropertyChanged(object? sender, PropertyChangedEventArgs e)
        {
            if (_loading || e.PropertyName is null)
                return;

            switch (e.PropertyName)
            {
                case nameof(SettingsViewModel.PostLaunchAction):
                case nameof(SettingsViewModel.CloseLauncherOnLaunch):
                case nameof(SettingsViewModel.MinimizeLauncherOnLaunch):
                case nameof(SettingsViewModel.EnableFlowClientMod):
                case nameof(SettingsViewModel.EnableVoiceChatMod):
                case nameof(SettingsViewModel.EnableDiscordRpc):
                case nameof(SettingsViewModel.UiAnimations):
                case nameof(SettingsViewModel.AutoCheckUpdates):
                case nameof(SettingsViewModel.ConfirmBeforeStop):
                case nameof(SettingsViewModel.DefaultRamMb):
                case nameof(SettingsViewModel.CustomJvmArgs):
                case nameof(SettingsViewModel.CustomJavaPath):
                case nameof(SettingsViewModel.GameWidth):
                case nameof(SettingsViewModel.GameHeight):
                case nameof(SettingsViewModel.IsGameFullscreen):
                    break;
                default:
                    return;
            }

            var previousFlowClientMod = LauncherSettingsService.Instance.Current.EnableFlowClientMod;
            PersistSettings();

            if (e.PropertyName == nameof(SettingsViewModel.EnableFlowClientMod)
                && previousFlowClientMod
                && !Vm.EnableFlowClientMod)
            {
                _branding.RemoveBrandingModFromAllInstances(_instances);
            }
        }

        private void PersistSettings()
        {
            LauncherSettingsService.Instance.Update(s =>
            {
                s.PostLaunchAction = Vm.PostLaunchAction;
                s.EnableFlowClientMod = Vm.EnableFlowClientMod;
                s.EnableVoiceChatMod = Vm.EnableVoiceChatMod;
                s.EnableDiscordRpc = Vm.EnableDiscordRpc;
                s.UiAnimations = Vm.UiAnimations;
                s.AutoCheckUpdates = Vm.AutoCheckUpdates;
                s.ConfirmBeforeStop = Vm.ConfirmBeforeStop;
                s.DefaultRamMb = Vm.DefaultRamMb;
                s.CustomJvmArgs = Vm.CustomJvmArgs;
                s.CustomJavaPath = Vm.CustomJavaPath;
                s.GameWidth = Vm.GameWidth;
                s.GameHeight = Vm.GameHeight;
                s.IsGameFullscreen = Vm.IsGameFullscreen;
            });
        }

        private void CategoryTab_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string tag)
            {
                Vm.SelectedCategory = tag;
            }
        }

        private void LaunchAction_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string tag && int.TryParse(tag, out var val))
            {
                Vm.PostLaunchAction = (LaunchPostAction)val;
            }
        }

        private void RamPreset_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string tag && int.TryParse(tag, out var mb))
            {
                Vm.DefaultRamMb = mb;
            }
        }

        private void ResolutionPreset_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string tag)
            {
                var parts = tag.Split('x');
                if (parts.Length == 2 && int.TryParse(parts[0], out var w) && int.TryParse(parts[1], out var h))
                {
                    Vm.SetResolution(w, h);
                }
            }
        }

        private void BrowseJava_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                var dialog = new OpenFileDialog
                {
                    Title = "Select Java Executable (javaw.exe)",
                    Filter = "Java Executable (javaw.exe;java.exe)|javaw.exe;java.exe|All Executables (*.exe)|*.exe",
                    CheckFileExists = true
                };

                if (dialog.ShowDialog() == true)
                {
                    Vm.CustomJavaPath = dialog.FileName;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Could not open file picker:\n{ex.Message}", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void ResetJava_Click(object sender, RoutedEventArgs e)
        {
            Vm.CustomJavaPath = null;
        }

        private void JvmPreset_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string preset)
            {
                Vm.CustomJvmArgs = preset == "DEFAULT" ? null : preset;
            }
        }

        // ══════ DIRECTORIES ══════
        private void OpenWorldsDir_Click(object sender, RoutedEventArgs e) => OpenFolderSafe(Vm.WorldsDirectory);
        private void OpenScreenshotsDir_Click(object sender, RoutedEventArgs e) => OpenFolderSafe(Vm.ScreenshotsDirectory);
        private void OpenResourcePacksDir_Click(object sender, RoutedEventArgs e) => OpenFolderSafe(Vm.ResourcePacksDirectory);
        private void OpenShaderPacksDir_Click(object sender, RoutedEventArgs e) => OpenFolderSafe(Vm.ShaderPacksDirectory);
        private void OpenModsDir_Click(object sender, RoutedEventArgs e) => OpenFolderSafe(Vm.ModsDirectory);
        private void OpenCrashReportsDir_Click(object sender, RoutedEventArgs e) => OpenFolderSafe(Vm.CrashReportsDirectory);
        private void OpenMinecraftDir_Click(object sender, RoutedEventArgs e) => OpenFolderSafe(Vm.MinecraftDirectory);
        private void OpenLauncherDataDir_Click(object sender, RoutedEventArgs e) => OpenFolderSafe(Vm.LauncherDataDirectory);
        private void OpenInstallDir_Click(object sender, RoutedEventArgs e) => OpenFolderSafe(Vm.OfficialInstallDirectory);

        private static void OpenFolderSafe(string path)
        {
            try
            {
                if (!Directory.Exists(path))
                    Directory.CreateDirectory(path);

                Process.Start(new ProcessStartInfo
                {
                    FileName = path,
                    UseShellExecute = true
                });
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Could not open folder:\n{ex.Message}", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void CopyPath_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string path)
            {
                try
                {
                    Clipboard.SetText(path);
                    if (btn.ToolTip is ToolTip tt)
                    {
                        tt.Content = "Copied!";
                        tt.IsOpen = true;
                    }
                    else
                    {
                        btn.ToolTip = "Copied!";
                    }
                }
                catch { }
            }
        }

        // ══════ STORAGE & CLEANUP ══════
        private async void CleanTempFiles_Click(object sender, RoutedEventArgs e)
        {
            if (Vm.IsCleaningStorage) return;
            await Vm.CleanTempFilesAsync();
        }

        private async void CleanLogsAndCrashes_Click(object sender, RoutedEventArgs e)
        {
            if (Vm.IsCleaningStorage) return;
            await Vm.CleanOldLogsAsync();
        }

        private async void RefreshStorage_Click(object sender, RoutedEventArgs e)
        {
            await Vm.RefreshStorageAnalysisAsync();
        }

        private void CopyDiagnostic_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                var report = Vm.GenerateDiagnosticReport();
                Clipboard.SetText(report);
                MessageBox.Show("Diagnostic report copied to clipboard successfully!", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to copy report:\n{ex.Message}", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void ResetDefaults_Click(object sender, RoutedEventArgs e)
        {
            var res = MessageBox.Show(
                "Are you sure you want to reset all Flow Client settings to their default values?",
                "Reset Settings",
                MessageBoxButton.YesNo,
                MessageBoxImage.Question);

            if (res == MessageBoxResult.Yes)
            {
                LauncherSettingsService.Instance.Save(new LauncherSettings());
                OnLoaded(this, new RoutedEventArgs());
                MessageBox.Show("All settings have been restored to default values.", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Information);
            }
        }

        private UpdateCheckResult? _latestUpdateResult;

        private async void CheckUpdatesNow_Click(object sender, RoutedEventArgs e)
        {
            if (Vm.IsCheckingUpdate || Vm.IsUpdating) return;

            Vm.IsCheckingUpdate = true;
            Vm.UpdateCheckMessage = "Checking for updates…";

            try
            {
                var updates = new UpdateService();
                var res = await updates.CheckAsync();
                if (res.UpdateAvailable)
                {
                    _latestUpdateResult = res;
                    Vm.LatestVersion = res.LatestVersion ?? "New version";
                    Vm.IsUpdateAvailable = true;
                    Vm.UpdateCheckMessage = $"Update available: v{res.LatestVersion}!";
                }
                else
                {
                    _latestUpdateResult = null;
                    Vm.IsUpdateAvailable = false;
                    Vm.UpdateCheckMessage = "Flow Client is already up to date!";
                }
            }
            catch (Exception ex)
            {
                Vm.UpdateCheckMessage = $"Update check failed: {ex.Message}";
            }
            finally
            {
                Vm.IsCheckingUpdate = false;
            }
        }

        private async void InstallUpdateNow_Click(object sender, RoutedEventArgs e)
        {
            if (Vm.IsUpdating || _latestUpdateResult == null || string.IsNullOrEmpty(_latestUpdateResult.DownloadUrl))
                return;

            var confirm = MessageBox.Show(
                $"Do you want to download and install Flow Client v{_latestUpdateResult.LatestVersion} now?",
                "Flow Client Update",
                MessageBoxButton.YesNo,
                MessageBoxImage.Question);

            if (confirm != MessageBoxResult.Yes) return;

            Vm.IsUpdating = true;
            Vm.UpdateCheckMessage = "Starting download…";

            try
            {
                var updates = new UpdateService();
                var progress = new Progress<(double percent, double downloadedMb, double totalMb)>(p =>
                {
                    Dispatcher.Invoke(() =>
                    {
                        Vm.UpdateCheckMessage = p.totalMb > 0
                            ? $"Downloading: {(int)p.percent}% ({p.downloadedMb:0.1}/{p.totalMb:0.1} MB)…"
                            : $"Downloading: {p.downloadedMb:0.1} MB…";
                    });
                });

                var packagePath = await updates.DownloadAsync(_latestUpdateResult.DownloadUrl, progress);
                Vm.UpdateCheckMessage = "Installing update & restarting…";

                var scriptPath = updates.PrepareInstall(packagePath);

                Process.Start(new ProcessStartInfo
                {
                    FileName = scriptPath,
                    UseShellExecute = true,
                    CreateNoWindow = true
                });

                Application.Current.Dispatcher.Invoke(() => Application.Current.Shutdown());
                Environment.Exit(0);
            }
            catch (Exception ex)
            {
                Vm.IsUpdating = false;
                Vm.UpdateCheckMessage = $"Update failed: {ex.Message}";
                MessageBox.Show($"Could not complete update:\n{ex.Message}", "Update Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private void BackToLaunch_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow mw)
            {
                mw.Home_Click(sender, e);
            }
        }
    }
}
