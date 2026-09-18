using System;
using System.IO;
using System.Net.Http;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Threading;

namespace Launcher.Services
{
    public enum SessionState
    {
        Unchecked,
        Validating,
        Ready,
        OfflineReady,
        NotLoggedIn,
        Expired
    }

    /// <summary>
    /// Proactively validates, refreshes, and keeps the Microsoft / Minecraft token alive.
    /// Manages the verification lifecycle on startup so the launcher knows with 100% certainty
    /// whether the account is ready to launch before enabling the Launch button.
    /// </summary>
    public sealed class SessionKeepAliveService : IDisposable
    {
        public static SessionKeepAliveService Instance { get; } = new();

        private readonly MinecraftAuthService _auth = new();
        private readonly DispatcherTimer _timer;
        private SavedAccount? _account;
        private bool _refreshing;
        private Task<bool>? _startupValidationTask;

        public event Action<SessionState, string>? StateChanged;
        public event Action<SavedAccount, MinecraftProfile>? SessionRefreshed;
        public event Action<string>? StatusChanged;
        public event Action<Exception>? RefreshFailed;

        public SessionState State { get; private set; } = SessionState.Unchecked;
        public bool IsSessionReady => State is SessionState.Ready or SessionState.OfflineReady;
        public bool IsValidating => State == SessionState.Validating;
        public string StatusText { get; private set; } = string.Empty;
        public DateTime? LastSuccessfulSync { get; private set; }
        public SavedAccount? CurrentAccount => _account;

        public SessionKeepAliveService()
        {
            _timer = new DispatcherTimer { Interval = TimeSpan.FromMinutes(20) };
            _timer.Tick += async (_, _) => await RefreshIfNeededAsync();

            if (File.Exists(MinecraftAuthService.SavePath))
            {
                State = SessionState.Validating;
                StatusText = "Verifica sessione Microsoft in corso…";
            }
            else
            {
                State = SessionState.NotLoggedIn;
                StatusText = "Nessun account collegato";
            }
        }

        public void Start(SavedAccount account)
        {
            _account = account;
            State = SessionState.Ready;
            StatusText = "Sessione attiva";
            LastSuccessfulSync = DateTime.Now;
            _timer.Start();
            StateChanged?.Invoke(State, StatusText);
        }

        public void SignOut()
        {
            _timer.Stop();
            _account = null;
            State = SessionState.NotLoggedIn;
            StatusText = "Nessun account collegato";
            StateChanged?.Invoke(State, StatusText);
        }

        public void Stop()
        {
            _timer.Stop();
        }

        /// <summary>
        /// Instantly triggered on launcher startup to validate the token with Mojang servers
        /// or refresh it proactively before user can click Launch.
        /// </summary>
        public Task<bool> InitializeAndValidateOnStartupAsync(CancellationToken ct = default)
        {
            if (_startupValidationTask != null && !_startupValidationTask.IsCompleted)
                return _startupValidationTask;

            _startupValidationTask = RunStartupValidationAsync(ct);
            return _startupValidationTask;
        }

        private async Task<bool> RunStartupValidationAsync(CancellationToken ct)
        {
            var saved = _auth.LoadSavedAccount();
            if (saved == null || string.IsNullOrWhiteSpace(saved.MinecraftToken))
            {
                _account = null;
                SetState(SessionState.NotLoggedIn, "Nessun account collegato");
                return false;
            }

            _account = saved;
            SetState(SessionState.Validating, "Verifica sessione Microsoft in corso…");

            try
            {
                // If token still has more than 4 hours before expiration, verify with a quick profile ping
                if (saved.TokenExpiry > DateTime.UtcNow.AddHours(4))
                {
                    using var ctsFast = CancellationTokenSource.CreateLinkedTokenSource(ct);
                    ctsFast.CancelAfter(TimeSpan.FromSeconds(6));

                    try
                    {
                        var profile = await _auth.GetProfileAsync(saved.MinecraftToken, ctsFast.Token);
                        if (profile != null)
                        {
                            LastSuccessfulSync = DateTime.Now;
                            SetState(SessionState.Ready, "Sessione verificata e attiva");
                            SessionRefreshed?.Invoke(saved, profile);
                            _timer.Start();
                            return true;
                        }
                    }
                    catch (OperationCanceledException) when (!ct.IsCancellationRequested)
                    {
                        LauncherLogService.Instance.Warn("Session fast-check timed out; attempting proactive refresh.");
                    }
                    catch (HttpRequestException ex) when (ex.StatusCode == System.Net.HttpStatusCode.Unauthorized)
                    {
                        LauncherLogService.Instance.Warn("Token returned 401 Unauthorized; attempting proactive refresh.");
                    }
                    catch (Exception ex) when (IsNetworkFailure(ex))
                    {
                        LauncherLogService.Instance.Warn("Network unreachable during validation; falling back to offline mode.");
                        LastSuccessfulSync = DateTime.Now;
                        SetState(SessionState.OfflineReady, "Modalità offline (token valido)");
                        return true;
                    }
                }

                // If token expires soon (<= 4h) or fast-check needed a refresh:
                return await RefreshInternalAsync(ct);
            }
            catch (Exception ex)
            {
                LauncherLogService.Instance.Error($"Startup session validation error: {ex.Message}");
                if (_account != null && _account.TokenExpiry > DateTime.UtcNow && IsNetworkFailure(ex))
                {
                    SetState(SessionState.OfflineReady, "Modalità offline");
                    return true;
                }

                SetState(SessionState.Expired, "Sessione scaduta");
                RefreshFailed?.Invoke(ex);
                return false;
            }
        }

        public async Task<bool> EnsureFreshSessionAsync(CancellationToken ct = default)
        {
            if (_account == null)
            {
                _account = _auth.LoadSavedAccount();
                if (_account == null)
                    return false;
            }

            if (State == SessionState.Ready &&
                _account.TokenExpiry > DateTime.UtcNow.AddHours(2) &&
                !string.IsNullOrEmpty(_account.MinecraftToken))
            {
                LastSuccessfulSync ??= DateTime.Now;
                return true;
            }

            return await RefreshInternalAsync(ct);
        }

        private async Task RefreshIfNeededAsync()
        {
            if (_account == null || _refreshing)
                return;

            if (_account.TokenExpiry > DateTime.UtcNow.AddHours(3))
                return;

            await RefreshInternalAsync(CancellationToken.None);
        }

        private async Task<bool> RefreshInternalAsync(CancellationToken ct)
        {
            if (_account == null)
            {
                SetState(SessionState.NotLoggedIn, "Nessun account");
                return false;
            }

            if (string.IsNullOrEmpty(_account.MsaRefreshToken))
            {
                SetState(SessionState.Expired, "Riconnessione necessaria");
                RefreshFailed?.Invoke(new InvalidOperationException("Nessun refresh token disponibile."));
                return false;
            }

            _refreshing = true;
            SetState(SessionState.Validating, "Rinnovo token Microsoft in corso…");

            try
            {
                StatusChanged?.Invoke("Syncing session…");
                var (profile, updated) = await _auth.RefreshAccountAsync(
                    _account,
                    msg =>
                    {
                        StatusText = msg;
                        StatusChanged?.Invoke(msg);
                    },
                    ct);

                _account = updated;
                LastSuccessfulSync = DateTime.Now;
                SetState(SessionState.Ready, "Sessione attiva");
                SessionRefreshed?.Invoke(updated, profile);
                StatusChanged?.Invoke("Session active");
                _timer.Start();
                return true;
            }
            catch (Exception ex)
            {
                LauncherLogService.Instance.Error($"Session refresh failed: {ex.Message}");
                if (_account != null && _account.TokenExpiry > DateTime.UtcNow && IsNetworkFailure(ex))
                {
                    SetState(SessionState.OfflineReady, "Modalità offline");
                    return true;
                }

                SetState(SessionState.Expired, "Sessione scaduta");
                RefreshFailed?.Invoke(ex);
                StatusChanged?.Invoke("Waiting to reconnect");
                return false;
            }
            finally
            {
                _refreshing = false;
            }
        }

        private void SetState(SessionState newState, string status)
        {
            State = newState;
            StatusText = status;
            StateChanged?.Invoke(newState, status);
        }

        private static bool IsNetworkFailure(Exception ex)
        {
            return ex is HttpRequestException or SocketException or TimeoutException;
        }

        public void Dispose() => Stop();
    }
}
