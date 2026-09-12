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

            _loading = false;
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
                MessageBox.Show($"Impossibile aprire la cartella:\n{ex.Message}", "Flow Client", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void CleanTempFiles_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                UpdateService.CleanOldUpdates();
                Vm.MaintenanceMessage = "Cartelle temporanee e vecchi aggiornamenti rimossi con successo!";
            }
            catch (Exception ex)
            {
                Vm.MaintenanceMessage = $"Errore durante la pulizia: {ex.Message}";
            }
        }

        private async void CheckUpdatesNow_Click(object sender, RoutedEventArgs e)
        {
            if (Vm.IsCheckingUpdate) return;

            Vm.IsCheckingUpdate = true;
            Vm.UpdateCheckMessage = "Controllo aggiornamenti in corso…";

            try
            {
                var updates = new UpdateService();
                var res = await updates.CheckAsync();
                if (res.UpdateAvailable)
                {
                    Vm.UpdateCheckMessage = $"Nuova versione disponibile: v{res.LatestVersion}! Clicca l'icona aggiornamento nella barra laterale.";
                }
                else
                {
                    Vm.UpdateCheckMessage = "Stai già utilizzando l'ultima versione disponibile!";
                }
            }
            catch (Exception ex)
            {
                Vm.UpdateCheckMessage = $"Controllo non riuscito: {ex.Message}";
            }
            finally
            {
                Vm.IsCheckingUpdate = false;
            }
        }

        private void BackToLaunch_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Launch");
        }
    }
}
