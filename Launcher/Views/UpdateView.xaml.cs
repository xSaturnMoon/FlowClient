using System.Diagnostics;
using System.Globalization;
using System.Windows;
using System.Windows.Controls;
using Launcher.Services;
using Launcher.ViewModels;

namespace Launcher.Views
{
    public partial class UpdateView : UserControl
    {
        private static readonly CultureInfo En = new("en-US");

        private readonly UpdateViewModel _viewModel = new();
        private readonly LauncherInfoService _launcherInfo = new();
        private readonly UpdateService _updates = new();
        private int _checkGeneration;

        public UpdateView()
        {
            InitializeComponent();
            DataContext = _viewModel;
            Loaded += OnLoaded;
        }

        private async void OnLoaded(object sender, RoutedEventArgs e)
        {
            var info = _launcherInfo.Gather();
            _viewModel.LauncherVersion = _updates.GetCurrentVersion();
            _viewModel.LauncherChannel = info.Channel;
            _viewModel.LatestVersion = _viewModel.LauncherVersion;

            await RunCheckAsync(silent: true);
        }

        private async void Check_Click(object sender, RoutedEventArgs e)
        {
            await RunCheckAsync(silent: false);
        }

        private async Task RunCheckAsync(bool silent)
        {
            var generation = Interlocked.Increment(ref _checkGeneration);
            var vm = _viewModel;

            vm.IsChecking = true;
            if (!silent)
            {
                vm.ResetDownloadState();
                vm.StatusMessage = "Checking for updates…";
            }

            try
            {
                var result = await Task.Run(() => _updates.CheckAsync());
                if (generation != _checkGeneration)
                    return;

                ApplyResult(vm, result);
                vm.LastCheckedText = DateTime.Now.ToString("MMM d, HH:mm", En);

                if (!silent && result.UpdateAvailable && !string.IsNullOrEmpty(result.DownloadUrl))
                    await DownloadAndInstallAsync(generation);
            }
            catch (Exception ex)
            {
                if (generation == _checkGeneration)
                    vm.StatusMessage = $"Couldn't check for updates. {ex.Message}";
            }
            finally
            {
                if (generation == _checkGeneration)
                    vm.IsChecking = false;
            }
        }

        private static void ApplyResult(UpdateViewModel vm, UpdateCheckResult result)
        {
            vm.LatestVersion = result.LatestVersion ?? vm.LauncherVersion;
            vm.ReleaseNotes = result.ReleaseNotes;
            vm.HasUpdate = result.UpdateAvailable;
            vm.DownloadUrl = result.DownloadUrl;

            if (!string.IsNullOrEmpty(result.Channel))
                vm.LauncherChannel = result.Channel;

            if (!result.Success)
            {
                vm.StatusMessage = string.IsNullOrWhiteSpace(result.Message)
                    ? "Couldn't check for updates."
                    : result.Message;
                return;
            }

            if (result.UpdateAvailable && string.IsNullOrEmpty(result.DownloadUrl))
            {
                vm.StatusMessage = string.IsNullOrWhiteSpace(result.Message)
                    ? "Update available, but no download URL is configured."
                    : $"{result.Message} No download URL configured.";
                return;
            }

            vm.StatusMessage = string.IsNullOrWhiteSpace(result.Message)
                ? (result.UpdateAvailable ? $"Update available: {result.LatestVersion}" : "You're on the latest version.")
                : result.Message;
        }

        private async void Download_Click(object sender, RoutedEventArgs e)
        {
            await DownloadAndInstallAsync(_checkGeneration);
        }

        private async Task DownloadAndInstallAsync(int generation)
        {
            var vm = _viewModel;
            if (string.IsNullOrEmpty(vm.DownloadUrl))
                return;

            vm.IsDownloading = true;
            vm.StatusMessage = "Downloading update…";

            try
            {
                var progress = new Progress<double>(value => vm.DownloadProgress = value);
                var packagePath = await _updates.DownloadAsync(vm.DownloadUrl, progress);
                if (generation != _checkGeneration)
                    return;

                vm.SetDownloadedPackage(packagePath);
                vm.StatusMessage = "Installing update and restarting…";
                InstallDownloadedPackage(vm);
            }
            catch (Exception ex)
            {
                vm.StatusMessage = $"Update failed: {ex.Message}";
            }
            finally
            {
                vm.IsDownloading = false;
            }
        }

        private void Install_Click(object sender, RoutedEventArgs e)
        {
            InstallDownloadedPackage(_viewModel);
        }

        private void InstallDownloadedPackage(UpdateViewModel vm)
        {
            if (string.IsNullOrEmpty(vm.DownloadedPackagePath))
                return;

            try
            {
                var scriptPath = _updates.PrepareInstall(vm.DownloadedPackagePath);
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
                vm.StatusMessage = $"Install failed: {ex.Message}";
            }
        }

        private void BackToLaunch_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Launch");
        }
    }
}
