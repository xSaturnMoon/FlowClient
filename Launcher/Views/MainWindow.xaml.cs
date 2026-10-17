using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Controls;
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
        private const int WS_MINIMIZEBOX = 0x00020000;
        private const int WS_SYSMENU = 0x00080000;
        private const int SWP_FRAMECHANGED = 0x0020;
        private const int SWP_NOMOVE = 0x0002;
        private const int SWP_NOSIZE = 0x0001;
        private const int SWP_NOZORDER = 0x0004;
        private const int SWP_NOOWNERZORDER = 0x0200;

        #endregion

        private bool _isMinimizing = false;
        private bool _wasMinimized = false;
        private bool _isClosingAnimated = false;

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

                // Version Settings sidebar morphing
                LibraryViewModel.VersionSettingsModeChanged += OnVersionSettingsModeChanged;

                // Ensure hardware-accelerated root corner clipping
                UpdateRootClip();
            };
            Closed += (_, _) =>
            {
                DiscordRpcService.Instance.Stop();
                LibraryViewModel.VersionSettingsModeChanged -= OnVersionSettingsModeChanged;
                MinecraftProcessTracker.Instance.ProcessStarted -= OnGameProcessStarted;
                MinecraftProcessTracker.Instance.ProcessExited -= OnGameProcessExited;
                MinecraftProcessTracker.Instance.ReleaseRunningGame();
                InstanceService.InstancesChanged -= OnInstancesChanged;
                InstanceSelectionService.SelectedInstanceChanged -= OnInstanceSelectionChanged;
            };
        }

        private void RootBorder_SizeChanged(object sender, SizeChangedEventArgs e)
        {
            UpdateRootClip();
        }

        private void UpdateRootClip()
        {
            if (RootBorder != null && RootBorder.ActualWidth > 0 && RootBorder.ActualHeight > 0)
            {
                RootBorder.Clip = new RectangleGeometry(
                    new Rect(0, 0, RootBorder.ActualWidth, RootBorder.ActualHeight),
                    12, 12);
            }
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
                "Library" or "Versions" or "Explore" => new LibraryView(),
                "Create"                => new CreateView(),
                "LegacyExplore"         => new LegacyExploreView(),
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
                    _ = GetOrCreateView("Library");
                    _ = GetOrCreateView("Create");
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
                    var style = GetWindowLongPtr(hwnd, GWL_STYLE).ToInt64();
                    style = (style | WS_MINIMIZEBOX | WS_SYSMENU) & ~WS_MAXIMIZEBOX;
                    SetWindowLongPtr(hwnd, GWL_STYLE, new IntPtr(style));

                    // Refresh window frame to apply changes
                    SetWindowPos(hwnd, IntPtr.Zero, 0, 0, 0, 0,
                        SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOOWNERZORDER);
                }
            }
            catch
            {
                // Ignore errors in window API calls
            }

            // Prevent maximization via Windows snap and handle native-like window restore animations
            StateChanged += (s, e) =>
            {
                if (WindowState == WindowState.Maximized)
                {
                    WindowState = WindowState.Normal;
                    Dispatcher.BeginInvoke(() => SetHalfScreenSize());
                }
                else if (WindowState == WindowState.Normal)
                {
                    if (_wasMinimized)
                    {
                        _wasMinimized = false;
                        UiTransitions.PlayWindowRestoreAnimation(this);
                    }
                }
                else if (WindowState == WindowState.Minimized)
                {
                    _wasMinimized = true;
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

        private static readonly string[] NavigationOrder = ["News", "Account", "Home", "Library", "Settings"];
        private string _currentDisplayedSection = "Home";

        private static int GetNavIndex(string? s) => (s ?? "").ToLowerInvariant() switch
        {
            "news"                  => 0,
            "account" or "cosmetics"=> 1,
            "home" or "launch"      => 2,
            "library" or "versions" or "explore" => 3,
            "create"                => 4,
            "settings"              => 5,
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

        private bool _sessionEventsHooked;

        private void LoadUserAccount()
        {
            try
            {
                if (!_sessionEventsHooked)
                {
                    _sessionEventsHooked = true;
                    SessionKeepAliveService.Instance.SessionRefreshed += (updated, profile) =>
                    {
                        Dispatcher.Invoke(() =>
                        {
                            _viewModel.PlayerName = string.IsNullOrWhiteSpace(updated.MinecraftUsername) ? "Player" : updated.MinecraftUsername;
                            _viewModel.IsAuthenticated = true;
                            _ = Task.Run(async () =>
                            {
                                var avatar = await SkinAvatarService.LoadHeadAsync(updated.SkinTextureUrl, updated.MinecraftUsername, updated.MinecraftUuid);
                                if (avatar != null)
                                {
                                    Dispatcher.Invoke(() => _viewModel.PlayerAvatar = avatar);
                                }
                            });
                        });
                    };

                    SessionKeepAliveService.Instance.StateChanged += (state, status) =>
                    {
                        Dispatcher.Invoke(() =>
                        {
                            if (state is SessionState.NotLoggedIn or SessionState.Expired)
                            {
                                _viewModel.PlayerName = "Guest";
                                _viewModel.PlayerAvatar = null;
                                _viewModel.IsAuthenticated = false;
                            }
                            else if (state is SessionState.Ready or SessionState.OfflineReady)
                            {
                                var current = SessionKeepAliveService.Instance.CurrentAccount;
                                if (current != null)
                                {
                                    _viewModel.PlayerName = string.IsNullOrWhiteSpace(current.MinecraftUsername) ? "Player" : current.MinecraftUsername;
                                    _viewModel.IsAuthenticated = true;
                                }
                            }
                        });
                    };
                }

                var account = new MinecraftAuthService().LoadSavedAccount();
                if (account == null)
                {
                    _viewModel.PlayerName = "Guest";
                    _viewModel.PlayerAvatar = null;
                    _viewModel.IsAuthenticated = false;
                }
                else
                {
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

                // Proactively trigger the smart token validation & refresh in background
                _ = SessionKeepAliveService.Instance.InitializeAndValidateOnStartupAsync();
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
            {
                if (e.ClickCount == 2)
                {
                    // Ignore double click to prevent maximize
                    e.Handled = true;
                    return;
                }
                DragMove();
            }
        }

        private void Window_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left)
                DragMove();
        }

        private void Minimize_Click(object sender, RoutedEventArgs e)
        {
            if (_isMinimizing) return;

            if (!LauncherSettingsService.Instance.Current.UiAnimations)
            {
                WindowState = WindowState.Minimized;
                return;
            }

            _isMinimizing = true;
            _wasMinimized = true;
            UiTransitions.PlayWindowMinimizeAnimation(this, onCompleted: () =>
            {
                WindowState = WindowState.Minimized;
                _isMinimizing = false;
            });
        }

        private void Close_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }

        protected override void OnClosing(System.ComponentModel.CancelEventArgs e)
        {
            if (!_isClosingAnimated && LauncherSettingsService.Instance.Current.UiAnimations)
            {
                e.Cancel = true;
                _isClosingAnimated = true;
                IsHitTestVisible = false;
                UiTransitions.PlayWindowCloseAnimation(this, onCompleted: () =>
                {
                    _isClosingAnimated = true;
                    Close();
                });
                return;
            }

            base.OnClosing(e);
        }

        public void Home_Click(object? sender = null, RoutedEventArgs? e = null) =>
            _viewModel.ActiveButton = "Home";

        private void Versions_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Library";

        private void Cosmetics_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Cosmetics";

        private void News_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "News";

        private bool _isSidebarExpanded = true;

        private void SidebarToggle_Click(object sender, RoutedEventArgs e)
        {
            _isSidebarExpanded = !_isSidebarExpanded;
            var targetWidth = _isSidebarExpanded ? 195.0 : 0.0;

            var anim = new DoubleAnimation
            {
                To = targetWidth,
                Duration = TimeSpan.FromMilliseconds(200),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseInOut }
            };
            SidebarContainer.BeginAnimation(WidthProperty, anim);
        }

        private bool _isInVersionSettingsMode;

        private void OnVersionSettingsModeChanged(bool isInSettings, LibraryCardItemViewModel? item)
        {
            Dispatcher.BeginInvoke(() =>
            {
                _isInVersionSettingsMode = isInSettings;
                AnimateSidebarVersionSettingsMode(isInSettings);
            });
        }

        private void UpdateVersionSettingsSidebarActiveButton(string tab)
        {
            var activeBg = new SolidColorBrush(Color.FromRgb(0x26, 0x26, 0x29));
            var inactiveFg = new SolidColorBrush(Color.FromRgb(0x9C, 0x9C, 0x9D));

            void Apply(Button btn, bool active)
            {
                btn.SetCurrentValue(BackgroundProperty, active ? activeBg : Brushes.Transparent);
                btn.SetCurrentValue(ForegroundProperty, active ? Brushes.White : inactiveFg);
            }

            Apply(NavDiscoverBtn, tab == "Info");
            Apply(NavLibraryBtn, tab == "Mods" || tab == "Mod");
            Apply(NavTexturesBtn, tab == "Textures");
            Apply(NavShadersBtn, tab == "Shaders");
            Apply(NavModrinthBtn, tab == "Modrinth");
        }

        private void AnimateSidebarVersionSettingsMode(bool isInSettings)
        {
            var duration = TimeSpan.FromMilliseconds(200);
            var cubic = new CubicEase { EasingMode = EasingMode.EaseInOut };

            if (isInSettings)
            {
                // Active button styling for Info tab
                UpdateVersionSettingsSidebarActiveButton("Info");

                // 1. Discover -> Info
                var fadeOutDiscover = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(120));
                fadeOutDiscover.Completed += (_, _) =>
                {
                    NavDiscoverDefaultPanel.Visibility = Visibility.Collapsed;
                    NavDiscoverInfoPanel.Visibility = Visibility.Visible;
                    NavDiscoverInfoPanel.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(140)));
                };
                NavDiscoverDefaultPanel.BeginAnimation(UIElement.OpacityProperty, fadeOutDiscover);

                // 2. Library -> Mods
                var fadeOutLibrary = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(120));
                fadeOutLibrary.Completed += (_, _) =>
                {
                    NavLibraryDefaultPanel.Visibility = Visibility.Collapsed;
                    NavLibraryModPanel.Visibility = Visibility.Visible;
                    NavLibraryModPanel.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(140)));
                };
                NavLibraryDefaultPanel.BeginAnimation(UIElement.OpacityProperty, fadeOutLibrary);

                // 3. Textures -> Expand
                NavTexturesContainer.Visibility = Visibility.Visible;
                var expandTextures = new DoubleAnimation(0, 40, duration) { EasingFunction = cubic };
                var fadeInTextures = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(200));
                NavTexturesContainer.BeginAnimation(FrameworkElement.HeightProperty, expandTextures);
                NavTexturesContainer.BeginAnimation(UIElement.OpacityProperty, fadeInTextures);

                // 4. Shaders -> Expand
                NavShadersContainer.Visibility = Visibility.Visible;
                var expandShaders = new DoubleAnimation(0, 40, duration) { EasingFunction = cubic };
                var fadeInShaders = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(200));
                NavShadersContainer.BeginAnimation(FrameworkElement.HeightProperty, expandShaders);
                NavShadersContainer.BeginAnimation(UIElement.OpacityProperty, fadeInShaders);

                // 5. Modrinth -> Expand
                NavModrinthContainer.Visibility = Visibility.Visible;
                var expandModrinth = new DoubleAnimation(0, 40, duration) { EasingFunction = cubic };
                var fadeInModrinth = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(200));
                NavModrinthContainer.BeginAnimation(FrameworkElement.HeightProperty, expandModrinth);
                NavModrinthContainer.BeginAnimation(UIElement.OpacityProperty, fadeInModrinth);

                // 6. Create -> Collapse
                var collapseCreate = new DoubleAnimation(40, 0, duration) { EasingFunction = cubic };
                var fadeCreate = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(150));
                collapseCreate.Completed += (_, _) => NavCreateContainer.Visibility = Visibility.Collapsed;
                NavCreateContainer.BeginAnimation(FrameworkElement.HeightProperty, collapseCreate);
                NavCreateContainer.BeginAnimation(UIElement.OpacityProperty, fadeCreate);

                // 7. Settings -> Collapse
                var collapseSettings = new DoubleAnimation(40, 0, duration) { EasingFunction = cubic };
                var fadeSettings = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(150));
                collapseSettings.Completed += (_, _) => NavSettingsContainer.Visibility = Visibility.Collapsed;
                NavSettingsContainer.BeginAnimation(FrameworkElement.HeightProperty, collapseSettings);
                NavSettingsContainer.BeginAnimation(UIElement.OpacityProperty, fadeSettings);
            }
            else
            {
                // Restore button styling
                NavDiscoverBtn.ClearValue(BackgroundProperty);
                NavDiscoverBtn.ClearValue(ForegroundProperty);
                NavLibraryBtn.ClearValue(BackgroundProperty);
                NavLibraryBtn.ClearValue(ForegroundProperty);
                NavTexturesBtn.ClearValue(BackgroundProperty);
                NavTexturesBtn.ClearValue(ForegroundProperty);
                NavShadersBtn.ClearValue(BackgroundProperty);
                NavShadersBtn.ClearValue(ForegroundProperty);
                NavModrinthBtn.ClearValue(BackgroundProperty);
                NavModrinthBtn.ClearValue(ForegroundProperty);

                // 1. Info -> Discover
                var fadeOutInfo = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(120));
                fadeOutInfo.Completed += (_, _) =>
                {
                    NavDiscoverInfoPanel.Visibility = Visibility.Collapsed;
                    NavDiscoverDefaultPanel.Visibility = Visibility.Visible;
                    NavDiscoverDefaultPanel.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(140)));
                };
                NavDiscoverInfoPanel.BeginAnimation(UIElement.OpacityProperty, fadeOutInfo);

                // 2. Mods -> Library
                var fadeOutMod = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(120));
                fadeOutMod.Completed += (_, _) =>
                {
                    NavLibraryModPanel.Visibility = Visibility.Collapsed;
                    NavLibraryDefaultPanel.Visibility = Visibility.Visible;
                    NavLibraryDefaultPanel.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(140)));
                };
                NavLibraryModPanel.BeginAnimation(UIElement.OpacityProperty, fadeOutMod);

                // 3. Textures -> Collapse
                var collapseTextures = new DoubleAnimation(40, 0, duration) { EasingFunction = cubic };
                var fadeTextures = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(150));
                collapseTextures.Completed += (_, _) => NavTexturesContainer.Visibility = Visibility.Collapsed;
                NavTexturesContainer.BeginAnimation(FrameworkElement.HeightProperty, collapseTextures);
                NavTexturesContainer.BeginAnimation(UIElement.OpacityProperty, fadeTextures);

                // 4. Shaders -> Collapse
                var collapseShaders = new DoubleAnimation(40, 0, duration) { EasingFunction = cubic };
                var fadeShaders = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(150));
                collapseShaders.Completed += (_, _) => NavShadersContainer.Visibility = Visibility.Collapsed;
                NavShadersContainer.BeginAnimation(FrameworkElement.HeightProperty, collapseShaders);
                NavShadersContainer.BeginAnimation(UIElement.OpacityProperty, fadeShaders);

                // 5. Modrinth -> Collapse
                var collapseModrinth = new DoubleAnimation(40, 0, duration) { EasingFunction = cubic };
                var fadeModrinth = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(150));
                collapseModrinth.Completed += (_, _) => NavModrinthContainer.Visibility = Visibility.Collapsed;
                NavModrinthContainer.BeginAnimation(FrameworkElement.HeightProperty, collapseModrinth);
                NavModrinthContainer.BeginAnimation(UIElement.OpacityProperty, fadeModrinth);

                // 6. Create -> Expand
                NavCreateContainer.Visibility = Visibility.Visible;
                var expandCreate = new DoubleAnimation(0, 40, duration) { EasingFunction = cubic };
                var fadeInCreate = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(200));
                NavCreateContainer.BeginAnimation(FrameworkElement.HeightProperty, expandCreate);
                NavCreateContainer.BeginAnimation(UIElement.OpacityProperty, fadeInCreate);

                // 7. Settings -> Expand
                NavSettingsContainer.Visibility = Visibility.Visible;
                var expandSettings = new DoubleAnimation(0, 40, duration) { EasingFunction = cubic };
                var fadeInSettings = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(200));
                NavSettingsContainer.BeginAnimation(FrameworkElement.HeightProperty, expandSettings);
                NavSettingsContainer.BeginAnimation(UIElement.OpacityProperty, fadeInSettings);
            }
        }

        private void NavDiscover_Click(object sender, RoutedEventArgs e)
        {
            if (_isInVersionSettingsMode)
            {
                UpdateVersionSettingsSidebarActiveButton("Info");
                if (_viewCache.GetValueOrDefault("Library") is LibraryView lv)
                {
                    lv.Vm.SettingsTab = "Info";
                }
                return;
            }
            NavigateTo("Home");
        }

        private void NavLibrary_Click(object sender, RoutedEventArgs e)
        {
            if (_isInVersionSettingsMode)
            {
                UpdateVersionSettingsSidebarActiveButton("Mods");
                if (_viewCache.GetValueOrDefault("Library") is LibraryView lv)
                {
                    lv.Vm.SettingsTab = "Mods";
                }
                return;
            }
            NavigateTo("Library");
        }

        private void NavTextures_Click(object sender, RoutedEventArgs e)
        {
            if (_isInVersionSettingsMode)
            {
                UpdateVersionSettingsSidebarActiveButton("Textures");
                if (_viewCache.GetValueOrDefault("Library") is LibraryView lv)
                {
                    lv.Vm.SettingsTab = "Textures";
                }
            }
        }

        private void NavShaders_Click(object sender, RoutedEventArgs e)
        {
            if (_isInVersionSettingsMode)
            {
                UpdateVersionSettingsSidebarActiveButton("Shaders");
                if (_viewCache.GetValueOrDefault("Library") is LibraryView lv)
                {
                    lv.Vm.SettingsTab = "Shaders";
                }
            }
        }

        private void NavModrinth_Click(object sender, RoutedEventArgs e)
        {
            if (_isInVersionSettingsMode)
            {
                UpdateVersionSettingsSidebarActiveButton("Modrinth");
                if (_viewCache.GetValueOrDefault("Library") is LibraryView lv)
                {
                    lv.Vm.SettingsTab = "Modrinth";
                }
            }
        }

        private void NavCreate_Click(object sender, RoutedEventArgs e)
        {
            if (_isInVersionSettingsMode) return;
            NavigateTo("Create");
        }

        private void NavSettings_Click(object sender, RoutedEventArgs e) =>
            NavigateTo("Settings");

        private bool _isSidebarCheckingUpdate = false;

        private void AnimateSidebarArrowToSpinner()
        {
            var easeIn = new CubicEase { EasingMode = EasingMode.EaseIn };

            SidebarCheckmarkScale.BeginAnimation(ScaleTransform.ScaleXProperty, null);
            SidebarCheckmarkScale.BeginAnimation(ScaleTransform.ScaleYProperty, null);
            SidebarCheckmarkRotate.BeginAnimation(RotateTransform.AngleProperty, null);
            SidebarUpdateCheckmark.BeginAnimation(UIElement.OpacityProperty, null);
            SidebarUpdateCheckmark.Opacity = 0;
            SidebarCheckmarkScale.ScaleX = 0;
            SidebarCheckmarkScale.ScaleY = 0;

            SidebarCrossScale.BeginAnimation(ScaleTransform.ScaleXProperty, null);
            SidebarCrossScale.BeginAnimation(ScaleTransform.ScaleYProperty, null);
            SidebarCrossRotate.BeginAnimation(RotateTransform.AngleProperty, null);
            SidebarUpdateCross.BeginAnimation(UIElement.OpacityProperty, null);
            SidebarUpdateCross.Opacity = 0;
            SidebarCrossScale.ScaleX = 0;
            SidebarCrossScale.ScaleY = 0;

            SidebarSpinnerScale.BeginAnimation(ScaleTransform.ScaleXProperty, null);
            SidebarSpinnerScale.BeginAnimation(ScaleTransform.ScaleYProperty, null);
            SidebarSpinnerScale.ScaleX = 1.0;
            SidebarSpinnerScale.ScaleY = 1.0;

            SidebarUpdateSpinner.BeginAnimation(UIElement.OpacityProperty, null);
            SidebarUpdateSpinner.Opacity = 0;

            var arrowShrink = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(220)) { EasingFunction = easeIn };
            var arrowFade = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(180));

            SidebarUpdateArrowScale.BeginAnimation(ScaleTransform.ScaleXProperty, arrowShrink);
            SidebarUpdateArrowScale.BeginAnimation(ScaleTransform.ScaleYProperty, arrowShrink);
            SidebarUpdateArrow.BeginAnimation(UIElement.OpacityProperty, arrowFade);

            SidebarUpdateCircle.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(180)));

            var spinnerFadeIn = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(180));
            SidebarUpdateSpinner.BeginAnimation(UIElement.OpacityProperty, spinnerFadeIn);

            var spinRotate = new DoubleAnimation(0, 360, TimeSpan.FromMilliseconds(750))
            {
                RepeatBehavior = RepeatBehavior.Forever
            };
            SidebarSpinnerRotate.BeginAnimation(RotateTransform.AngleProperty, spinRotate);
        }

        private void AnimateSidebarSpinnerToCheckmark()
        {
            var easeOut = new BackEase { Amplitude = 0.5, EasingMode = EasingMode.EaseOut };
            var cubicOut = new CubicEase { EasingMode = EasingMode.EaseOut };

            SidebarSpinnerRotate.BeginAnimation(RotateTransform.AngleProperty, null);
            SidebarUpdateSpinner.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(180)));
            SidebarUpdateCircle.Opacity = 0;

            var checkScale = new DoubleAnimation(0.2, 1, TimeSpan.FromMilliseconds(340)) { EasingFunction = easeOut };
            var checkRotate = new DoubleAnimation(-35, 0, TimeSpan.FromMilliseconds(340)) { EasingFunction = cubicOut };
            var checkFade = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(200));

            SidebarCheckmarkScale.BeginAnimation(ScaleTransform.ScaleXProperty, checkScale);
            SidebarCheckmarkScale.BeginAnimation(ScaleTransform.ScaleYProperty, checkScale);
            SidebarCheckmarkRotate.BeginAnimation(RotateTransform.AngleProperty, checkRotate);
            SidebarUpdateCheckmark.BeginAnimation(UIElement.OpacityProperty, checkFade);
        }

        private async Task AnimateSidebarResetAsync()
        {
            await Task.Delay(2500);

            var checkFadeOut = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(220));
            SidebarUpdateCheckmark.BeginAnimation(UIElement.OpacityProperty, checkFadeOut);

            var crossFadeOut = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(220));
            SidebarUpdateCross.BeginAnimation(UIElement.OpacityProperty, crossFadeOut);

            var resetEase = new CubicEase { EasingMode = EasingMode.EaseOut };
            var arrowGrow = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(260)) { EasingFunction = resetEase };
            var arrowFadeIn = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(240));
            var circleFadeIn = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(240));

            SidebarUpdateArrowScale.BeginAnimation(ScaleTransform.ScaleXProperty, arrowGrow);
            SidebarUpdateArrowScale.BeginAnimation(ScaleTransform.ScaleYProperty, arrowGrow);
            SidebarUpdateArrow.BeginAnimation(UIElement.OpacityProperty, arrowFadeIn);
            SidebarUpdateCircle.BeginAnimation(UIElement.OpacityProperty, circleFadeIn);
        }

        private async void CheckUpdates_Click(object sender, RoutedEventArgs e)
        {
            if (_isSidebarCheckingUpdate) return;
            _isSidebarCheckingUpdate = true;
            AnimateSidebarArrowToSpinner();

            try
            {
                var updates = new UpdateService();
                var res = await updates.CheckAsync();
                if (res.UpdateAvailable)
                {
                    AnimateSidebarSpinnerToCheckmark();

                    var msg = $"A new version (v{res.LatestVersion}) is available!\n\n" +
                              (!string.IsNullOrWhiteSpace(res.ReleaseNotes) ? $"Release Notes:\n{res.ReleaseNotes}\n\n" : "") +
                              "Do you want to download and install it now?";

                    var prompt = MessageBox.Show(msg, "Flow Client Update", MessageBoxButton.YesNo, MessageBoxImage.Information);
                    if (prompt == MessageBoxResult.Yes && !string.IsNullOrEmpty(res.DownloadUrl))
                    {
                        AnimateSidebarArrowToSpinner();
                        UpdateOverlay.Visibility = Visibility.Visible;
                        UpdateOverlayStatus.Text = $"Downloading Flow Client v{res.LatestVersion}…";
                        UpdateOverlayProgressBar.Value = 0;
                        UpdateOverlayPercent.Text = "0%";
                        UpdateOverlayDetails.Text = "Starting download…";

                        var progress = new Progress<(double percent, double downloadedMb, double totalMb)>(p =>
                        {
                            Dispatcher.Invoke(() =>
                            {
                                UpdateOverlayProgressBar.Value = p.percent;
                                UpdateOverlayPercent.Text = $"{(int)p.percent}%";
                                UpdateOverlayDetails.Text = p.totalMb > 0
                                    ? $"{p.downloadedMb:0.1} MB / {p.totalMb:0.1} MB"
                                    : $"{p.downloadedMb:0.1} MB";
                            });
                        });

                        var packagePath = await updates.DownloadAsync(res.DownloadUrl, progress);

                        UpdateOverlayStatus.Text = "Extracting & Installing update…";
                        UpdateOverlayDetails.Text = "Restarting Flow Client…";
                        await Task.Delay(400);

                        var scriptPath = updates.PrepareInstall(packagePath);
                        System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
                        {
                            FileName = scriptPath,
                            UseShellExecute = true,
                            CreateNoWindow = true
                        });
                        Application.Current.Dispatcher.Invoke(() => Application.Current.Shutdown());
                        Environment.Exit(0);
                        return;
                    }

                    _ = AnimateSidebarResetAsync();
                }
                else
                {
                    AnimateSidebarSpinnerToCheckmark();
                    MessageBox.Show("Flow Client is already up to date!", "Check Updates", MessageBoxButton.OK, MessageBoxImage.Information);
                    _ = AnimateSidebarResetAsync();
                }
            }
            catch (Exception ex)
            {
                SidebarSpinnerRotate.BeginAnimation(RotateTransform.AngleProperty, null);
                SidebarUpdateSpinner.Opacity = 0;
                var crossScale = new DoubleAnimation(0.2, 1.0, TimeSpan.FromMilliseconds(320));
                SidebarCrossScale.BeginAnimation(ScaleTransform.ScaleXProperty, crossScale);
                SidebarCrossScale.BeginAnimation(ScaleTransform.ScaleYProperty, crossScale);
                SidebarUpdateCross.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(200)));
                MessageBox.Show($"Failed to check for updates: {ex.Message}", "Update Error", MessageBoxButton.OK, MessageBoxImage.Warning);
                _ = AnimateSidebarResetAsync();
            }
            finally
            {
                _isSidebarCheckingUpdate = false;
            }
        }

        private void Profile_Click(object sender, RoutedEventArgs e) =>
            NavigateTo("Account");

        private void Community_Click(object sender, RoutedEventArgs e)
        {
            AccountMenuOverlay.Visibility = AccountMenuOverlay.Visibility == Visibility.Visible
                ? Visibility.Collapsed
                : Visibility.Visible;
        }

        private void Logo_MouseDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left)
            {
                if (_isInVersionSettingsMode)
                {
                    (_viewCache.GetValueOrDefault("Library") as LibraryView)?.Vm?.CloseSettings();
                }
                NavigateTo("Home");
                e.Handled = true;
            }
        }

        private void TopSearchTextBox_GotFocus(object sender, RoutedEventArgs e)
        {
            TopSearchPlaceholder.Visibility = Visibility.Collapsed;
        }

        private void TopSearchTextBox_LostFocus(object sender, RoutedEventArgs e)
        {
            TopSearchPlaceholder.Visibility = string.IsNullOrEmpty(TopSearchTextBox.Text)
                ? Visibility.Visible
                : Visibility.Collapsed;
        }

        private void TopSearchTextBox_TextChanged(object sender, System.Windows.Controls.TextChangedEventArgs e)
        {
            if (!TopSearchTextBox.IsFocused)
            {
                TopSearchPlaceholder.Visibility = string.IsNullOrEmpty(TopSearchTextBox.Text)
                    ? Visibility.Visible
                    : Visibility.Collapsed;
            }
        }

        private void TopSearchTextBox_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Enter)
            {
                var query = TopSearchTextBox.Text.Trim();
                if (string.IsNullOrEmpty(query)) return;

                if (query.Equals("home", StringComparison.OrdinalIgnoreCase) ||
                    query.Equals("discover", StringComparison.OrdinalIgnoreCase))
                {
                    NavigateTo("Home");
                }
                else if (query.Equals("versions", StringComparison.OrdinalIgnoreCase) ||
                         query.Equals("library", StringComparison.OrdinalIgnoreCase) ||
                         query.Equals("mods", StringComparison.OrdinalIgnoreCase))
                {
                    NavigateTo("Library");
                }
                else if (query.Equals("settings", StringComparison.OrdinalIgnoreCase))
                {
                    NavigateTo("Settings");
                }
                else if (query.Equals("news", StringComparison.OrdinalIgnoreCase))
                {
                    NavigateTo("News");
                }
                else if (query.Equals("account", StringComparison.OrdinalIgnoreCase) ||
                         query.Equals("skin", StringComparison.OrdinalIgnoreCase))
                {
                    NavigateTo("Account");
                }
                else if (query.Equals("create", StringComparison.OrdinalIgnoreCase) ||
                         query.Equals("new", StringComparison.OrdinalIgnoreCase) ||
                         query.Equals("install", StringComparison.OrdinalIgnoreCase))
                {
                    NavigateTo("Create");
                }
                else
                {
                    NavigateTo("Library");
                    if (GetOrCreateView("Library") is LibraryView lv)
                    {
                        lv.ApplySearch(query);
                    }
                }
            }
        }

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
                    SessionKeepAliveService.Instance.SignOut();
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
            _viewModel.ActiveButton = "Library";

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
            if (e.Key is Key.Oem2 or Key.Divide) // '/'
            {
                if (!TopSearchTextBox.IsFocused)
                {
                    TopSearchTextBox.Focus();
                    TopSearchTextBox.SelectAll();
                    e.Handled = true;
                }
            }
            else if (e.Key == Key.Escape)
            {
                if (TopSearchTextBox.IsFocused)
                {
                    Keyboard.ClearFocus();
                    e.Handled = true;
                }
                if (AccountMenuOverlay.Visibility == Visibility.Visible)
                {
                    AccountMenuOverlay.Visibility = Visibility.Collapsed;
                    e.Handled = true;
                }
            }
        }

        private void Quit_Click(object sender, RoutedEventArgs e)
        {
            // Disable all input immediately so the user can't click again during animation
            IsHitTestVisible = false;

            UiTransitions.PlayWindowCloseAnimation(this, onCompleted: () => Application.Current.Shutdown());
        }
    }
}
