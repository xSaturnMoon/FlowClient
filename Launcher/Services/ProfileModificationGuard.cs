using System;
using System.Net;
using System.Threading;
using System.Threading.Tasks;

namespace Launcher.Services
{
    /// <summary>
    /// Mojang rate-limits skin/cape changes (HTTP 423/429). Serialize and cool down profile edits.
    /// </summary>
    public static class ProfileModificationGuard
    {
        private static readonly SemaphoreSlim Mutex = new(1, 1);
        private static DateTime _cooldownUntilUtc = DateTime.MinValue;

        public static bool IsCoolingDown => DateTime.UtcNow < _cooldownUntilUtc;

        public static TimeSpan Remaining =>
            IsCoolingDown ? _cooldownUntilUtc - DateTime.UtcNow : TimeSpan.Zero;

        public static void BeginCooldown(TimeSpan duration)
        {
            var until = DateTime.UtcNow.Add(duration);
            if (until > _cooldownUntilUtc)
                _cooldownUntilUtc = until;
        }

        public static string FormatRemaining()
        {
            var seconds = (int)Math.Ceiling(Remaining.TotalSeconds);
            if (seconds <= 0) return string.Empty;
            if (seconds < 60) return $"Attendi {seconds}s — Mojang limita i cambi profilo.";
            return $"Attendi {seconds / 60}m {seconds % 60}s — Mojang limita i cambi profilo.";
        }

        public static async Task<T> ExecuteAsync<T>(Func<Task<T>> action, CancellationToken ct = default)
        {
            await Mutex.WaitAsync(ct);
            try
            {
                if (IsCoolingDown)
                    throw new ProfileModificationException(FormatRemaining());

                try
                {
                    var result = await action();
                    BeginCooldown(TimeSpan.FromSeconds(30));
                    return result;
                }
                catch (ProfileModificationException)
                {
                    throw;
                }
                catch (ProfileApiException api)
                {
                    if (api.StatusCode is HttpStatusCode.Locked or HttpStatusCode.TooManyRequests)
                    {
                        BeginCooldown(api.StatusCode == HttpStatusCode.TooManyRequests
                            ? TimeSpan.FromSeconds(90)
                            : TimeSpan.FromSeconds(60));
                    }
                    throw;
                }
            }
            finally
            {
                Mutex.Release();
            }
        }
    }

    public sealed class ProfileApiException : Exception
    {
        public HttpStatusCode StatusCode { get; }

        public ProfileApiException(HttpStatusCode statusCode, string message)
            : base(message)
        {
            StatusCode = statusCode;
        }
    }

    public sealed class ProfileModificationException : Exception
    {
        public ProfileModificationException(string message) : base(message) { }
    }
}
