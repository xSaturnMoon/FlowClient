using System;
using System.Diagnostics;
using System.IO;
using System.IO.Pipes;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Launcher.Services
{
    public sealed class DiscordRpcService : IDisposable
    {
        private static readonly Lazy<DiscordRpcService> LazyInstance = new(() => new DiscordRpcService());
        public static DiscordRpcService Instance => LazyInstance.Value;

        // Discord application client ID for Minecraft / Flow Client
        private const string DefaultClientId = "1068228394468659220";

        private readonly object _lock = new();
        private NamedPipeClientStream? _pipe;
        private CancellationTokenSource? _cts;
        private CancellationTokenSource? _readCts;
        private bool _isConnected;
        private bool _disposed;
        private bool _skipAssets;
        private long _launcherStartTime;

        private string? _currentDetails;
        private string? _currentState;
        private long? _currentTimestamp;
        private string? _largeImageKey;
        private string? _largeImageText;
        private string? _smallImageKey;
        private string? _smallImageText;

        private DiscordRpcService()
        {
            _launcherStartTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        }

        public bool IsConnected
        {
            get { lock (_lock) return _isConnected; }
        }

        public event Action<bool>? ConnectionChanged;

        public void Start()
        {
            lock (_lock)
            {
                if (_cts != null) return;
                _cts = new CancellationTokenSource();
            }

            Task.Run(() => WorkerLoopAsync(_cts.Token));
        }

        public void Stop()
        {
            lock (_lock)
            {
                _cts?.Cancel();
                _cts = null;
                ClosePipe();
            }
        }

        public void SetPresenceInLauncher(string? versionSelected = null)
        {
            var details = "Flow Client";
            var state = string.IsNullOrWhiteSpace(versionSelected)
                ? "Nel Launcher"
                : $"Nel Launcher • {versionSelected}";

            UpdateActivity(
                details: details,
                state: state,
                startTimestamp: _launcherStartTime,
                largeImage: "flow_logo",
                largeText: "Flow Client",
                smallImage: "minecraft",
                smallText: "Minecraft"
            );
        }

        public void SetPresenceInGame(string instanceName, string? loader = null, string? version = null, string? server = null)
        {
            var ldr = string.IsNullOrWhiteSpace(loader) ? "Minecraft" : loader.Trim();
            var ver = string.IsNullOrWhiteSpace(version) ? "" : version.Trim();
            var details = !string.IsNullOrWhiteSpace(server)
                ? $"Giocando su {server}"
                : $"In gioco: {instanceName}";
            var state = !string.IsNullOrWhiteSpace(ver)
                ? $"Flow Client • {ldr} {ver}"
                : $"Flow Client • {ldr}";

            var loaderIcon = ldr.ToLowerInvariant() switch
            {
                "fabric" => "fabric",
                "neoforge" => "neoforge",
                "forge" => "forge",
                "quilt" => "quilt",
                _ => "minecraft"
            };

            UpdateActivity(
                details: details,
                state: state,
                startTimestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                largeImage: "flow_logo",
                largeText: "Flow Client",
                smallImage: loaderIcon,
                smallText: !string.IsNullOrWhiteSpace(ver) ? $"{ldr} {ver}" : ldr
            );
        }

        public void UpdateActivity(
            string details,
            string state,
            long? startTimestamp = null,
            string? largeImage = null,
            string? largeText = null,
            string? smallImage = null,
            string? smallText = null)
        {
            lock (_lock)
            {
                _currentDetails = details;
                _currentState = state;
                _currentTimestamp = startTimestamp;
                _largeImageKey = largeImage;
                _largeImageText = largeText;
                _smallImageKey = smallImage;
                _smallImageText = smallText;
            }

            SendCurrentPresence();
        }

        private void SendCurrentPresence()
        {
            lock (_lock)
            {
                if (!_isConnected || _pipe == null || !_pipe.IsConnected)
                    return;

                try
                {
                    var pid = Environment.ProcessId;
                    var nonce = Guid.NewGuid().ToString("N");

                    object activityObj;
                    if (_currentDetails == null && _currentState == null)
                    {
                        activityObj = null!;
                    }
                    else
                    {
                        object? timestamps = _currentTimestamp.HasValue
                            ? new { start = _currentTimestamp.Value }
                            : null;

                        object? assets = (!_skipAssets && (_largeImageKey != null || _smallImageKey != null))
                            ? new
                            {
                                large_image = _largeImageKey,
                                large_text = _largeImageText,
                                small_image = _smallImageKey,
                                small_text = _smallImageText
                            }
                            : null;

                        activityObj = new
                        {
                            details = _currentDetails,
                            state = _currentState,
                            timestamps = timestamps,
                            assets = assets
                        };
                    }

                    var payload = new
                    {
                        cmd = "SET_ACTIVITY",
                        args = new
                        {
                            pid = pid,
                            activity = activityObj
                        },
                        nonce = nonce
                    };

                    var json = JsonSerializer.Serialize(payload);
                    WriteFrame(1, json);
                }
                catch
                {
                    ClosePipe();
                }
            }
        }

        private async Task WorkerLoopAsync(CancellationToken ct)
        {
            while (!ct.IsCancellationRequested)
            {
                if (!LauncherSettingsService.Instance.Current.EnableDiscordRpc)
                {
                    ClosePipe();
                    await Task.Delay(3000, ct);
                    continue;
                }

                if (!_isConnected)
                {
                    TryConnect();
                    if (_isConnected)
                    {
                        SendCurrentPresence();
                    }
                }

                // Check pipe health
                if (_isConnected && _pipe != null)
                {
                    if (!_pipe.IsConnected)
                    {
                        ClosePipe();
                    }
                }

                try
                {
                    await Task.Delay(4000, ct);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
            }
        }

        private void TryConnect()
        {
            var clientId = DefaultClientId;

            for (var i = 0; i < 10; i++)
            {
                var pipeName = $"discord-ipc-{i}";
                try
                {
                    var pipe = new NamedPipeClientStream(".", pipeName, PipeDirection.InOut, PipeOptions.Asynchronous);
                    pipe.Connect(80);

                    lock (_lock)
                    {
                        _pipe = pipe;
                    }

                    // Handshake
                    var handshake = JsonSerializer.Serialize(new
                    {
                        v = 1,
                        client_id = clientId
                    });

                    WriteFrame(0, handshake);

                    // Read handshake response
                    var (op, resp) = ReadFrameSync(pipe);
                    if (op == 1 && resp.Contains("READY"))
                    {
                        lock (_lock)
                        {
                            _isConnected = true;
                            _readCts = new CancellationTokenSource();
                        }

                        // Start continuous read pump to handle PING (opcode 3) and PONG (opcode 4)
                        _ = Task.Run(() => ReadPumpAsync(pipe, _readCts.Token));

                        ConnectionChanged?.Invoke(true);
                        return;
                    }

                    pipe.Dispose();
                }
                catch
                {
                    // Continue scanning pipes
                }
            }

            lock (_lock)
            {
                _isConnected = false;
            }
        }

        private async Task ReadPumpAsync(NamedPipeClientStream pipe, CancellationToken ct)
        {
            var header = new byte[8];
            while (!ct.IsCancellationRequested && pipe.IsConnected)
            {
                try
                {
                    var headerRead = 0;
                    while (headerRead < 8)
                    {
                        var read = await pipe.ReadAsync(header.AsMemory(headerRead, 8 - headerRead), ct);
                        if (read <= 0) return;
                        headerRead += read;
                    }

                    var opcode = BitConverter.ToInt32(header, 0);
                    var length = BitConverter.ToInt32(header, 4);
                    if (length < 0 || length > 65536) return;

                    var data = new byte[length];
                    var totalRead = 0;
                    while (totalRead < length)
                    {
                        var read = await pipe.ReadAsync(data.AsMemory(totalRead, length - totalRead), ct);
                        if (read <= 0) return;
                        totalRead += read;
                    }

                    var json = Encoding.UTF8.GetString(data);

                    // Opcode 3 = PING from Discord: MUST reply with Opcode 4 (PONG) to keep alive!
                    if (opcode == 3)
                    {
                        WriteFrame(4, json);
                    }
                    // Opcode 2 = CLOSE
                    else if (opcode == 2)
                    {
                        break;
                    }
                    // Opcode 1 = FRAME
                    else if (opcode == 1)
                    {
                        // Check if Discord rejected activity due to invalid custom assets (code 4000)
                        if (json.Contains("\"evt\":\"ERROR\"") || json.Contains("\"code\":4000"))
                        {
                            lock (_lock)
                            {
                                if (!_skipAssets)
                                {
                                    _skipAssets = true;
                                    Task.Run(SendCurrentPresence);
                                }
                            }
                        }
                    }
                }
                catch
                {
                    break;
                }
            }

            ClosePipe();
        }

        private void WriteFrame(int opcode, string json)
        {
            lock (_lock)
            {
                if (_pipe == null || !_pipe.IsConnected) return;

                try
                {
                    var bytes = Encoding.UTF8.GetBytes(json);
                    var buffer = new byte[8 + bytes.Length];

                    BitConverter.GetBytes(opcode).CopyTo(buffer, 0);
                    BitConverter.GetBytes(bytes.Length).CopyTo(buffer, 4);
                    bytes.CopyTo(buffer, 8);

                    _pipe.Write(buffer, 0, buffer.Length);
                    _pipe.Flush();
                }
                catch
                {
                    ClosePipe();
                }
            }
        }

        private static (int Opcode, string Json) ReadFrameSync(NamedPipeClientStream pipe)
        {
            if (!pipe.IsConnected) return (-1, "");

            var header = new byte[8];
            var headerRead = 0;
            while (headerRead < 8)
            {
                var r = pipe.Read(header, headerRead, 8 - headerRead);
                if (r <= 0) return (-1, "");
                headerRead += r;
            }

            var opcode = BitConverter.ToInt32(header, 0);
            var length = BitConverter.ToInt32(header, 4);
            if (length < 0 || length > 65536) return (-1, "");

            var data = new byte[length];
            var totalRead = 0;
            while (totalRead < length)
            {
                var read = pipe.Read(data, totalRead, length - totalRead);
                if (read <= 0) break;
                totalRead += read;
            }

            var json = Encoding.UTF8.GetString(data, 0, totalRead);
            return (opcode, json);
        }

        private void ClosePipe()
        {
            var wasConnected = false;
            lock (_lock)
            {
                wasConnected = _isConnected;
                _isConnected = false;
                try { _readCts?.Cancel(); } catch { }
                _readCts = null;
                try { _pipe?.Dispose(); } catch { }
                _pipe = null;
            }
            if (wasConnected)
            {
                ConnectionChanged?.Invoke(false);
            }
        }

        public void Dispose()
        {
            if (_disposed) return;
            _disposed = true;
            Stop();
        }
    }
}
