using System;
using System.ComponentModel;
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

            Vm.CloseLauncherOnLaunch = settings.CloseLauncherOnLaunch;
            Vm.MinimizeLauncherOnLaunch = settings.MinimizeLauncherOnLaunch;
            Vm.EnableFlowClientMod = settings.EnableFlowClientMod;
            Vm.EnableVoiceChatMod = settings.EnableVoiceChatMod;
            Vm.UiAnimations = settings.UiAnimations;
            Vm.AutoCheckUpdates = settings.AutoCheckUpdates;
            Vm.ConfirmBeforeStop = settings.ConfirmBeforeStop;

            SettingsPathText.Text = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "FlowLauncher", "settings.json");

            _loading = false;
        }

        private void OnSettingPropertyChanged(object? sender, PropertyChangedEventArgs e)
        {
            if (_loading || e.PropertyName is null)
                return;

            switch (e.PropertyName)
            {
                case nameof(SettingsViewModel.CloseLauncherOnLaunch):
                case nameof(SettingsViewModel.MinimizeLauncherOnLaunch):
                case nameof(SettingsViewModel.EnableFlowClientMod):
                case nameof(SettingsViewModel.EnableVoiceChatMod):
                case nameof(SettingsViewModel.UiAnimations):
                case nameof(SettingsViewModel.AutoCheckUpdates):
                case nameof(SettingsViewModel.ConfirmBeforeStop):
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
                s.CloseLauncherOnLaunch = Vm.CloseLauncherOnLaunch;
                s.MinimizeLauncherOnLaunch = Vm.MinimizeLauncherOnLaunch;
                s.EnableFlowClientMod = Vm.EnableFlowClientMod;
                s.EnableVoiceChatMod = Vm.EnableVoiceChatMod;
                s.UiAnimations = Vm.UiAnimations;
                s.AutoCheckUpdates = Vm.AutoCheckUpdates;
                s.ConfirmBeforeStop = Vm.ConfirmBeforeStop;
            });
        }

        private void BackToLaunch_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Launch");
        }
    }
}
