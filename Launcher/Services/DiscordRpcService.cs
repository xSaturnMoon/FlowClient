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
        private bool _isConnected;
        private bool _disposed;
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
            var details = string.IsNullOrWhiteSpace(versionSelected)
                ? "Nel Launcher"
                : $"Versione: {versionSelected}";

            UpdateActivity(
                details: details,
                state: "Menu Principale",
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
            var details = $"In gioco su {instanceName}";
            var state = !string.IsNullOrWhiteSpace(server)
                ? $"Server: {server}"
                : (!string.IsNullOrWhiteSpace(ver) ? $"{ldr} {ver}" : ldr);

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

                        object? assets = (_largeImageKey != null || _smallImageKey != null)
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
                    await Task.Delay(5000, ct);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
            }
        }

        private void TryConnect()
        {
            for (var i = 0; i < 10; i++)
            {
                var pipeName = $"discord-ipc-{i}";
                try
                {
                    var pipe = new NamedPipeClientStream(".", pipeName, PipeDirection.InOut, PipeOptions.None);
                    pipe.Connect(1000);

                    _pipe = pipe;

                    // Handshake
                    var handshake = JsonSerializer.Serialize(new
                    {
                        v = 1,
                        client_id = DefaultClientId
                    });

                    WriteFrame(0, handshake);

                    // Read handshake response
                    var (op, resp) = ReadFrame();
                    if (op == 1 && resp.Contains("READY"))
                    {
                        _isConnected = true;
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

            _isConnected = false;
        }

        private void WriteFrame(int opcode, string json)
        {
            if (_pipe == null || !_pipe.IsConnected) return;

            var bytes = Encoding.UTF8.GetBytes(json);
            var buffer = new byte[8 + bytes.Length];

            BitConverter.GetBytes(opcode).CopyTo(buffer, 0);
            BitConverter.GetBytes(bytes.Length).CopyTo(buffer, 4);
            bytes.CopyTo(buffer, 8);

            _pipe.Write(buffer, 0, buffer.Length);
            _pipe.Flush();
        }

        private (int Opcode, string Json) ReadFrame()
        {
            if (_pipe == null || !_pipe.IsConnected) return (-1, "");

            var header = new byte[8];
            var bytesRead = _pipe.Read(header, 0, 8);
            if (bytesRead < 8) return (-1, "");

            var opcode = BitConverter.ToInt32(header, 0);
            var length = BitConverter.ToInt32(header, 4);

            var data = new byte[length];
            var totalRead = 0;
            while (totalRead < length)
            {
                var read = _pipe.Read(data, totalRead, length - totalRead);
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
