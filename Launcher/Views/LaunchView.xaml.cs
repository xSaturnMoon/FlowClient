using System;
using System.Globalization;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using Launcher.Helpers;
using Launcher.Models;
using Launcher.Services;
using Launcher.ViewModels;

namespace Launcher.Views
{
    public partial class LaunchView : UserControl
    {
        private static readonly CultureInfo EnUs = new("en-US");

        private readonly InstanceService _instances = new();
        private readonly MinecraftAuthService _auth = new();
        private readonly MinecraftLaunchService _launch = new();
        private readonly ServerFavoritesService _favorites = ServerFavoritesService.Instance;
        private readonly MinecraftProcessTracker _tracker = MinecraftProcessTracker.Instance;

        private bool _serversAnimating;

        public LaunchView()
        {
            DataContext = new LaunchViewModel();
            InitializeComponent();
            Loaded += OnLoaded;
            Unloaded += OnUnloaded;
        }

        private LaunchViewModel Vm => (LaunchViewModel)DataContext;

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            ServersDrawer.Opacity = 0;
            ServersDrawer.Visibility = Visibility.Collapsed;
            ServersDrawer.IsHitTestVisible = false;
            ServersChevronRotate.Angle = 180;
            Vm.IsServersExpanded = false;

            // Hook tracker events so we update UI even when navigated away and back
            _tracker.ProcessStarted += OnProcessStarted;
            _tracker.ProcessExited += OnProcessExited;
            _tracker.LaunchStateChanged += OnLaunchStateChanged;

            Refresh();
            _ = SetServersExpandedAsync(true);

            // Sync UI with the current tracker state (game might already be running)
            SyncRunningState();
        }

        private void OnUnloaded(object sender, RoutedEventArgs e)
        {
            _tracker.ProcessStarted -= OnProcessStarted;
            _tracker.ProcessExited -= OnProcessExited;
            _tracker.LaunchStateChanged -= OnLaunchStateChanged;
        }

        private void OnLaunchStateChanged()
        {
            Dispatcher.BeginInvoke(() =>
            {
                if (_tracker.IsRunning) return;

                if (_tracker.IsLaunching)
                {
                    Vm.CanPlay = false;
                    Vm.IsRunning = false;
                    Vm.StatusMessage = _tracker.LaunchStatusMessage;
                }
                else
                {
                    Vm.CanPlay = !string.IsNullOrEmpty(Vm.SelectedInstanceId);
                    Vm.StatusMessage = _tracker.LaunchStatusMessage;
                }
            });
        }

        private void OnProcessStarted()
        {
            Dispatcher.BeginInvoke(() =>
            {
                Vm.IsRunning = true;
                Vm.CanPlay = true;
                Vm.StatusMessage = "Minecraft is running.";
            });
        }

        private void OnProcessExited()
        {
            Dispatcher.BeginInvoke(() =>
            {
                Vm.IsRunning = false;
                Vm.StatusMessage = null;
                Vm.CanPlay = !string.IsNullOrEmpty(Vm.SelectedInstanceId) && !_tracker.IsLaunching;
                Refresh();
            });
        }

        private void SyncRunningState()
        {
            if (_tracker.IsRunning)
            {
                Vm.IsRunning = true;
                Vm.CanPlay = true;
                Vm.StatusMessage = "Minecraft is running.";
            }
            else if (_tracker.IsLaunching)
            {
                Vm.IsRunning = false;
                Vm.CanPlay = false;
                Vm.StatusMessage = _tracker.LaunchStatusMessage;
            }
            else
            {
                Vm.IsRunning = false;
                Vm.CanPlay = !string.IsNullOrEmpty(Vm.SelectedInstanceId);
                Vm.StatusMessage = null;
            }
        }

        private void Refresh()
        {
            var vm = Vm;
            var account = _auth.LoadSavedAccount();
            vm.Greeting = account != null && !string.IsNullOrEmpty(account.MinecraftUsername)
                ? $"Hey, {account.MinecraftUsername}"
                : "Ready to play";

            var all = _instances.GetAll()
                .OrderByDescending(i => i.IsFavorite)
                .ThenByDescending(i => i.LastPlayedAt ?? DateTime.MinValue)
                .ToList();

            vm.Instances.Clear();
            foreach (var inst in all)
            {
                vm.Instances.Add(new InstanceListItemViewModel
                {
                    Id = inst.Id,
                    Name = inst.Name,
                    Version = inst.MinecraftVersion,
                    Loader = inst.Loader,
                    LastPlayedText = FormatLastPlayed(inst.LastPlayedAt),
                    IsFavorite = inst.IsFavorite,
                    LoaderIconUri = LoaderBranding.GetIconUri(inst.Loader),
                    IsSelected = inst.Id == vm.SelectedInstanceId
                });
            }

            RefreshServers();
            vm.NotifyListState();

            if (all.Count == 0)
            {
                vm.SelectedInstanceId = null;
                vm.CanPlay = false;
                return;
            }

            var selectedId = vm.SelectedInstanceId;
            if (string.IsNullOrEmpty(selectedId) || all.All(i => i.Id != selectedId))
                selectedId = all[0].Id;

            SelectInstance(selectedId, refreshList: true);
        }

        private void RefreshServers()
        {
            var vm = Vm;
            vm.FavoriteServers.Clear();

            foreach (var server in _favorites.GetFavorites(5))
            {
                vm.FavoriteServers.Add(new ServerListItemViewModel
                {
                    Host = server.Host,
                    Port = server.Port,
                    Name = server.DisplayName,
                    Address = server.AddressText,
                    Icon = ServerIconLoader.Load(server.IconPath)
                });
            }

            vm.NotifyServersChanged();
            _ = FetchMissingIconsAsync();
        }

        private async System.Threading.Tasks.Task FetchMissingIconsAsync()
        {
            var pending = Vm.FavoriteServers.Where(s => s.Icon == null).ToList();
            if (pending.Count == 0) return;

            await System.Threading.Tasks.Task.Run(() =>
            {
                foreach (var item in pending)
                {
                    var path = ServerIconFetcher.TryFetch(item.Host, item.Port);
                    if (path == null) continue;
                    var icon = ServerIconLoader.Load(path);
                    if (icon == null) continue;
                    Dispatcher.Invoke(() => item.Icon = icon);
                }
            });
        }

        private void SelectInstance(string id, bool refreshList = false)
        {
            var inst = _instances.GetById(id);
            var vm = Vm;
            if (inst == null) return;

            vm.SelectedInstanceId = id;
            vm.SelectedName = inst.Name;
            vm.SelectedVersion = inst.MinecraftVersion;
            vm.SelectedLoader = inst.Loader;
            vm.SelectedLoaderIconUri = LoaderBranding.GetIconUri(inst.Loader);
            vm.SelectedRamText = $"{inst.RamMb} MB RAM";
            vm.SelectedLastPlayed = FormatLastPlayed(inst.LastPlayedAt);

            // Only allow Play if we're not running and not currently in the middle of launching
            vm.CanPlay = !_tracker.IsRunning && !_tracker.IsLaunching;

            if (refreshList)
            {
                foreach (var item in vm.Instances)
                    item.IsSelected = item.Id == id;
                var items = vm.Instances.ToList();
                vm.Instances.Clear();
                foreach (var item in items)
                    vm.Instances.Add(item);
            }
        }

        private void Instance_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is not FrameworkElement fe || fe.Tag is not string id) return;
            SelectInstance(id, refreshList: true);
        }

        private async void Play_Click(object sender, RoutedEventArgs e)
        {
            await LaunchSelectedAsync(null);
        }

        private async void ServerPlay_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not FrameworkElement { Tag: ServerListItemViewModel server })
                return;

            e.Handled = true;
            await LaunchSelectedAsync(new LaunchServerTarget
            {
                Host = server.Host,
                Port = server.Port,
                Name = server.Name
            });
        }

        private async System.Threading.Tasks.Task LaunchSelectedAsync(LaunchServerTarget? server)
        {
            // If game is running, the button acts as Stop
            if (_tracker.IsRunning)
            {
                if (LauncherSettingsService.Instance.Current.ConfirmBeforeStop)
                {
                    var confirm = MessageBox.Show(
                        "Stop Minecraft and close the game?",
                        "Stop game",
                        MessageBoxButton.YesNo,
                        MessageBoxImage.Question);
                    if (confirm != MessageBoxResult.Yes)
                        return;
                }

                _tracker.KillProcess();
                return;
            }

            // Prevent double-click during the launch setup phase
            if (_tracker.IsLaunching) return;

            var id = Vm.SelectedInstanceId;
            if (string.IsNullOrEmpty(id)) return;

            var initialMsg = server != null
                ? $"Connecting to {server.Host}…"
                : "Preparing launch…";
                
            _tracker.SetLaunching(true, initialMsg);
            LauncherLogService.Instance.Info(initialMsg);

            try
            {
                var progress = new Progress<string>(msg =>
                {
                    LauncherLogService.Instance.Info(msg);
                    if (_tracker.IsLaunching)
                        _tracker.SetLaunching(true, msg);
                });
                var result = await System.Threading.Tasks.Task.Run(async () =>
                    await _launch.LaunchAsync(id, progress, server: server));

                if (!result.Success)
                {
                    _tracker.SetLaunching(false, result.Message);
                    LauncherLogService.Instance.Error(result.Message);
                    MessageBox.Show(result.Message, "Launch failed",
                        MessageBoxButton.OK, MessageBoxImage.Warning);
                }
                else if (result.Process != null)
                {
                    _tracker.SetLaunching(false, null);
                    LauncherLogService.Instance.Info("Minecraft process started.");
                }
                else
                {
                    // No process returned (shouldn't happen), just reset
                    _tracker.SetLaunching(false, null);
                }
            }
            catch (Exception ex)
            {
                _tracker.SetLaunching(false, ex.Message);
                LauncherLogService.Instance.Error(ex.Message);
                MessageBox.Show(ex.Message, "Launch failed",
                    MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private void ToggleFavorite_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not FrameworkElement { Tag: ServerListItemViewModel server })
                return;

            e.Handled = true;
            _favorites.ToggleFavorite(server.Host, server.Port);
            RefreshServers();
        }

        private async void ToggleServers_Click(object sender, MouseButtonEventArgs e)
        {
            if (_serversAnimating) return;
            await SetServersExpandedAsync(!Vm.IsServersExpanded);
        }

        private async System.Threading.Tasks.Task SetServersExpandedAsync(bool expanded)
        {
            _serversAnimating = true;
            Vm.IsServersExpanded = expanded;

            const int duration = 220;
            _ = UiTransitions.AnimateRotationAsync(ServersChevronRotate, expanded ? 0 : 180, duration);

            if (expanded)
            {
                ServersDrawer.Visibility = Visibility.Visible;
                ServersDrawer.IsHitTestVisible = true;
                UiTransitions.AnimateOpacity(ServersDrawer, 1, duration);
            }
            else
            {
                UiTransitions.AnimateOpacity(ServersDrawer, 0, duration);
                await System.Threading.Tasks.Task.Delay(duration + 30);
                ServersDrawer.IsHitTestVisible = false;
                ServersDrawer.Visibility = Visibility.Collapsed;
            }

            await System.Threading.Tasks.Task.Delay(duration);
            _serversAnimating = false;
        }

        private void GoToExplore_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Explore");
        }

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
    }
}
