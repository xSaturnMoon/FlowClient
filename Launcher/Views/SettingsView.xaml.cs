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

        private void BrowseJava_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                var dialog = new OpenFileDialog
                {
                    Title = "Seleziona eseguibile Java (javaw.exe)",
                    Filter = "Java Executable (javaw.exe;java.exe)|javaw.exe;java.exe|Tutti gli eseguibili (*.exe)|*.exe",
                    CheckFileExists = true
                };

                if (dialog.ShowDialog() == true)
                {
                    Vm.CustomJavaPath = dialog.FileName;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Impossibile aprire il selettore di file:\n{ex.Message}", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Warning);
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
                MessageBox.Show($"Impossibile aprire la cartella:\n{ex.Message}", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Warning);
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
                        tt.Content = "Copiato!";
                        tt.IsOpen = true;
                    }
                    else
                    {
                        btn.ToolTip = "Copiato!";
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
                MessageBox.Show("Report diagnostico copiato negli appunti con successo!", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Errore nella copia del report:\n{ex.Message}", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void ResetDefaults_Click(object sender, RoutedEventArgs e)
        {
            var res = MessageBox.Show(
                "Sei sicuro di voler ripristinare tutte le impostazioni predefinite di Flow Client?",
                "Ripristina Impostazioni",
                MessageBoxButton.YesNo,
                MessageBoxImage.Question);

            if (res == MessageBoxResult.Yes)
            {
                LauncherSettingsService.Instance.Save(new LauncherSettings());
                OnLoaded(this, new RoutedEventArgs());
                MessageBox.Show("Tutte le impostazioni sono state ripristinate ai valori predefiniti.", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Information);
            }
        }

        private UpdateCheckResult? _latestUpdateResult;

        private async void CheckUpdatesNow_Click(object sender, RoutedEventArgs e)
        {
            if (Vm.IsCheckingUpdate || Vm.IsUpdating) return;

            Vm.IsCheckingUpdate = true;
            Vm.UpdateCheckMessage = "Controllo aggiornamenti in corso…";

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
                else
                {
                    _latestUpdateResult = null;
                    Vm.IsUpdateAvailable = false;
                    Vm.UpdateCheckMessage = "Flow Client è già aggiornato all'ultima versione!";
                }
            }
            catch (Exception ex)
            {
                Vm.UpdateCheckMessage = $"Controllo fallito: {ex.Message}";
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
                $"Vuoi scaricare e installare Flow Client v{_latestUpdateResult.LatestVersion} adesso?",
                "Aggiornamento Flow Client",
                MessageBoxButton.YesNo,
                MessageBoxImage.Question);

            if (confirm != MessageBoxResult.Yes) return;

            Vm.IsUpdating = true;
            Vm.UpdateCheckMessage = "Download aggiornamento in corso…";

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
                Vm.IsUpdating = false;
                Vm.UpdateCheckMessage = $"Aggiornamento fallito: {ex.Message}";
                MessageBox.Show($"Impossibile completare l'aggiornamento:\n{ex.Message}", "Errore Aggiornamento", MessageBoxButton.OK, MessageBoxImage.Error);
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
