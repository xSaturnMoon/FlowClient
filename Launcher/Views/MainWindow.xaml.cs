using System.ComponentModel;
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
        private static readonly string[] TopNavTags = ["Launch", "Explore", "Account"];
        private static readonly string[] BottomNavTags = ["Settings"];
        private const double NavItemStep = 57;

        private readonly MainViewModel _viewModel;
        private readonly AccountViewModel _accountViewModel;

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
            Loaded += (_, _) =>
            {
                DisableMaximizeButton();
                SetHalfScreenSize();
                MediaProbeBootstrapService.EnsureInstalled();
                BatteryProbeBootstrapService.EnsureInstalled();
                UpdateNavIndicators(_viewModel.ActiveButton, animate: false);
                UpdateMainContent(_viewModel.ActiveButton);
                _ = RunStartupUpdateCheckAsync();

                // Startup entrance animation — converges to the current layout, changes nothing permanently
                UiTransitions.PlayStartupAnimation(this, SidebarBorder, MiniSidebarGrid, ContentBorder);

                // Pre-warm views in background so switching to Explore, Account, etc. is instant with 0 freeze
                PrewarmViews();
            };
            Closed += (_, _) =>
            {
                MinecraftProcessTracker.Instance.ProcessStarted -= OnGameProcessStarted;
                MinecraftProcessTracker.Instance.ReleaseRunningGame();
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
                "Launch"   => new LaunchView(),
                "Account"  => new AccountView(),
                "Explore"  => new ExploreView(),
                "Settings" => new SettingsView(),
                "Stats"    => new StatsView(),
                "Console"  => new ConsoleView(),
                _          => null
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
                    _ = GetOrCreateView("Explore");
                    _ = GetOrCreateView("Account");
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

        private void OnGameProcessStarted() =>
            Dispatcher.BeginInvoke(() => LauncherPostLaunchBehavior.Apply(this));

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

        private void SwapContent(string activeButton)
        {
            try
            {
                var view = GetOrCreateView(activeButton);
                MainContentControl.Content = view;
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

        private void UpdateNavIndicators(string activeButton, bool animate)
        {
            var topIndex = Array.IndexOf(TopNavTags, activeButton);
            var bottomIndex = Array.IndexOf(BottomNavTags, activeButton);

            if (topIndex >= 0)
            {
                NavIndicator.Visibility = Visibility.Visible;
                BottomNavIndicator.Visibility = Visibility.Collapsed;
                MoveIndicator(NavIndicatorTransform, topIndex * NavItemStep, animate);
            }
            else if (bottomIndex >= 0)
            {
                NavIndicator.Visibility = Visibility.Collapsed;
                BottomNavIndicator.Visibility = Visibility.Visible;
                MoveIndicator(BottomNavIndicatorTransform, bottomIndex * NavItemStep, animate);
            }
            else
            {
                NavIndicator.Visibility = Visibility.Collapsed;
                BottomNavIndicator.Visibility = Visibility.Collapsed;
            }
        }

        private static void MoveIndicator(TranslateTransform transform, double targetY, bool animate)
        {
            if (!animate)
            {
                transform.Y = targetY;
                return;
            }

            UiTransitions.AnimateIndicatorTo(transform, targetY);
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

        private void Launch_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Launch";

        private void Explore_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Explore";

        private void Account_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Account";

        private void Stats_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Stats";

        private void Console_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Console";

        private void Settings_Click(object sender, RoutedEventArgs e) =>
            _viewModel.ActiveButton = "Settings";

        #region Interactive Update Animation & Check Logic

        private bool _isCheckingUpdate;
        private UpdateCheckResult? _availableUpdate;

        private async void Update_Click(object sender, RoutedEventArgs e)
        {
            if (_isCheckingUpdate)
                return;

            // If an update was already found and the checkmark is shown, clicking downloads & installs!
            if (_availableUpdate is { UpdateAvailable: true, DownloadUrl: not null })
            {
                var dialog = MessageBox.Show(
                    $"È disponibile la nuova versione {_availableUpdate.LatestVersion} di Flow Client!\n\nVuoi scaricare ed installare l'aggiornamento ora?",
                    "Aggiornamento Flow Client",
                    MessageBoxButton.YesNo,
                    MessageBoxImage.Information);

                if (dialog == MessageBoxResult.Yes)
                {
                    await PerformDownloadAndInstallAsync(_availableUpdate);
                }
                return;
            }

            // In-place update check with smooth morphing animations (no view category change!)
            _isCheckingUpdate = true;
            BtnUpdate.ToolTip = "Controllo aggiornamenti in corso…";

            // 1. Morph from Arrow to spinning loader
            AnimateArrowToSpinner();

            var minAnimTime = Task.Delay(950); // Guarantees smooth visible spin even if check is instant
            UpdateCheckResult? result = null;

            try
            {
                var checkTask = Task.Run(async () =>
                {
                    var updates = new UpdateService();
                    return await updates.CheckAsync();
                });

                await Task.WhenAll(checkTask, minAnimTime);
                result = await checkTask;
            }
            catch (Exception ex)
            {
                result = new UpdateCheckResult
                {
                    Success = false,
                    Message = ex.Message
                };
            }

            if (result is { Success: true, UpdateAvailable: true })
            {
                _availableUpdate = result;
                AnimateSpinnerToCheckmark(result.LatestVersion ?? "Nuova versione");
            }
            else
            {
                _availableUpdate = null;
                var msg = result?.Success == true
                    ? "Il client è aggiornato all'ultima versione."
                    : (result?.Message ?? "Nessun aggiornamento disponibile.");
                await AnimateSpinnerToCrossAndResetAsync(msg);
            }

            _isCheckingUpdate = false;
        }

        private void AnimateArrowToSpinner()
        {
            var easeIn = new CubicEase { EasingMode = EasingMode.EaseIn };
            var easeOut = new CubicEase { EasingMode = EasingMode.EaseOut };

            // 0. Completely detach any holding animations from previous runs
            CheckmarkScale.BeginAnimation(ScaleTransform.ScaleXProperty, null);
            CheckmarkScale.BeginAnimation(ScaleTransform.ScaleYProperty, null);
            CheckmarkRotate.BeginAnimation(RotateTransform.AngleProperty, null);
            IconCheckmark.BeginAnimation(UIElement.OpacityProperty, null);
            IconCheckmark.Opacity = 0;
            CheckmarkScale.ScaleX = 0;
            CheckmarkScale.ScaleY = 0;

            CrossScale.BeginAnimation(ScaleTransform.ScaleXProperty, null);
            CrossScale.BeginAnimation(ScaleTransform.ScaleYProperty, null);
            CrossRotate.BeginAnimation(RotateTransform.AngleProperty, null);
            IconCross.BeginAnimation(UIElement.OpacityProperty, null);
            IconCross.Opacity = 0;
            CrossScale.ScaleX = 0;
            CrossScale.ScaleY = 0;

            SpinnerScale.BeginAnimation(ScaleTransform.ScaleXProperty, null);
            SpinnerScale.BeginAnimation(ScaleTransform.ScaleYProperty, null);
            SpinnerScale.ScaleX = 1.0;
            SpinnerScale.ScaleY = 1.0;

            OuterCircleSpinner.BeginAnimation(UIElement.OpacityProperty, null);
            OuterCircleSpinner.Opacity = 0;

            // 1. Arrow shrinks backwards into the distance and dissolves (recedes in depth)
            var arrowShrink = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(240)) { EasingFunction = easeIn };
            var arrowFade = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(200));

            UpdateArrowScale.BeginAnimation(ScaleTransform.ScaleXProperty, arrowShrink);
            UpdateArrowScale.BeginAnimation(ScaleTransform.ScaleYProperty, arrowShrink);
            IconUpdateArrow.BeginAnimation(UIElement.OpacityProperty, arrowFade);

            // 2. The solid circle fades away — NO faint track or ghost line!
            OuterCircle.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(180)));

            // 3. The spinner arc activates on the exact same circular path and spins smoothly (Windows 11 boot style)
            var spinnerFadeIn = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(180));
            OuterCircleSpinner.BeginAnimation(UIElement.OpacityProperty, spinnerFadeIn);

            var spinRotate = new DoubleAnimation(0, 360, TimeSpan.FromMilliseconds(750))
            {
                RepeatBehavior = RepeatBehavior.Forever
            };
            SpinnerRotate.BeginAnimation(RotateTransform.AngleProperty, spinRotate);
        }

        private void AnimateSpinnerToCheckmark(string version)
        {
            var easeOut = new BackEase { Amplitude = 0.5, EasingMode = EasingMode.EaseOut };
            var cubicOut = new CubicEase { EasingMode = EasingMode.EaseOut };

            // Stop continuous spin and fade out spinner arc
            SpinnerRotate.BeginAnimation(RotateTransform.AngleProperty, null);
            OuterCircleSpinner.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(180)));
            OuterCircle.Opacity = 0; // The checkmark ITSELF is the icon!

            // Checkmark swoops in with elastic bounce — fills the full 18x18 icon size!
            var checkScale = new DoubleAnimation(0.2, 1, TimeSpan.FromMilliseconds(360)) { EasingFunction = easeOut };
            var checkRotate = new DoubleAnimation(-35, 0, TimeSpan.FromMilliseconds(360)) { EasingFunction = cubicOut };
            var checkFade = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(220));

            CheckmarkScale.BeginAnimation(ScaleTransform.ScaleXProperty, checkScale);
            CheckmarkScale.BeginAnimation(ScaleTransform.ScaleYProperty, checkScale);
            CheckmarkRotate.BeginAnimation(RotateTransform.AngleProperty, checkRotate);
            IconCheckmark.BeginAnimation(UIElement.OpacityProperty, checkFade);

            BtnUpdate.ToolTip = $"Aggiornamento disponibile ({version})! Clicca per installare.";
        }

        private async Task AnimateSpinnerToCrossAndResetAsync(string message)
        {
            var easeOut = new BackEase { Amplitude = 0.45, EasingMode = EasingMode.EaseOut };
            var cubicOut = new CubicEase { EasingMode = EasingMode.EaseOut };

            // Stop spin and decelerate / fade spinner arc
            SpinnerRotate.BeginAnimation(RotateTransform.AngleProperty, null);
            var spinShrink = new DoubleAnimation(1, 0.1, TimeSpan.FromMilliseconds(220));
            var spinFade = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(200));
            SpinnerScale.BeginAnimation(ScaleTransform.ScaleXProperty, spinShrink);
            SpinnerScale.BeginAnimation(ScaleTransform.ScaleYProperty, spinShrink);
            OuterCircleSpinner.BeginAnimation(UIElement.OpacityProperty, spinFade);
            OuterCircle.Opacity = 0; // The X ITSELF is the icon!

            // Cross rotates and snaps straight into position (full 18x18 size!)
            var crossScale = new DoubleAnimation(0.2, 1.0, TimeSpan.FromMilliseconds(340)) { EasingFunction = easeOut };
            var crossRotate = new DoubleAnimation(90, 0, TimeSpan.FromMilliseconds(340)) { EasingFunction = cubicOut };
            var crossFade = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(220));

            CrossScale.BeginAnimation(ScaleTransform.ScaleXProperty, crossScale);
            CrossScale.BeginAnimation(ScaleTransform.ScaleYProperty, crossScale);
            CrossRotate.BeginAnimation(RotateTransform.AngleProperty, crossRotate);
            IconCross.BeginAnimation(UIElement.OpacityProperty, crossFade);

            BtnUpdate.ToolTip = message;

            // Keep X visible for 2.5s so the user clearly sees they are up to date
            await Task.Delay(2500);

            // Morph X back into the default circle and arrow
            var crossShrink = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(180));
            var crossOutFade = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(160));
            CrossScale.BeginAnimation(ScaleTransform.ScaleXProperty, crossShrink);
            CrossScale.BeginAnimation(ScaleTransform.ScaleYProperty, crossShrink);
            IconCross.BeginAnimation(UIElement.OpacityProperty, crossOutFade);

            // Circle and arrow re-emerge forward from the center
            OuterCircleScale.BeginAnimation(ScaleTransform.ScaleXProperty, new DoubleAnimation(0.5, 1, TimeSpan.FromMilliseconds(280)) { EasingFunction = cubicOut });
            OuterCircleScale.BeginAnimation(ScaleTransform.ScaleYProperty, new DoubleAnimation(0.5, 1, TimeSpan.FromMilliseconds(280)) { EasingFunction = cubicOut });
            OuterCircle.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(240)));

            var arrowInScale = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(300)) { EasingFunction = easeOut };
            var arrowInFade = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(240));

            UpdateArrowScale.BeginAnimation(ScaleTransform.ScaleXProperty, arrowInScale);
            UpdateArrowScale.BeginAnimation(ScaleTransform.ScaleYProperty, arrowInScale);
            IconUpdateArrow.BeginAnimation(UIElement.OpacityProperty, arrowInFade);

            BtnUpdate.ToolTip = "Controlla aggiornamenti";
        }

        private async Task PerformDownloadAndInstallAsync(UpdateCheckResult updateResult)
        {
            if (string.IsNullOrEmpty(updateResult.DownloadUrl))
                return;

            _isCheckingUpdate = true;
            BtnUpdate.ToolTip = "Scaricamento aggiornamento in corso…";
            AnimateArrowToSpinner();

            try
            {
                var updates = new UpdateService();
                var packagePath = await updates.DownloadAsync(updateResult.DownloadUrl);
                var scriptPath = updates.PrepareInstall(packagePath);

                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
                {
                    FileName = scriptPath,
                    UseShellExecute = true,
                    CreateNoWindow = true
                });

                Application.Current.Shutdown();
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Download fallito:\n{ex.Message}", "Errore Aggiornamento",
                    MessageBoxButton.OK, MessageBoxImage.Error);
                await AnimateSpinnerToCrossAndResetAsync("Aggiornamento fallito.");
            }
            finally
            {
                _isCheckingUpdate = false;
            }
        }

        #endregion

        private void Quit_Click(object sender, RoutedEventArgs e)
        {
            // Disable all input immediately so the user can't click again during animation
            IsHitTestVisible = false;

            UiTransitions.PlayExitAnimation(
                this, SidebarBorder, MiniSidebarGrid, ContentBorder,
                onCompleted: () => Application.Current.Shutdown());
        }
    }
}
