using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
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

        private static readonly string MinecraftRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".minecraft");

        private readonly InstanceService _instances = new();
        private readonly MinecraftAuthService _auth = new();
        private readonly MinecraftLaunchService _launch = new();
        private readonly ServerFavoritesService _favorites = ServerFavoritesService.Instance;
        private readonly MinecraftProcessTracker _tracker = MinecraftProcessTracker.Instance;

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
            // Hook tracker events so we update UI even when navigated away and back
            _tracker.ProcessStarted += OnProcessStarted;
            _tracker.ProcessExited += OnProcessExited;
            _tracker.LaunchStateChanged += OnLaunchStateChanged;
            InstanceSelectionService.SelectedInstanceChanged += OnInstanceSelectionChanged;
            InstanceService.InstancesChanged += OnInstancesChanged;

            // Hook Discord RPC events
            DiscordRpcService.Instance.ConnectionChanged += OnDiscordRpcConnectionChanged;
            Vm.IsDiscordRpcConnected = DiscordRpcService.Instance.IsConnected;
            Vm.DiscordRpcStatus = DiscordRpcService.Instance.IsConnected ? "Connesso a Discord" : "In attesa di Discord";

            Refresh();

            // Sync UI with the current tracker state (game might already be running)
            SyncRunningState();
        }

        private void OnUnloaded(object sender, RoutedEventArgs e)
        {
            _tracker.ProcessStarted -= OnProcessStarted;
            _tracker.ProcessExited -= OnProcessExited;
            _tracker.LaunchStateChanged -= OnLaunchStateChanged;
            InstanceSelectionService.SelectedInstanceChanged -= OnInstanceSelectionChanged;
            InstanceService.InstancesChanged -= OnInstancesChanged;
            DiscordRpcService.Instance.ConnectionChanged -= OnDiscordRpcConnectionChanged;
        }

        private void OnDiscordRpcConnectionChanged(bool connected)
        {
            Dispatcher.BeginInvoke(() =>
            {
                Vm.IsDiscordRpcConnected = connected;
                Vm.DiscordRpcStatus = connected ? "Connesso a Discord" : "In attesa di Discord";
            });
        }

        private void OnInstancesChanged()
        {
            Dispatcher.BeginInvoke(() => Refresh());
        }

        private void OnInstanceSelectionChanged(string id)
        {
            Dispatcher.BeginInvoke(() =>
            {
                SelectInstance(id, refreshList: true);
            });
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
                Vm.StatusMessage = "Minecraft in esecuzione.";
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
                Vm.StatusMessage = "Minecraft in esecuzione.";
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
            _ = LoadPlayerAvatarAsync(account);

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

        private async Task LoadPlayerAvatarAsync(SavedAccount? account)
        {
            if (account == null)
            {
                Dispatcher.Invoke(() =>
                {
                    Vm.PlayerAvatar = null;
                    Vm.PlayerName = "Guest";
                    Vm.Greeting = "Welcome to Flow Client";
                    Vm.IsAuthenticated = false;
                });
                return;
            }

            var username = string.IsNullOrWhiteSpace(account.MinecraftUsername) ? "Player" : account.MinecraftUsername;
            Dispatcher.Invoke(() =>
            {
                Vm.PlayerName = username;
                Vm.Greeting = $"Welcome back, {username}";
                Vm.IsAuthenticated = true;
            });

            try
            {
                var head = await SkinAvatarService.LoadHeadAsync(account.SkinTextureUrl, account.MinecraftUsername, account.MinecraftUuid);
                if (head != null)
                {
                    Dispatcher.Invoke(() => Vm.PlayerAvatar = head);
                }
            }
            catch { }
        }

        private void RefreshServers()
        {
            var vm = Vm;
            vm.FavoriteServers.Clear();

            foreach (var server in _favorites.GetFavorites(4))
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

        private async Task FetchMissingIconsAsync()
        {
            var pending = Vm.FavoriteServers.Where(s => s.Icon == null).ToList();
            if (pending.Count == 0) return;

            await Task.Run(() =>
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

        private async Task LaunchSelectedAsync(LaunchServerTarget? server)
        {
            // If game is running, the button acts as Stop
            if (_tracker.IsRunning)
            {
                if (LauncherSettingsService.Instance.Current.ConfirmBeforeStop)
                {
                    var confirm = MessageBox.Show(
                        "Are you sure you want to stop Minecraft and close the session?",
                        "Stop Game",
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
                var result = await Task.Run(async () =>
                    await _launch.LaunchAsync(id, progress, server: server));

                if (!result.Success)
                {
                    _tracker.SetLaunching(false, result.Message);
                    LauncherLogService.Instance.Error(result.Message);
                    MessageBox.Show(result.Message, "Launch Failed",
                        MessageBoxButton.OK, MessageBoxImage.Warning);
                }
                else if (result.Process != null)
                {
                    _tracker.SetLaunching(false, null);
                    LauncherLogService.Instance.Info("Minecraft process started successfully.");
                }
                else
                {
                    _tracker.SetLaunching(false, null);
                }
            }
            catch (Exception ex)
            {
                _tracker.SetLaunching(false, ex.Message);
                LauncherLogService.Instance.Error(ex.Message);
                MessageBox.Show(ex.Message, "Launch Failed",
                    MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private void PlayerCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Account");
        }

        private void OpenModsFolder_Click(object sender, RoutedEventArgs e)
        {
            var id = Vm.SelectedInstanceId;
            if (string.IsNullOrEmpty(id)) return;
            var inst = _instances.GetById(id);
            if (inst == null) return;
            var gameDir = ResolveGameDirectory(inst);
            OpenFolderSafe(Path.Combine(gameDir, "mods"));
        }

        private void OpenScreenshotsFolder_Click(object sender, RoutedEventArgs e)
        {
            var id = Vm.SelectedInstanceId;
            if (string.IsNullOrEmpty(id)) return;
            var inst = _instances.GetById(id);
            if (inst == null) return;
            var gameDir = ResolveGameDirectory(inst);
            OpenFolderSafe(Path.Combine(gameDir, "screenshots"));
        }

        private void OpenGameFolder_Click(object sender, RoutedEventArgs e)
        {
            var id = Vm.SelectedInstanceId;
            if (string.IsNullOrEmpty(id)) return;
            var inst = _instances.GetById(id);
            if (inst == null) return;
            var gameDir = ResolveGameDirectory(inst);
            OpenFolderSafe(gameDir);
        }

        private static string ResolveGameDirectory(MinecraftInstance instance)
        {
            var loader = instance.Loader?.Trim() ?? "Vanilla";
            var isVanilla = loader.Equals("Vanilla", StringComparison.OrdinalIgnoreCase);
            var hasFlowMods = instance.Mods.Count > 0;

            if (isVanilla && !hasFlowMods)
                return MinecraftRoot;

            return Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "FlowLauncher", "FlowVersions", instance.Id);
        }

        private static void OpenFolderSafe(string path)
        {
            try
            {
                Directory.CreateDirectory(path);
                Process.Start(new ProcessStartInfo
                {
                    FileName = path,
                    UseShellExecute = true
                });
            }
            catch (Exception ex)
            {
                LauncherLogService.Instance.Error($"Impossibile aprire la cartella {path}: {ex.Message}");
            }
        }

        private void GoToExplore_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Explore");
        }

        private void GoToSettings_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Settings");
        }

        private void GoToCosmetics_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Cosmetics");
        }

        private async void QuickServerPlay_Click(object sender, RoutedEventArgs e)
        {
            var firstServer = Vm.FavoriteServers.FirstOrDefault();
            if (firstServer != null)
            {
                await LaunchSelectedAsync(new LaunchServerTarget
                {
                    Host = firstServer.Host,
                    Port = firstServer.Port,
                    Name = firstServer.Name
                });
            }
            else
            {
                await LaunchSelectedAsync(new LaunchServerTarget
                {
                    Host = "mc.coralmc.it",
                    Port = 25565,
                    Name = "CoralMC"
                });
            }
        }

        private static string FormatLastPlayed(DateTime? dt)
        {
            if (dt == null) return "Mai giocato";
            var local = dt.Value.ToLocalTime();
            var diff = DateTime.Now - local;
            if (diff.TotalMinutes < 60) return $"{(int)diff.TotalMinutes}m fa";
            if (diff.TotalHours < 24) return $"{(int)diff.TotalHours}h fa";
            if (diff.TotalDays < 7) return $"{(int)diff.TotalDays}g fa";
            return local.ToString("MMM dd", EnUs);
        }
    }
}
