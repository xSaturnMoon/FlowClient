using System;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Threading;

namespace Launcher.Services
{
    /// <summary>
    /// Keeps the Microsoft / Minecraft session alive with proactive token refresh.
    /// </summary>
    public sealed class SessionKeepAliveService : IDisposable
    {
        private readonly MinecraftAuthService _auth = new();
        private readonly DispatcherTimer _timer;
        private SavedAccount? _account;
        private bool _refreshing;

        public event Action<SavedAccount, MinecraftProfile>? SessionRefreshed;
        public event Action<string>? StatusChanged;
        public event Action<Exception>? RefreshFailed;

        public DateTime? LastSuccessfulSync { get; private set; }

        public SessionKeepAliveService()
        {
            _timer = new DispatcherTimer { Interval = TimeSpan.FromMinutes(20) };
            _timer.Tick += async (_, _) => await RefreshIfNeededAsync();
        }

        public void Start(SavedAccount account)
        {
            _account = account;
            _timer.Start();
        }

        public void Stop()
        {
            _timer.Stop();
            _account = null;
        }

        public async Task<bool> EnsureFreshSessionAsync(CancellationToken ct = default)
        {
            if (_account == null)
                return false;

            if (_account.TokenExpiry > DateTime.UtcNow.AddHours(2) &&
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
                return false;

            if (string.IsNullOrEmpty(_account.MsaRefreshToken))
            {
                RefreshFailed?.Invoke(new InvalidOperationException("Nessun refresh token disponibile."));
                return false;
            }

            _refreshing = true;
            try
            {
                StatusChanged?.Invoke("Syncing session…");
                var (profile, updated) = await _auth.RefreshAccountAsync(
                    _account,
                    msg => StatusChanged?.Invoke(msg),
                    ct);

                _account = updated;
                LastSuccessfulSync = DateTime.Now;
                SessionRefreshed?.Invoke(updated, profile);
                StatusChanged?.Invoke("Session active");
                return true;
            }
            catch (Exception ex)
            {
                RefreshFailed?.Invoke(ex);
                StatusChanged?.Invoke("Waiting to reconnect");
                return false;
            }
            finally
            {
                _refreshing = false;
            }
        }

        public void Dispose() => Stop();
    }
}
