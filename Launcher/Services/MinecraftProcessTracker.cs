using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace Launcher.Services
{
    public sealed class LaunchMetadata
    {
        public string Version { get; init; } = "—";
        public string Loader { get; init; } = "—";
        public string? Server { get; init; }
        public int RamMb { get; init; }
    }

    public sealed class MinecraftProcessTracker
    {
        private static readonly Lazy<MinecraftProcessTracker> _instance =
            new(() => new MinecraftProcessTracker());

        public static MinecraftProcessTracker Instance => _instance.Value;

        private readonly MinecraftSessionService _sessions = new();
        private readonly object _lock = new();

        private Process? _process;
        private MinecraftSessionRecord? _session;
        private MinecraftJobObject? _job;
        private HashSet<int> _launchJavaPids = new();
        private bool _gameWasActive;
        private LaunchMetadata? _launchMetadata;
        private int _generation;
        private CancellationTokenSource? _monitorCts;

        private MinecraftProcessTracker() { }

        public bool IsRunning
        {
            get
            {
                lock (_lock)
                {
                    if (_session == null)
                        return false;

                    if (_process != null)
                    {
                        try
                        {
                            if (!_process.HasExited)
                                return true;
                        }
                        catch
                        {
                            // Stale handle — fall through to PID scan.
                        }
                    }

                    return ProcessKiller.AnyJavaAliveSince(_session.StartedAtUtc);
                }
            }
        }

        private bool _isLaunching;
        private string? _launchStatusMessage;

        public bool IsLaunching
        {
            get { lock (_lock) return _isLaunching; }
        }

        public string? LaunchStatusMessage
        {
            get { lock (_lock) return _launchStatusMessage; }
        }

        public Process? CurrentProcess
        {
            get { lock (_lock) return _process; }
        }

        public LaunchMetadata? CurrentLaunchMetadata
        {
            get { lock (_lock) return _launchMetadata; }
        }

        public event Action? ProcessExited;
        public event Action? ProcessStarted;
        public event Action? LaunchStateChanged;

        public void SetLaunching(bool isLaunching, string? message = null)
        {
            lock (_lock)
            {
                _isLaunching = isLaunching;
                _launchStatusMessage = message;
            }
            LaunchStateChanged?.Invoke();
        }

        public void SetLaunchMetadata(string version, string loader, int ramMb, string? server = null)
        {
            lock (_lock)
            {
                _launchMetadata = new LaunchMetadata
                {
                    Version = string.IsNullOrWhiteSpace(version) ? "—" : version.Trim(),
                    Loader = string.IsNullOrWhiteSpace(loader) ? "—" : loader.Trim(),
                    RamMb = ramMb,
                    Server = string.IsNullOrWhiteSpace(server) ? null : server.Trim()
                };
            }
        }

        public void SetProcess(Process process, string instanceId, string gameDirectory, HashSet<int> beforeJavaPids)
        {
            StopMonitor();

            var session = _sessions.BeginSession(instanceId, gameDirectory, process.Id);

            MinecraftJobObject? job = null;
            try
            {
                job = new MinecraftJobObject();
                job.Assign(process);
            }
            catch
            {
                job?.Dispose();
                job = null;
            }

            int generation;
            lock (_lock)
            {
                _process = process;
                _session = session;
                _job = job;
                _launchJavaPids = new HashSet<int>(beforeJavaPids);
                _gameWasActive = false;
                generation = ++_generation;
            }

            process.EnableRaisingEvents = true;
            process.Exited += (_, _) => TryFinishSession(generation);
            ProcessStarted?.Invoke();
            LauncherLogService.Instance.Info($"Tracking Minecraft session ({instanceId}).");

            _monitorCts = new CancellationTokenSource();
            _ = Task.Run(() => MonitorAsync(generation, _monitorCts.Token), _monitorCts.Token);
        }

        public void KillProcess() => TryFinishSession(GetGeneration());

        /// <summary>
        /// Stops tracking the running game without terminating it. Used when the launcher UI closes.
        /// </summary>
        public void ReleaseRunningGame()
        {
            MinecraftJobObject? job;
            lock (_lock)
            {
                job = _job;
                _job = null;
                _process = null;
            }

            StopMonitor();

            if (job != null)
            {
                try { job.Dispose(); } catch { /* ignore */ }
            }
        }

        public void CleanupStaleSessions() => _sessions.CleanupStaleSessions();

        private int GetGeneration()
        {
            lock (_lock) return _generation;
        }

        private async Task MonitorAsync(int generation, CancellationToken ct)
        {
            while (!ct.IsCancellationRequested)
            {
                try
                {
                    await Task.Delay(TimeSpan.FromMilliseconds(500), ct);
                }
                catch (OperationCanceledException)
                {
                    return;
                }

                MinecraftSessionRecord? session;
                Process? process;
                lock (_lock)
                {
                    if (_generation != generation)
                        return;
                    session = _session;
                    process = _process;
                }

                if (session == null)
                    return;

                RefreshLaunchJavaPids(session);
                UpdateGameActive(process, session);

                if (ShouldForceKill(process))
                    ForceTerminateAll(session);

                if (!ProcessKiller.AnyJavaAliveSince(session.StartedAtUtc))
                {
                    CompleteExit(generation);
                    return;
                }
            }
        }

        private void UpdateGameActive(Process? process, MinecraftSessionRecord session)
        {
            if (_gameWasActive || process == null)
                return;

            try
            {
                if (process.HasExited)
                {
                    _gameWasActive = true;
                    return;
                }

                if (process.WorkingSet64 > 150_000_000 &&
                    DateTime.UtcNow - session.StartedAtUtc > TimeSpan.FromSeconds(15))
                {
                    _gameWasActive = true;
                }
            }
            catch
            {
                // Ignore stale handles.
            }
        }

        private bool ShouldForceKill(Process? process)
        {
            if (!_gameWasActive || process == null)
                return false;

            try
            {
                return process.HasExited;
            }
            catch
            {
                return true;
            }
        }

        private void TryFinishSession(int generation)
        {
            MinecraftSessionRecord? session;
            lock (_lock)
            {
                if (_generation != generation)
                    return;
                session = _session;
            }

            if (session == null)
            {
                CompleteExit(generation);
                return;
            }

            ForceTerminateAll(session);

            for (var attempt = 0; attempt < 20; attempt++)
            {
                if (!ProcessKiller.AnyJavaAliveSince(session.StartedAtUtc))
                {
                    CompleteExit(generation);
                    return;
                }

                ProcessKiller.KillAllJavaSince(session.StartedAtUtc);
                Thread.Sleep(250);
            }

            // Last resort — still try to update UI only if truly dead.
            if (!ProcessKiller.AnyJavaAliveSince(session.StartedAtUtc))
                CompleteExit(generation);
        }

        private void RefreshLaunchJavaPids(MinecraftSessionRecord session)
        {
            HashSet<int> before;
            lock (_lock) { before = _launchJavaPids; }

            var current = MinecraftSessionService.SnapshotJavaPids();
            var newPids = current.Where(pid => !before.Contains(pid)).ToList();
            _sessions.AddPids(session, newPids);

            var fromGame = _sessions.LoadFromGameDirectory(session.GameDirectory);
            if (fromGame != null)
                _sessions.AddPids(session, fromGame.Pids);
        }

        private void ForceTerminateAll(MinecraftSessionRecord session)
        {
            Process? process;
            MinecraftJobObject? job;
            lock (_lock)
            {
                process = _process;
                job = _job;
            }

            try { job?.Terminate(); } catch { /* ignore */ }

            foreach (var pid in session.Pids.Distinct())
                ProcessKiller.ForceKillTree(pid);

            ProcessKiller.ForceKillTree(session.RootPid);

            if (process != null)
            {
                try
                {
                    if (!process.HasExited)
                        ProcessKiller.ForceKillTree(process.Id);
                }
                catch
                {
                    // Ignore stale process handles.
                }
            }

            ProcessKiller.KillAllJavaSince(session.StartedAtUtc);
            MinecraftJavaProcessResolver.KillForLaunch(session.RootPid, session.GameDirectory);
            ProcessKiller.KillLikelyMinecraftOrphans();
        }

        private void CompleteExit(int generation)
        {
            Process? process;
            MinecraftSessionRecord? session;
            MinecraftJobObject? job;
            LaunchMetadata? launchMetadata;
            bool shouldNotify;
            lock (_lock)
            {
                if (_generation != generation)
                    return;

                process = _process;
                session = _session;
                job = _job;
                launchMetadata = _launchMetadata;
                _launchMetadata = null;
                _process = null;
                _session = null;
                _job = null;
                _launchJavaPids.Clear();
                _gameWasActive = false;
                shouldNotify = true;
            }

            try { job?.Terminate(); } catch { /* ignore */ }
            job?.Dispose();

            if (session != null)
            {
                RecordCompletedSession(session, launchMetadata);
                _sessions.EndSession(session);
            }

            StopMonitor();
            try { process?.Dispose(); } catch { /* ignore */ }

            if (shouldNotify)
            {
                if (session != null)
                {
                    var duration = DateTime.UtcNow - session.StartedAtUtc;
                    LauncherLogService.Instance.Info(
                        $"Minecraft session ended ({FormatDuration(duration)}).");
                }
                ProcessExited?.Invoke();
            }
        }

        private static string FormatDuration(TimeSpan duration)
        {
            if (duration.TotalHours >= 1)
                return $"{duration.TotalHours:0.#}h";
            if (duration.TotalMinutes >= 1)
                return $"{duration.TotalMinutes:0}m";
            return $"{duration.TotalSeconds:0}s";
        }

        private static void RecordCompletedSession(MinecraftSessionRecord session, LaunchMetadata? metadata)
        {
            var endUtc = DateTime.UtcNow;
            var duration = endUtc - session.StartedAtUtc;
            if (duration < TimeSpan.FromMinutes(1))
                return;

            try
            {
                new PlayTimeService().RecordSession(duration);
                new AccountActivityService().RecordSession(new GameSessionRecord
                {
                    Start = session.StartedAtUtc.ToLocalTime(),
                    End = endUtc.ToLocalTime(),
                    Hours = duration.TotalHours,
                    Version = metadata?.Version ?? "—",
                    Loader = metadata?.Loader ?? "—",
                    Server = metadata?.Server ?? "—",
                    RamMb = metadata?.RamMb > 0 ? metadata.RamMb : null
                });
            }
            catch
            {
                // Stats should never block session cleanup.
            }
        }

        private void StopMonitor()
        {
            var cts = _monitorCts;
            _monitorCts = null;
            if (cts == null) return;
            try { cts.Cancel(); } catch { /* ignore */ }
            cts.Dispose();
        }
    }
}
