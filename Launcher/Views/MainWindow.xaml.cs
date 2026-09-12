using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using Launcher.Helpers;
using Launcher.Services;
using Launcher.ViewModels;

namespace Launcher.Views
{
    public partial class MainWindow : Window
    {
        private static readonly string[] TopNavTags = ["Launch", "Explore", "Account", "Settings"];
        private const double NavItemStep = 57;

        private readonly MainViewModel _viewModel;
        private readonly AccountViewModel _accountViewModel;
        private readonly InstanceService _instances = new();

        #region Windows API for Snap Prevention

        [DllImport("user32.dll")]
        private static extern IntPtr SetWindowLongPtr(IntPtr hWnd, int nIndex, IntPtr dwNewLong);

        [DllImport("user32.dll")]
        private static extern IntPtr GetWindowLongPtr(IntPtr hWnd, int nIndex);

        [DllImport("user32.dll")]
        private static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);

        private const int GWL_STYLE = -16;
        private const int WS_MAXIMIZEBOX = 0x00010000;
        private const int SWP_FRAMECHANGED = 0x0020;
        private const int SWP_NOMOVE = 0x0002;
        private const int SWP_NOSIZE = 0x0001;
        private const int SWP_NOZORDER = 0x0004;
        private const int SWP_NOOWNERZORDER = 0x0200;

        #endregion

        public MainWindow()
        {
            InitializeComponent();
            _viewModel = new MainViewModel();
            _accountViewModel = new AccountViewModel();
            DataContext = _viewModel;
            _viewModel.PropertyChanged += ViewModel_PropertyChanged;
            MinecraftProcessTracker.Instance.ProcessStarted += OnGameProcessStarted;
            MinecraftProcessTracker.Instance.ProcessExited += OnGameProcessExited;
            InstanceService.InstancesChanged += OnInstancesChanged;
            InstanceSelectionService.SelectedInstanceChanged += OnInstanceSelectionChanged;

            Loaded += (_, _) =>
            {
                DisableMaximizeButton();
                SetHalfScreenSize();
                MediaProbeBootstrapService.EnsureInstalled();
                BatteryProbeBootstrapService.EnsureInstalled();
                LoadUserAccount();
                UpdateNavIndicators(_viewModel.ActiveButton, animate: false);
                UpdateMainContent(_viewModel.ActiveButton);
                LoadInstalledVersions();
                UpdateService.CleanOldUpdates();
                _ = RunStartupUpdateCheckAsync();

                // Startup entrance animation — converges to the current layout
                UiTransitions.PlayStartupAnimation(this, TopBarGrid, null, ContentBorder);

                // Pre-warm views in background so switching to Explore, Account, etc. is instant with 0 freeze
                PrewarmViews();

                // Discord Rich Presence
                DiscordRpcService.Instance.Start();
                DiscordRpcService.Instance.SetPresenceInLauncher();
            };
            Closed += (_, _) =>
            {
                DiscordRpcService.Instance.Stop();
                MinecraftProcessTracker.Instance.ProcessStarted -= OnGameProcessStarted;
                MinecraftProcessTracker.Instance.ProcessExited -= OnGameProcessExited;
                MinecraftProcessTracker.Instance.ReleaseRunningGame();
                InstanceService.InstancesChanged -= OnInstancesChanged;
                InstanceSelectionService.SelectedInstanceChanged -= OnInstanceSelectionChanged;
            };
        }

        #region View Caching & Instant Navigation

        private readonly Dictionary<string, System.Windows.Controls.UserControl> _viewCache = new(StringComparer.OrdinalIgnoreCase);

        private System.Windows.Controls.UserControl? GetOrCreateView(string key)
        {
            if (_viewCache.TryGetValue(key, out var cached))
                return cached;

            System.Windows.Controls.UserControl? created = key switch
            {
                "Home" or "Launch"      => new LaunchView(),
                "Versions" or "Explore" => new ExploreView(),
                "Cosmetics" or "Account"=> new AccountView(),
                "News"                  => new NewsView(),
                "Settings"              => new SettingsView(),
                "Stats"                 => new StatsView(),
                "Console"               => new ConsoleView(),
                _                       => new LaunchView()
            };

            if (created != null)
                _viewCache[key] = created;

            return created;
        }

        private void PrewarmViews()
        {
            Dispatcher.BeginInvoke(System.Windows.Threading.DispatcherPriority.Background, () =>
            {
                try
                {
                    _ = GetOrCreateView("News");
                    _ = GetOrCreateView("Account");
                    _ = GetOrCreateView("Versions");
                    _ = GetOrCreateView("Settings");
                }
                catch { }
            });
        }

        #endregion

        private void DisableMaximizeButton()
        {
            try
            {
                var helper = new WindowInteropHelper(this);
                var hwnd = helper.Handle;
                if (hwnd != IntPtr.Zero)
                {
                    var style = GetWindowLongPtr(hwnd, GWL_STYLE);
                    style = new IntPtr(style.ToInt64() & ~WS_MAXIMIZEBOX);
                    SetWindowLongPtr(hwnd, GWL_STYLE, style);

                    // Refresh window frame to apply changes
                    SetWindowPos(hwnd, IntPtr.Zero, 0, 0, 0, 0,
                        SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOOWNERZORDER);
                }
            }
            catch
            {
                // Ignore errors in window API calls
            }

            // Prevent maximization via Windows snap and programmatic changes
            StateChanged += (s, e) =>
            {
                if (WindowState == WindowState.Maximized)
                {
                    WindowState = WindowState.Normal;
                    Dispatcher.BeginInvoke(() => SetHalfScreenSize());
                }
            };
        }

        private void SetHalfScreenSize()
        {
            try
            {
                var screenWidth = SystemParameters.PrimaryScreenWidth;
                var screenHeight = SystemParameters.PrimaryScreenHeight;

                var targetWidth = screenWidth * 0.58;
                var targetHeight = screenHeight * 0.735;

                Width = targetWidth;
                Height = targetHeight;

                var left = (screenWidth - targetWidth) / 2;
                var top = (screenHeight - targetHeight) / 2;

                Left = left;
                Top = top;
            }
            catch
            {
                // Fallback to default if screen detection fails
                Width = 1114;
                Height = 794;
            }
        }

        private void OnGameProcessStarted()
        {
            Dispatcher.BeginInvoke(() =>
            {
                LauncherPostLaunchBehavior.Apply(this);
                var meta = MinecraftProcessTracker.Instance.CurrentLaunchMetadata;
                if (meta != null)
                {
                    DiscordRpcService.Instance.SetPresenceInGame("Minecraft", meta.Loader, meta.Version, meta.Server);
                }
                else
                {
                    DiscordRpcService.Instance.SetPresenceInGame("Minecraft", null, null, null);
                }
            });
        }

        private void OnGameProcessExited()
        {
            Dispatcher.BeginInvoke(() => DiscordRpcService.Instance.SetPresenceInLauncher());
        }

        private static async Task RunStartupUpdateCheckAsync()
        {
            if (!LauncherSettingsService.Instance.Current.AutoCheckUpdates)
                return;

            try
            {
                var updates = new UpdateService();
                await updates.CheckAsync();
            }
            catch { }
        }

        private void ViewModel_PropertyChanged(object? sender, PropertyChangedEventArgs e)
        {
            if (e.PropertyName != nameof(MainViewModel.ActiveButton))
                return;

            LoadUserAccount();
            UpdateNavIndicators(_viewModel.ActiveButton, animate: true);
            UpdateMainContent(_viewModel.ActiveButton);
        }

        public void NavigateTo(string section) => _viewModel.ActiveButton = section;

        // Keep track of ongoing navigation to avoid stacking
        private string? _pendingNav;

        private void UpdateMainContent(string activeButton)
        {
            // Debounce rapid clicks
            _pendingNav = activeButton;
            Dispatcher.BeginInvoke(System.Windows.Threading.DispatcherPriority.Background, () =>
            {
                if (_pendingNav != activeButton) return; // superseded
                _pendingNav = null;
                SwapContent(activeButton);
            });
        }

        private static readonly string[] NavigationOrder = ["News", "Account", "Home", "Versions", "Settings"];
        private string _currentDisplayedSection = "Home";

        private static int GetNavIndex(string? s) => (s ?? "").ToLowerInvariant() switch
        {
            "news"                  => 0,
            "account" or "cosmetics"=> 1,
            "home" or "launch"      => 2,
            "versions" or "explore" => 3,
            "settings"              => 4,
            _                       => 2
        };

        private void SwapContent(string activeButton)
        {
            try
            {
                var view = GetOrCreateView(activeButton);
                if (MainContentControl.Content == view) return;

                int oldIndex = GetNavIndex(_currentDisplayedSection);
                int newIndex = GetNavIndex(activeButton);

                double fromX = 0;
                if (newIndex > oldIndex)
                {
                    // Target is to the right: slide in from right (+65 to 0)
                    fromX = 65;
                }
                else if (newIndex < oldIndex)
                {
                    // Target is to the left: slide in from left (-65 to 0)
                    fromX = -65;
                }

                _currentDisplayedSection = activeButton;
                MainContentControl.Content = view;

                if (fromX != 0)
                {
                    var slideAnim = new DoubleAnimation
                    {
                        From = fromX,
                        To = 0,
                        Duration = TimeSpan.FromMilliseconds(240),
                        EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
                    };
                    ContentTranslate.BeginAnimation(TranslateTransform.XProperty, slideAnim);

                    var fadeAnim = new DoubleAnimation
                    {
                        From = 0.35,
                        To = 1.0,
                        Duration = TimeSpan.FromMilliseconds(200),
                        EasingFunction = new QuadraticEase { EasingMode = EasingMode.EaseOut }
                    };
                    MainContentControl.BeginAnimation(OpacityProperty, fadeAnim);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to load section:\n{ex.Message}", "Flow",
                    MessageBoxButton.OK, MessageBoxImage.Error);
                MainContentControl.Content = null;
            }
        }

        // Update the Window.Clip geometry when the window size changes
        protected override void OnRenderSizeChanged(SizeChangedInfo sizeInfo)
        {
            base.OnRenderSizeChanged(sizeInfo);
            if (WindowClipGeometry != null)
                WindowClipGeometry.Rect = new Rect(0, 0, sizeInfo.NewSize.Width, sizeInfo.NewSize.Height);
        }

        private void LoadUserAccount()
        {
            try
            {
                var account = new MinecraftAuthService().LoadSavedAccount();
                if (account == null)
                {
                    _viewModel.PlayerName = "Guest";
                    _viewModel.PlayerAvatar = null;
                    _viewModel.IsAuthenticated = false;
                    return;
                }

                _viewModel.PlayerName = string.IsNullOrWhiteSpace(account.MinecraftUsername) ? "Player" : account.MinecraftUsername;
                _viewModel.IsAuthenticated = true;
                _ = Task.Run(async () =>
                {
                    var avatar = await SkinAvatarService.LoadHeadAsync(account.SkinTextureUrl, account.MinecraftUsername, account.MinecraftUuid);
                    if (avatar != null)
                    {
                        Dispatcher.Invoke(() => _viewModel.PlayerAvatar = avatar);
                    }
                });
            }
            catch { }
        }

        private void UpdateNavIndicators(string activeButton, bool animate)
        {
            // Update active states
        }

        private void TitleBar_MouseDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left)
                DragMove();
        }

        private void Window_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left)
                DragMove();
        }

        private void Minimize_Click(object sender, RoutedEventArgs e) =>
            SystemCommands.MinimizeWindow(this);

        private void Close_Click(object sender, RoutedEventArgs e) =>
            SystemCommands.CloseWindow(this);

        private void Home_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Home";

        private void Versions_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Versions";

        private void Cosmetics_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Cosmetics";

        private void News_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "News";

        private void AccountPill_Click(object sender, MouseButtonEventArgs e)
        {
            AccountMenuOverlay.Visibility = AccountMenuOverlay.Visibility == Visibility.Visible
                ? Visibility.Collapsed
                : Visibility.Visible;
            e.Handled = true;
        }

        private void AccountMenuBackdrop_Click(object sender, MouseButtonEventArgs e)
        {
            AccountMenuOverlay.Visibility = Visibility.Collapsed;
        }

        private void AccountMenu_Manage_Click(object sender, RoutedEventArgs e)
        {
            AccountMenuOverlay.Visibility = Visibility.Collapsed;
            _viewModel.ActiveButton = "Account";
        }

        private void AccountMenu_AddAccount_Click(object sender, RoutedEventArgs e)
        {
            AccountMenuOverlay.Visibility = Visibility.Collapsed;
            _viewModel.ActiveButton = "Account";
        }

        private void AccountMenu_SignOut_Click(object sender, RoutedEventArgs e)
        {
            AccountMenuOverlay.Visibility = Visibility.Collapsed;
            var confirm = MessageBox.Show(
                $"Are you sure you want to sign out of your Microsoft account ({_viewModel.PlayerName})?",
                "Sign Out",
                MessageBoxButton.YesNo,
                MessageBoxImage.Question);

            if (confirm == MessageBoxResult.Yes)
            {
                try
                {
                    new MinecraftAuthService().DeleteSavedAccount();
                    _viewModel.PlayerName = "Guest";
                    _viewModel.PlayerAvatar = null;
                    _viewModel.IsAuthenticated = false;
                    MessageBox.Show("You have been signed out successfully.", "Signed Out", MessageBoxButton.OK, MessageBoxImage.Information);
                }
                catch (Exception ex)
                {
                    MessageBox.Show($"Failed to sign out: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                }
            }
        }

        private void Launch_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Home";

        private void Explore_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Versions";

        private void Account_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Account";

        private void Stats_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Stats";

        private void Console_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Console";

        private void Settings_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Settings";

        #region Installed Versions in Sidebar

        private void OnInstancesChanged()
        {
            LoadInstalledVersions();
        }

        private void OnInstanceSelectionChanged(string id)
        {
            Dispatcher.BeginInvoke(() =>
            {
                foreach (var item in _viewModel.InstalledVersions)
                {
                    item.IsSelected = item.Id == id;
                }
                var list = _viewModel.InstalledVersions.ToList();
                _viewModel.InstalledVersions.Clear();
                foreach (var item in list)
                    _viewModel.InstalledVersions.Add(item);
            });
        }

        public void LoadInstalledVersions()
        {
            Dispatcher.BeginInvoke(() =>
            {
                var all = _instances.GetAll()
                    .OrderByDescending(i => i.IsFavorite)
                    .ThenByDescending(i => i.LastPlayedAt ?? DateTime.MinValue)
                    .ToList();

                var currentSelected = InstanceSelectionService.SelectedInstanceId;
                if ((string.IsNullOrEmpty(currentSelected) || all.All(i => i.Id != currentSelected)) && all.Count > 0)
                {
                    currentSelected = all[0].Id;
                    InstanceSelectionService.ForceSelect(currentSelected);
                }

                _viewModel.InstalledVersions.Clear();
                foreach (var inst in all)
                {
                    _viewModel.InstalledVersions.Add(new InstanceListItemViewModel
                    {
                        Id = inst.Id,
                        Name = inst.Name,
                        Version = inst.MinecraftVersion,
                        Loader = inst.Loader,
                        LastPlayedText = inst.LastPlayedAt.HasValue ? $"{inst.LastPlayedAt.Value:dd/MM/yyyy}" : "Never",
                        IsFavorite = inst.IsFavorite,
                        LoaderIconUri = LoaderBranding.GetIconUri(inst.Loader),
                        IsSelected = inst.Id == currentSelected
                    });
                }
                _viewModel.NotifyInstalledVersionsChanged();
            });
        }

        private void SidebarInstance_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is not FrameworkElement fe || fe.Tag is not string id) return;

            InstanceSelectionService.Select(id);

            if (_viewModel.ActiveButton != "Launch")
            {
                _viewModel.ActiveButton = "Launch";
            }
        }

        #endregion

        private void Window_KeyDown(object sender, KeyEventArgs e)
        {
        }

        private void Quit_Click(object sender, RoutedEventArgs e)
        {
            // Disable all input immediately so the user can't click again during animation
            IsHitTestVisible = false;

            UiTransitions.PlayExitAnimation(
                this, TopBarGrid, null, ContentBorder,
                onCompleted: () => Application.Current.Shutdown());
        }
    }
}
