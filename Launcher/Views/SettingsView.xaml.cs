using System;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Windows;
using System.Windows.Controls;
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

            Vm.LoadSystemInfo();
            _ = AutoCheckUpdateSilentlyAsync();

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
                    Vm.LatestVersion = res.LatestVersion ?? "New version";
                    Vm.IsUpdateAvailable = true;
                    Vm.UpdateCheckMessage = $"New version available: v{res.LatestVersion}!";
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

        private void OpenInstancesDir_Click(object sender, RoutedEventArgs e)
        {
            OpenFolderSafe(Vm.InstancesDirectory);
        }

        private void OpenDataDir_Click(object sender, RoutedEventArgs e)
        {
            OpenFolderSafe(Vm.LauncherDataDirectory);
        }

        private void OpenInstallDir_Click(object sender, RoutedEventArgs e)
        {
            OpenFolderSafe(Vm.OfficialInstallDirectory);
        }

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
                MessageBox.Show($"Unable to open folder:\n{ex.Message}", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void CleanTempFiles_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                UpdateService.CleanOldUpdates();
                Vm.MaintenanceMessage = "Temporary folders and old updates removed successfully!";
            }
            catch (Exception ex)
            {
                Vm.MaintenanceMessage = $"Cleanup failed: {ex.Message}";
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
                    Vm.UpdateCheckMessage = $"New version available: v{res.LatestVersion}!";
                }
                else
                {
                    _latestUpdateResult = null;
                    Vm.IsUpdateAvailable = false;
                    Vm.UpdateCheckMessage = "Flow Client is up to date!";
                }
            }
            catch (Exception ex)
            {
                Vm.UpdateCheckMessage = $"Check failed: {ex.Message}";
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
            Vm.UpdateCheckMessage = "Downloading update…";

            try
            {
                var updates = new UpdateService();
                var packagePath = await updates.DownloadAsync(_latestUpdateResult.DownloadUrl);
                var scriptPath = updates.PrepareInstall(packagePath);

                Process.Start(new ProcessStartInfo
                {
                    FileName = scriptPath,
                    UseShellExecute = true,
                    CreateNoWindow = true
                });

                Application.Current.Shutdown();
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Download or installation failed:\n{ex.Message}", "Update Error",
                    MessageBoxButton.OK, MessageBoxImage.Error);
                Vm.UpdateCheckMessage = "Update failed.";
                Vm.IsUpdating = false;
            }
        }

        private void BackToLaunch_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Launch");
        }
    }
}
