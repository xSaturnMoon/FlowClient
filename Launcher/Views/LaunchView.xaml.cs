using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Media3D;
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

        // 3D Model rotation & floating state
        private bool _isDragging3D;
        private Point _lastMouse3D;
        private double _yaw = 35.0;
        private double _pitch = 18.0;
        private double _bobTime = 0.0;
        private string? _currentLoaderModel;

        // Interactive update animation state
        private bool _isCheckingUpdate;
        private UpdateCheckResult? _availableUpdate;

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
            // Hook tracker events
            _tracker.ProcessStarted += OnProcessStarted;
            _tracker.ProcessExited += OnProcessExited;
            _tracker.LaunchStateChanged += OnLaunchStateChanged;
            InstanceSelectionService.SelectedInstanceChanged += OnInstanceSelectionChanged;
            InstanceService.InstancesChanged += OnInstancesChanged;

            // Hook Discord RPC
            DiscordRpcService.Instance.ConnectionChanged += OnDiscordRpcConnectionChanged;
            Vm.IsDiscordRpcConnected = DiscordRpcService.Instance.IsConnected;
            Vm.DiscordRpcStatus = DiscordRpcService.Instance.IsConnected ? "Discord RPC Active" : "Waiting for Discord";

            // Hook 3D rendering loop for idle floating bob & gentle spin
            CompositionTarget.Rendering += OnRendering3D;

            Refresh();
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

            CompositionTarget.Rendering -= OnRendering3D;
        }

        private void OnDiscordRpcConnectionChanged(bool connected)
        {
            Dispatcher.BeginInvoke(() =>
            {
                Vm.IsDiscordRpcConnected = connected;
                Vm.DiscordRpcStatus = connected ? "Discord RPC Active" : "Waiting for Discord";
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
                Vm.StatusMessage = "Minecraft is running";
            });
        }

        private void OnProcessExited()
        {
            Dispatcher.BeginInvoke(() =>
            {
                Vm.IsRunning = false;
                Vm.CanPlay = true;
                Vm.StatusMessage = null;
            });
        }

        private void SyncRunningState()
        {
            if (_tracker.IsRunning)
            {
                Vm.IsRunning = true;
                Vm.CanPlay = true;
                Vm.StatusMessage = "Minecraft is running";
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

        public void Refresh()
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

            vm.NotifyListState();

            if (all.Count == 0)
            {
                vm.SelectedInstanceId = null;
                vm.SelectedName = "No Instance";
                vm.SelectedVersion = "Create one in Versions";
                vm.SelectedLoader = "Vanilla";
                vm.CanPlay = false;
                UpdateLoaderBadge("Vanilla");
                Update3DModel("Vanilla");
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

            vm.CanPlay = !_tracker.IsRunning && !_tracker.IsLaunching;

            UpdateLoaderBadge(inst.Loader);
            Update3DModel(inst.Loader);

            if (refreshList)
            {
                foreach (var item in vm.Instances)
                    item.IsSelected = item.Id == id;
            }
        }

        #region Loader Badge & 3D Model Management

        private void UpdateLoaderBadge(string loader)
        {
            var l = (loader ?? "VANILLA").Trim().ToUpperInvariant();
            TxtLoaderBadge.Text = l;

            LoaderBadgeBorder.Background = l switch
            {
                "FABRIC"   => new SolidColorBrush(Color.FromRgb(59, 130, 246)),
                "QUILT"    => new SolidColorBrush(Color.FromRgb(168, 85, 247)),
                "NEOFORGE" => new SolidColorBrush(Color.FromRgb(249, 115, 22)),
                "FORGE"    => new SolidColorBrush(Color.FromRgb(100, 116, 139)),
                _          => new SolidColorBrush(Color.FromRgb(16, 185, 129))
            };
        }

        private void Update3DModel(string loader)
        {
            var norm = (loader ?? "vanilla").Trim().ToLowerInvariant();
            if (norm == _currentLoaderModel && Mascot3DGroup.Children.Count > 0)
                return;

            _currentLoaderModel = norm;
            Mascot3DGroup.Children.Clear();

            try
            {
                var model = Minecraft3DModelBuilder.CreateModelForLoader(norm);
                Mascot3DGroup.Children.Add(model);

                var scaleAnim = new DoubleAnimation(0.85, 1.0, TimeSpan.FromMilliseconds(320))
                {
                    EasingFunction = new BackEase { Amplitude = 0.4, EasingMode = EasingMode.EaseOut }
                };
                ModelScale.BeginAnimation(ScaleTransform3D.ScaleXProperty, scaleAnim);
                ModelScale.BeginAnimation(ScaleTransform3D.ScaleYProperty, scaleAnim);
                ModelScale.BeginAnimation(ScaleTransform3D.ScaleZProperty, scaleAnim);
            }
            catch { }
        }

        #endregion

        #region 3D Mouse Drag & Continuous Idle Floating

        private void Viewport_MouseDown(object sender, MouseButtonEventArgs e)
        {
            if (e.LeftButton == MouseButtonState.Pressed)
            {
                _isDragging3D = true;
                _lastMouse3D = e.GetPosition(ModelViewport);
                ModelViewport.CaptureMouse();
                e.Handled = true;
            }
        }

        private void Viewport_MouseMove(object sender, MouseEventArgs e)
        {
            if (!_isDragging3D) return;

            var current = e.GetPosition(ModelViewport);
            var dx = current.X - _lastMouse3D.X;
            var dy = current.Y - _lastMouse3D.Y;

            _yaw = (_yaw + dx * 0.65) % 360;
            _pitch = Math.Clamp(_pitch + dy * 0.65, -75, 75);

            YawRotation.Angle = _yaw;
            PitchRotation.Angle = _pitch;

            _lastMouse3D = current;
            e.Handled = true;
        }

        private void Viewport_MouseUp(object sender, MouseButtonEventArgs e)
        {
            if (_isDragging3D)
            {
                _isDragging3D = false;
                ModelViewport.ReleaseMouseCapture();
                e.Handled = true;
            }
        }

        private DateTime _lastRenderTime = DateTime.MinValue;

        private void OnRendering3D(object? sender, EventArgs e)
        {
            // Delta-time based bobbing — frame-rate independent, slow & soothing
            var now = DateTime.UtcNow;
            double delta = _lastRenderTime == DateTime.MinValue
                ? 0.0
                : (now - _lastRenderTime).TotalSeconds;
            _lastRenderTime = now;

            // Clamp delta to avoid huge jumps on lag/focus restore
            delta = Math.Min(delta, 0.1);

            // Slow bob: period ~3.5 seconds → angular speed = 2π / 3.5 ≈ 1.795 rad/s
            _bobTime += delta * 1.795;
            BobTranslate.OffsetY = Math.Sin(_bobTime) * 0.07;

            // No auto-spin — user controls yaw exclusively via mouse drag
        }

        #endregion

        #region Version Navigation Arrows (< and >)

        private void PreviousVersion_Click(object sender, RoutedEventArgs e)
        {
            var all = _instances.GetAll().ToList();
            if (all.Count <= 1) return;

            var currentId = Vm.SelectedInstanceId;
            var idx = all.FindIndex(i => i.Id == currentId);
            if (idx <= 0)
                idx = all.Count - 1;
            else
                idx--;

            InstanceSelectionService.Select(all[idx].Id);
        }

        private void NextVersion_Click(object sender, RoutedEventArgs e)
        {
            var all = _instances.GetAll().ToList();
            if (all.Count <= 1) return;

            var currentId = Vm.SelectedInstanceId;
            var idx = all.FindIndex(i => i.Id == currentId);
            if (idx < 0 || idx >= all.Count - 1)
                idx = 0;
            else
                idx++;

            InstanceSelectionService.Select(all[idx].Id);
        }

        #endregion

        #region Bottom-Left: Quit Button

        private void Quit_Click(object sender, RoutedEventArgs e)
        {
            var parentWindow = Window.GetWindow(this);
            if (parentWindow != null)
            {
                parentWindow.IsHitTestVisible = false;
                UiTransitions.PlayExitAnimation(parentWindow, null, null, this,
                    () => Application.Current.Shutdown());
            }
            else
            {
                Application.Current.Shutdown();
            }
        }

        #endregion

        #region Bottom-Right: Interactive Animated Update Button

        private async void Update_Click(object sender, RoutedEventArgs e)
        {
            if (_isCheckingUpdate) return;

            if (_availableUpdate is { UpdateAvailable: true, DownloadUrl: not null })
            {
                var dialog = MessageBox.Show(
                    $"Flow Client {_availableUpdate.LatestVersion} is available!\n\nWould you like to download and install this update now?",
                    "Flow Client Update",
                    MessageBoxButton.YesNo,
                    MessageBoxImage.Information);

                if (dialog == MessageBoxResult.Yes)
                {
                    await PerformDownloadAndInstallAsync(_availableUpdate);
                }
                return;
            }

            _isCheckingUpdate = true;
            BtnUpdate.ToolTip = "Checking for updates…";

            AnimateArrowToSpinner();

            var minAnimTime = Task.Delay(950);
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
                AnimateSpinnerToCheckmark(result.LatestVersion ?? "New version");
            }
            else
            {
                _availableUpdate = null;
                var msg = result?.Success == true
                    ? "Flow Client is up to date."
                    : (result?.Message ?? "No updates available.");
                await AnimateSpinnerToCrossAndResetAsync(msg);
            }

            _isCheckingUpdate = false;
        }

        private void AnimateArrowToSpinner()
        {
            var easeIn = new CubicEase { EasingMode = EasingMode.EaseIn };

            CheckmarkScale.BeginAnimation(ScaleTransform.ScaleXProperty, (AnimationTimeline?)null);
            CheckmarkScale.BeginAnimation(ScaleTransform.ScaleYProperty, (AnimationTimeline?)null);
            CheckmarkRotate.BeginAnimation(RotateTransform.AngleProperty, (AnimationTimeline?)null);
            IconCheckmark.BeginAnimation(UIElement.OpacityProperty, (AnimationTimeline?)null);
            IconCheckmark.Opacity = 0;
            CheckmarkScale.ScaleX = 0;
            CheckmarkScale.ScaleY = 0;

            CrossScale.BeginAnimation(ScaleTransform.ScaleXProperty, (AnimationTimeline?)null);
            CrossScale.BeginAnimation(ScaleTransform.ScaleYProperty, (AnimationTimeline?)null);
            CrossRotate.BeginAnimation(RotateTransform.AngleProperty, (AnimationTimeline?)null);
            IconCross.BeginAnimation(UIElement.OpacityProperty, (AnimationTimeline?)null);
            IconCross.Opacity = 0;
            CrossScale.ScaleX = 0;
            CrossScale.ScaleY = 0;

            SpinnerScale.BeginAnimation(ScaleTransform.ScaleXProperty, (AnimationTimeline?)null);
            SpinnerScale.BeginAnimation(ScaleTransform.ScaleYProperty, (AnimationTimeline?)null);
            SpinnerScale.ScaleX = 1.0;
            SpinnerScale.ScaleY = 1.0;

            OuterCircleSpinner.BeginAnimation(UIElement.OpacityProperty, (AnimationTimeline?)null);
            OuterCircleSpinner.Opacity = 0;

            var arrowShrink = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(220)) { EasingFunction = easeIn };
            var arrowFade = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(180));

            UpdateArrowScale.BeginAnimation(ScaleTransform.ScaleXProperty, arrowShrink);
            UpdateArrowScale.BeginAnimation(ScaleTransform.ScaleYProperty, arrowShrink);
            IconUpdateArrow.BeginAnimation(UIElement.OpacityProperty, arrowFade);

            OuterCircle.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(180)));

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

            SpinnerRotate.BeginAnimation(RotateTransform.AngleProperty, (AnimationTimeline?)null);
            OuterCircleSpinner.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(180)));
            OuterCircle.Opacity = 0;

            var checkScale = new DoubleAnimation(0.2, 1, TimeSpan.FromMilliseconds(340)) { EasingFunction = easeOut };
            var checkRotate = new DoubleAnimation(-35, 0, TimeSpan.FromMilliseconds(340)) { EasingFunction = cubicOut };
            var checkFade = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(200));

            CheckmarkScale.BeginAnimation(ScaleTransform.ScaleXProperty, checkScale);
            CheckmarkScale.BeginAnimation(ScaleTransform.ScaleYProperty, checkScale);
            CheckmarkRotate.BeginAnimation(RotateTransform.AngleProperty, checkRotate);
            IconCheckmark.BeginAnimation(UIElement.OpacityProperty, checkFade);

            BtnUpdate.ToolTip = $"Update available ({version})! Click to install.";
        }

        private async Task AnimateSpinnerToCrossAndResetAsync(string message)
        {
            var easeOut = new BackEase { Amplitude = 0.45, EasingMode = EasingMode.EaseOut };
            var cubicOut = new CubicEase { EasingMode = EasingMode.EaseOut };

            SpinnerRotate.BeginAnimation(RotateTransform.AngleProperty, (AnimationTimeline?)null);
            var spinShrink = new DoubleAnimation(1, 0.1, TimeSpan.FromMilliseconds(220));
            var spinFade = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(200));
            SpinnerScale.BeginAnimation(ScaleTransform.ScaleXProperty, spinShrink);
            SpinnerScale.BeginAnimation(ScaleTransform.ScaleYProperty, spinShrink);
            OuterCircleSpinner.BeginAnimation(UIElement.OpacityProperty, spinFade);
            OuterCircle.Opacity = 0;

            var crossScale = new DoubleAnimation(0.2, 1.0, TimeSpan.FromMilliseconds(320)) { EasingFunction = easeOut };
            var crossRotate = new DoubleAnimation(90, 0, TimeSpan.FromMilliseconds(320)) { EasingFunction = cubicOut };
            var crossFade = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(200));

            CrossScale.BeginAnimation(ScaleTransform.ScaleXProperty, crossScale);
            CrossScale.BeginAnimation(ScaleTransform.ScaleYProperty, crossScale);
            CrossRotate.BeginAnimation(RotateTransform.AngleProperty, crossRotate);
            IconCross.BeginAnimation(UIElement.OpacityProperty, crossFade);

            BtnUpdate.ToolTip = message;

            await Task.Delay(2600);

            var crossFadeOut = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(220));
            IconCross.BeginAnimation(UIElement.OpacityProperty, crossFadeOut);

            var resetEase = new CubicEase { EasingMode = EasingMode.EaseOut };
            var arrowGrow = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(260)) { EasingFunction = resetEase };
            var arrowFadeIn = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(240));
            var circleFadeIn = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(240));

            UpdateArrowScale.BeginAnimation(ScaleTransform.ScaleXProperty, arrowGrow);
            UpdateArrowScale.BeginAnimation(ScaleTransform.ScaleYProperty, arrowGrow);
            IconUpdateArrow.BeginAnimation(UIElement.OpacityProperty, arrowFadeIn);
            OuterCircle.BeginAnimation(UIElement.OpacityProperty, circleFadeIn);

            BtnUpdate.ToolTip = "Check for Updates";
        }

        private async Task PerformDownloadAndInstallAsync(UpdateCheckResult updateResult)
        {
            if (string.IsNullOrEmpty(updateResult.DownloadUrl)) return;

            _isCheckingUpdate = true;
            BtnUpdate.ToolTip = "Downloading update…";
            AnimateArrowToSpinner();

            try
            {
                var updates = new UpdateService();
                var packagePath = await updates.DownloadAsync(updateResult.DownloadUrl);
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
                MessageBox.Show($"Download failed:\n{ex.Message}", "Update Error",
                    MessageBoxButton.OK, MessageBoxImage.Error);
                await AnimateSpinnerToCrossAndResetAsync("Update failed.");
            }
            finally
            {
                _isCheckingUpdate = false;
            }
        }

        #endregion

        #region Launch Game Logic

        private async void Play_Click(object sender, RoutedEventArgs e)
        {
            await LaunchSelectedAsync(null);
        }

        private async Task LaunchSelectedAsync(LaunchServerTarget? server)
        {
            if (_tracker.IsRunning)
            {
                var confirm = MessageBox.Show(
                    "Are you sure you want to stop Minecraft and close the session?",
                    "Stop Game",
                    MessageBoxButton.YesNo,
                    MessageBoxImage.Question);

                if (confirm == MessageBoxResult.Yes)
                {
                    _tracker.KillProcess();
                }
                return;
            }

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

        private static string FormatLastPlayed(DateTime? date)
        {
            if (!date.HasValue) return "Never";
            return date.Value.ToString("dd/MM/yyyy HH:mm", EnUs);
        }

        #endregion
    }
}
