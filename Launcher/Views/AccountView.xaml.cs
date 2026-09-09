using System;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Threading;
using Launcher.Helpers;
using Launcher.Services;
using Launcher.ViewModels;
using Microsoft.Win32;

namespace Launcher.Views
{
    public partial class AccountView : UserControl
    {
        private static readonly CultureInfo En = new("en-US");

        private CancellationTokenSource? _cts;
        private readonly MinecraftAuthService _auth = new();
        private readonly SessionKeepAliveService _session = new();
        private readonly LauncherInfoService _launcherInfo = new();
        private SavedAccount? _currentAccount;
        private bool _skinViewerOpen;
        private bool _skinAnimating;
        private double _slideDistance = 520;
        private DispatcherTimer? _lockTimer;

        private static readonly string RecentSkinsDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "recent_skins");

        public AccountView()
        {
            InitializeComponent();
            _session.SessionRefreshed += OnSessionRefreshed;
            _session.StatusChanged += OnSessionStatusChanged;
            _session.RefreshFailed += OnSessionRefreshFailed;
            Loaded += OnLoaded;
            Unloaded += OnUnloaded;
        }

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            StartLockTimer();
            _ = LoadAccountAsync();
        }

        private async Task LoadAccountAsync()
        {
            var vm = GetVm();
            if (vm == null) return;

            LoadLauncherInfo(vm);

            var saved = _auth.LoadSavedAccount();
            if (saved == null) return;

            _currentAccount = saved;
            ApplyToVm(vm, saved);
            _session.Start(saved);

            if (saved.TokenExpiry > DateTime.UtcNow.AddHours(1))
            {
                vm.IsSyncing = false;
                vm.LastSyncText = FormatSyncTime(DateTime.Now);
                _ = RefreshProfileInBackgroundAsync(saved, vm);
                return;
            }

            await TryRestoreSessionAsync(saved, vm, showLoading: false);
        }

        private void OnUnloaded(object sender, RoutedEventArgs e)
        {
            _lockTimer?.Stop();
            _lockTimer = null;
            _session.Stop();
        }

        private void StartLockTimer()
        {
            if (_lockTimer != null) return;
            _lockTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
            _lockTimer.Tick += (_, _) => GetVm()?.RefreshProfileLockState();
            _lockTimer.Start();
        }

        private async Task TryRestoreSessionAsync(SavedAccount saved, AccountViewModel vm, bool showLoading)
        {
            if (string.IsNullOrEmpty(saved.MsaRefreshToken))
            {
                vm.IsAuthenticated = false;
                return;
            }

            if (showLoading)
            {
                vm.IsLoading = true;
                vm.StatusMessage = "Restoring session…";
            }

            vm.IsSyncing = true;
            try
            {
                _cts = new CancellationTokenSource();
                await _session.EnsureFreshSessionAsync(_cts.Token);
            }
            catch { }
            finally
            {
                vm.IsSyncing = false;
                vm.IsLoading = false;
                vm.StatusMessage = string.Empty;
                vm.LastSyncText = FormatSyncTime(DateTime.Now);
            }
        }

        private async Task RefreshProfileInBackgroundAsync(SavedAccount saved, AccountViewModel vm)
        {
            try
            {
                var profile = await _auth.GetProfileAsync(saved.MinecraftToken, CancellationToken.None);
                var freshUrl = MinecraftAuthService.ResolveSkinUrl(profile);

                Dispatcher.Invoke(() =>
                {
                    if (freshUrl != saved.SkinTextureUrl)
                    {
                        saved.SkinTextureUrl = freshUrl;
                        _auth.SaveAccount(saved);
                        vm.SkinTextureUrl = freshUrl;
                    }
                    ApplyProfileToVm(vm, profile);
                    vm.LastSyncText = FormatSyncTime(DateTime.Now);
                    _ = RefreshProfilePictureAsync(vm, saved);
                });
            }
            catch { }
        }

        private void OnSessionRefreshed(SavedAccount account, MinecraftProfile profile)
        {
            Dispatcher.Invoke(() =>
            {
                _currentAccount = account;
                var vm = GetVm();
                if (vm == null) return;

                vm.TokenExpiryText = account.TokenExpiry > DateTime.MinValue
                    ? $"Valid until {account.TokenExpiry.ToLocalTime():MMM d, HH:mm}"
                    : "—";
                vm.LastSyncText = FormatSyncTime(DateTime.Now);
                ApplyProfileToVm(vm, profile);
            });
        }

        private void OnSessionStatusChanged(string status)
        {
            Dispatcher.Invoke(() =>
            {
                var vm = GetVm();
                if (vm == null) return;
                vm.IsSyncing = status.Contains("Sync", StringComparison.OrdinalIgnoreCase);
            });
        }

        private void OnSessionRefreshFailed(Exception _) { }

        private async void Login_Click(object sender, RoutedEventArgs e)
        {
            var vm = GetVm();
            if (vm == null) return;

            _cts?.Cancel();
            _cts = new CancellationTokenSource();

            vm.IsLoading = true;
            vm.ShowDeviceCode = false;
            vm.StatusMessage = "Requesting code from Microsoft…";

            try
            {
                var dc = await _auth.RequestDeviceCodeAsync();
                vm.UserCode = dc.UserCode;
                vm.VerificationUrl = dc.VerificationUri;
                vm.ShowDeviceCode = true;
                vm.StatusMessage = "Waiting for confirmation…";
                OpenUrl(dc.VerificationUri);

                var (profile, saved) = await _auth.AuthenticateAsync(
                    dc,
                    msg => Dispatcher.Invoke(() => vm.StatusMessage = msg),
                    _cts.Token);

                _currentAccount = saved;
                ApplyToVm(vm, saved);
                _session.Start(saved);
                vm.IsSyncing = false;
                vm.LastSyncText = FormatSyncTime(DateTime.Now);
            }
            catch (OperationCanceledException) { vm.StatusMessage = "Sign-in cancelled."; }
            catch (Exception ex)
            {
                MessageBox.Show(ex.Message, "Authentication Error",
                    MessageBoxButton.OK, MessageBoxImage.Error);
                vm.StatusMessage = "Sign-in failed.";
            }
            finally
            {
                vm.IsLoading = false;
                vm.ShowDeviceCode = false;
            }
        }

        private void CancelLogin_Click(object sender, RoutedEventArgs e)
        {
            _cts?.Cancel();
            var vm = GetVm();
            if (vm == null) return;
            vm.ShowDeviceCode = false;
            vm.IsLoading = false;
            vm.StatusMessage = "Sign-in cancelled.";
        }

        private void OpenBrowser_Click(object sender, RoutedEventArgs e)
        {
            var vm = GetVm();
            if (!string.IsNullOrEmpty(vm?.VerificationUrl)) OpenUrl(vm.VerificationUrl);
        }

        private async void RefreshSession_Click(object sender, RoutedEventArgs e)
        {
            if (_currentAccount == null) return;
            var vm = GetVm();
            if (vm == null) return;

            vm.IsSyncing = true;
            vm.LastSyncText = "Syncing…";
            _cts?.Cancel();
            _cts = new CancellationTokenSource();

            try
            {
                var (profile, updated) = await _auth.RefreshAccountAsync(
                    _currentAccount,
                    msg => Dispatcher.Invoke(() => vm.LastSyncText = msg),
                    _cts.Token);

                _currentAccount = updated;
                _session.Start(updated);
                ApplyToVm(vm, updated);
                vm.IsSyncing = false;
                vm.LastSyncText = FormatSyncTime(DateTime.Now);
            }
            catch (Exception ex)
            {
                vm.IsSyncing = false;
                vm.LastSyncText = FormatSyncTime(DateTime.Now);
                MessageBox.Show(ex.Message, "Session Refresh", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void PickSkin_Click(object sender, RoutedEventArgs e)
        {
            var dlg = new OpenFileDialog
            {
                Title = "Select Minecraft skin (PNG)",
                Filter = "PNG skin (*.png)|*.png",
                Multiselect = false
            };
            if (dlg.ShowDialog() != true) return;
            ApplySkinFile(dlg.FileName);
        }

        private void ApplySkinFile(string path)
        {
            var vm = GetVm();
            if (vm == null) return;

            vm.SelectedSkinFilePath = path;
            vm.SelectedSkinFileName = Path.GetFileName(path);
            vm.SkinUploadStatus = string.Empty;
        }

        private void SkinModel_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not Button btn || btn.Tag is not string variant) return;

            var vm = GetVm();
            if (vm == null) return;

            vm.SelectedSkinVariant = variant;
            UpdateSkinModelChips(variant);
        }

        private void UpdateSkinModelChips(string variant)
        {
            if (!IsInitialized) return;
            var isSlim = variant == "slim";
            ChipClassic.Style = (Style)FindResource(isSlim ? "ExploreNavPill" : "ExploreNavPillActive");
            ChipSlim.Style = (Style)FindResource(isSlim ? "ExploreNavPillActive" : "ExploreNavPill");
        }

        private async void UploadSkin_Click(object sender, RoutedEventArgs e)
        {
            var vm = GetVm();
            if (vm == null || !vm.CanApplySkin) return;
            if (_currentAccount == null)
            {
                MessageBox.Show("No active account.", "Error", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            vm.IsUploadingSkin = true;
            vm.SkinUploadStatus = "Applying…";
            string? tempSkinPath = null;

            try
            {
                _cts = new CancellationTokenSource();
                var skinPath = vm.SelectedSkinFilePath;
                if (string.IsNullOrEmpty(skinPath))
                {
                    tempSkinPath = await _auth.DownloadSkinToTempAsync(vm.SkinTextureUrl!, _cts.Token);
                    skinPath = tempSkinPath;
                }

                var hadFile = !string.IsNullOrEmpty(vm.SelectedSkinFilePath);
                var profile = await ProfileModificationGuard.ExecuteAsync(() =>
                    _auth.UploadSkinAsync(
                        _currentAccount.MinecraftToken,
                        skinPath!,
                        vm.SelectedSkinVariant,
                        _cts.Token),
                    _cts.Token);

                if (hadFile)
                {
                    vm.SkinUploadStatus = "Skin applied.";
                    AddSkinToRecent(vm.SelectedSkinFilePath!);
                }
                else
                {
                    vm.SkinUploadStatus = "Model updated.";
                }

                ApplyProfileAfterChange(vm, profile);
            }
            catch (ProfileModificationException ex)
            {
                vm.SkinUploadStatus = ex.Message;
            }
            catch (ProfileApiException ex)
            {
                vm.SkinUploadStatus = ex.Message;
            }
            catch (Exception ex)
            {
                vm.SkinUploadStatus = ex.Message;
                MessageBox.Show(ex.Message, "Skin", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
            finally
            {
                if (!string.IsNullOrEmpty(tempSkinPath))
                {
                    try { File.Delete(tempSkinPath); } catch { }
                }
                vm.IsUploadingSkin = false;
                vm.RefreshProfileLockState();
            }
        }

        private void ApplyProfileAfterChange(AccountViewModel vm, MinecraftProfile profile)
        {
            if (_currentAccount == null) return;

            _currentAccount.SkinTextureUrl = MinecraftAuthService.ResolveSkinUrl(profile);
            _currentAccount.SkinVariant = MinecraftAuthService.ResolveActiveSkinVariant(profile);
            _auth.SaveAccount(_currentAccount);

            vm.SelectedSkinFilePath = null;
            vm.SelectedSkinFileName = null;
            ApplyProfileToVm(vm, profile);
            _ = RefreshProfilePictureAsync(vm, _currentAccount);
        }

        private async void CapeItem_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is not FrameworkElement fe || fe.DataContext is not CapeOptionViewModel cape)
                return;
            if (cape.IsActive || _currentAccount == null)
                return;

            var vm = GetVm();
            if (vm == null || vm.IsUpdatingCape)
                return;

            if (ProfileModificationGuard.IsCoolingDown)
            {
                vm.CapeStatus = ProfileModificationGuard.FormatRemaining();
                vm.RefreshProfileLockState();
                return;
            }

            vm.IsUpdatingCape = true;
            vm.CapeStatus = "Updating cape…";
            _cts?.Cancel();
            _cts = new CancellationTokenSource();

            try
            {
                var token = _currentAccount.MinecraftToken;
                var profile = await ProfileModificationGuard.ExecuteAsync(() =>
                    string.IsNullOrEmpty(cape.Id)
                        ? _auth.DeactivateCapeAsync(token, _cts.Token)
                        : _auth.SetActiveCapeAsync(token, cape.Id, _cts.Token),
                    _cts.Token);

                ApplyProfileToVm(vm, profile);
                vm.CapeStatus = "Cape updated.";

                if (_skinViewerOpen)
                    await RefreshSkinViewerAsync(vm);
            }
            catch (ProfileModificationException ex)
            {
                vm.CapeStatus = ex.Message;
            }
            catch (ProfileApiException ex)
            {
                vm.CapeStatus = ex.Message;
            }
            catch (Exception ex)
            {
                vm.CapeStatus = ex.Message;
            }
            finally
            {
                vm.IsUpdatingCape = false;
                vm.RefreshProfileLockState();
            }
        }

        private async Task RefreshSkinViewerAsync(AccountViewModel vm)
        {
            var capeUrl = ResolveActiveCapeUrl(vm);
            var hasCape = !string.IsNullOrEmpty(capeUrl);
            await SkinViewer.LoadAppearanceAsync(
                vm.SkinTextureUrl,
                capeUrl,
                vm.SelectedSkinVariant == "slim",
                hasCape);
        }

        private static string? ResolveActiveCapeUrl(AccountViewModel vm)
        {
            if (!string.IsNullOrEmpty(vm.ActiveCapeTextureUrl))
                return vm.ActiveCapeTextureUrl;

            return vm.CapeOptions.FirstOrDefault(c => c.IsActive && !string.IsNullOrEmpty(c.TextureUrl))?.TextureUrl;
        }

        private void AccountView_DragOver(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent(DataFormats.FileDrop))
            {
                var files = (string[])e.Data.GetData(DataFormats.FileDrop)!;
                if (files?.Length > 0 && files[0].EndsWith(".png", StringComparison.OrdinalIgnoreCase))
                {
                    e.Effects = DragDropEffects.Copy;
                    e.Handled = true;
                    return;
                }
            }
            e.Effects = DragDropEffects.None;
            e.Handled = true;
        }

        private void AccountView_Drop(object sender, DragEventArgs e)
        {
            if (!e.Data.GetDataPresent(DataFormats.FileDrop)) return;
            var files = (string[])e.Data.GetData(DataFormats.FileDrop)!;
            if (files == null || files.Length == 0) return;

            var path = files[0];
            if (!path.EndsWith(".png", StringComparison.OrdinalIgnoreCase)) return;

            ApplySkinFile(path);
            var vm = GetVm();
            if (vm != null)
                vm.SkinUploadStatus = "Skin imported — press Apply to upload.";
        }

        private void AddSkinToRecent(string sourcePath)
        {
            try
            {
                Directory.CreateDirectory(RecentSkinsDir);
                var dest = Path.Combine(RecentSkinsDir, $"skin_{DateTime.UtcNow.Ticks}.png");
                File.Copy(sourcePath, dest, overwrite: true);
                foreach (var old in Directory.GetFiles(RecentSkinsDir, "*.png")
                    .Select(f => new FileInfo(f))
                    .OrderByDescending(fi => fi.LastWriteTime)
                    .Skip(8))
                {
                    try { old.Delete(); } catch { }
                }
            }
            catch { }
        }

        private void CopyUuid_Click(object sender, MouseButtonEventArgs e)
        {
            var vm = GetVm();
            if (vm?.FormattedUuid == null) return;
            try { Clipboard.SetText(vm.FormattedUuid); } catch { }
        }

        private void Logout_Click(object sender, RoutedEventArgs e)
        {
            var result = MessageBox.Show(
                "Sign out of this device?\nYour local session will be removed.",
                "Sign out", MessageBoxButton.YesNo, MessageBoxImage.Question);
            if (result != MessageBoxResult.Yes) return;

            _session.Stop();
            _auth.DeleteSavedAccount();
            _currentAccount = null;
            var vm = GetVm();
            if (vm == null) return;

            vm.IsAuthenticated = false;
            vm.MinecraftUsername = null;
            vm.FormattedUuid = null;
            vm.ShortUuid = null;
            vm.Email = null;
            vm.SkinTextureUrl = null;
            vm.StatusMessage = string.Empty;
            vm.SelectedSkinFilePath = null;
            vm.SelectedSkinFileName = null;
            vm.SkinUploadStatus = null;
            vm.ProfilePicture = null;
            vm.LastSyncText = null;
            vm.CapeOptions = new ObservableCollection<CapeOptionViewModel>();
            vm.CapeStatus = null;
            vm.ActiveCapeTextureUrl = null;
        }

        private void ApplyToVm(AccountViewModel vm, SavedAccount saved)
        {
            vm.IsAuthenticated = true;
            vm.MinecraftUsername = saved.MinecraftUsername;
            vm.FormattedUuid = AccountViewModel.FormatUuid(saved.MinecraftUuid);
            vm.ShortUuid = FormatShortUuid(saved.MinecraftUuid);
            vm.Email = NormalizeEmail(saved.MicrosoftEmail);
            vm.SkinTextureUrl = saved.SkinTextureUrl;
            vm.SelectedSkinVariant = saved.SkinVariant == "slim" ? "slim" : "classic";
            vm.ActiveSkinVariant = vm.SelectedSkinVariant;
            vm.TokenExpiryText = saved.TokenExpiry > DateTime.MinValue
                ? $"Valid until {saved.TokenExpiry.ToLocalTime():MMM d, HH:mm}"
                : "—";
            vm.LinkedAtText = saved.LinkedAt != default
                ? saved.LinkedAt.ToLocalTime().ToString("MMM d, yyyy", En)
                : "Linked";
            vm.LastRefreshText = saved.LastRefreshAt != default
                ? saved.LastRefreshAt.ToLocalTime().ToString("MMM d, yyyy HH:mm", En)
                : "—";
            vm.LastLoginText = saved.LastLoginAt != default
                ? saved.LastLoginAt.ToLocalTime().ToString("MMM d, yyyy HH:mm", En)
                : "—";
            vm.LastSyncText = FormatSyncTime(DateTime.Now);
            vm.StatusMessage = string.Empty;

            UpdateSkinModelChips(vm.SelectedSkinVariant);

            if (!string.IsNullOrEmpty(saved.MinecraftUsername))
                _ = RefreshProfilePictureAsync(vm, saved);

            _ = LoadProfileDetailsAsync(vm, saved);
        }

        private async Task LoadProfileDetailsAsync(AccountViewModel vm, SavedAccount saved)
        {
            try
            {
                var profile = await _auth.GetProfileAsync(saved.MinecraftToken, CancellationToken.None);
                Dispatcher.Invoke(() => ApplyProfileToVm(vm, profile));
            }
            catch { }
        }

        private void ApplyProfileToVm(AccountViewModel vm, MinecraftProfile profile)
        {
            var variant = MinecraftAuthService.ResolveActiveSkinVariant(profile);
            vm.ActiveSkinVariant = variant;
            if (string.IsNullOrEmpty(vm.SelectedSkinFilePath) && !vm.HasPendingSkinChange)
                vm.SelectedSkinVariant = variant;
            UpdateSkinModelChips(vm.SelectedSkinVariant);

            vm.SkinTextureUrl = MinecraftAuthService.ResolveSkinUrl(profile);

            var activeCape = MinecraftAuthService.ResolveActiveCape(profile);
            vm.ActiveCapeName = activeCape?.Alias ?? "None";
            vm.ActiveCapeTextureUrl = activeCape?.Url;

            if (profile.Capes == null || profile.Capes.Count == 0)
            {
                vm.CapeOptions = new ObservableCollection<CapeOptionViewModel>();
                return;
            }

            var options = new ObservableCollection<CapeOptionViewModel>
            {
                new()
                {
                    Id = null,
                    DisplayName = "None",
                    IsActive = activeCape == null
                }
            };

            foreach (var cape in profile.Capes ?? Enumerable.Empty<MinecraftCape>())
            {
                options.Add(new CapeOptionViewModel
                {
                    Id = cape.Id,
                    TextureUrl = cape.Url,
                    DisplayName = FormatCapeName(cape.Alias),
                    IsActive = cape.State == "ACTIVE"
                });
            }

            vm.CapeOptions = options;

            if (_currentAccount != null)
            {
                _currentAccount.SkinVariant = variant;
                _currentAccount.SkinTextureUrl = vm.SkinTextureUrl;
                _auth.SaveAccount(_currentAccount);
            }
        }

        private static string FormatCapeName(string? alias)
        {
            if (string.IsNullOrWhiteSpace(alias))
                return "Cape";
            return alias.Replace('_', ' ');
        }

        private async void ViewSkin_Click(object sender, RoutedEventArgs e)
        {
            if (_skinViewerOpen || _skinAnimating)
                return;

            var vm = GetVm();
            if (vm == null || !vm.IsAuthenticated)
                return;

            _skinAnimating = true;
            _slideDistance = Math.Max(AccountStage.ActualHeight, 480);

            SkinViewerPanel.Visibility = Visibility.Visible;
            SkinViewerPanel.IsHitTestVisible = true;
            SkinViewerTransform.Y = -_slideDistance;
            AccountPanelTransform.Y = 0;

            var capeUrl = ResolveActiveCapeUrl(vm);
            var hasCape = !string.IsNullOrEmpty(capeUrl);
            await SkinViewer.LoadAppearanceAsync(
                vm.SkinTextureUrl,
                capeUrl,
                vm.SelectedSkinVariant == "slim",
                hasCape);

            var showTask = UiTransitions.AnimateTranslateYAsync(SkinViewerTransform, 0);
            var hideTask = UiTransitions.AnimateTranslateYAsync(AccountPanelTransform, _slideDistance);
            await Task.WhenAll(showTask, hideTask);

            _skinViewerOpen = true;
            _skinAnimating = false;
        }

        private async void CloseSkinPreview_Click(object sender, RoutedEventArgs e)
        {
            if (!_skinViewerOpen || _skinAnimating)
                return;

            _skinAnimating = true;

            var hideTask = UiTransitions.AnimateTranslateYAsync(SkinViewerTransform, -_slideDistance);
            var showTask = UiTransitions.AnimateTranslateYAsync(AccountPanelTransform, 0);
            await Task.WhenAll(hideTask, showTask);

            SkinViewerPanel.Visibility = Visibility.Collapsed;
            SkinViewerPanel.IsHitTestVisible = false;
            _skinViewerOpen = false;
            _skinAnimating = false;
        }

        private static string NormalizeEmail(string? email)
        {
            if (string.IsNullOrEmpty(email) || email == "Account Microsoft collegato")
                return "Microsoft account linked";
            return email;
        }

        private static string FormatShortUuid(string? uuid)
        {
            if (string.IsNullOrEmpty(uuid)) return "—";
            var clean = uuid.Replace("-", "");
            return clean.Length >= 8 ? clean[..8] : uuid;
        }

        private async Task RefreshProfilePictureAsync(AccountViewModel vm, SavedAccount account)
        {
            var head = await SkinAvatarService.LoadHeadAsync(
                account.SkinTextureUrl,
                account.MinecraftUsername,
                account.MinecraftUuid);

            if (head == null)
                return;

            Dispatcher.Invoke(() => vm.ProfilePicture = head);
        }

        private void LoadLauncherInfo(AccountViewModel vm)
        {
            var info = _launcherInfo.Gather();
            vm.LauncherVersion = info.LauncherVersion;
            vm.LauncherChannel = info.Channel;
            vm.JavaVersion = info.JavaVersion;
        }

        private static string FormatSyncTime(DateTime time)
        {
            var diff = DateTime.Now - time;
            if (diff.TotalSeconds < 60) return "Synced just now";
            if (diff.TotalMinutes < 60) return $"Synced {diff.Minutes} min ago";
            return $"Synced at {time:HH:mm}";
        }

        private AccountViewModel? GetVm() => DataContext as AccountViewModel;

        private static void OpenUrl(string url)
        {
            try { Process.Start(new ProcessStartInfo { FileName = url, UseShellExecute = true }); }
            catch { try { Process.Start("explorer.exe", url); } catch { } }
        }
    }
}

