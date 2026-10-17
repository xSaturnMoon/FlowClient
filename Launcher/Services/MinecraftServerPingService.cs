using System;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Launcher.Services
{
    public sealed class ServerPingResult
    {
        public bool Success { get; init; }
        public string Host { get; init; } = "";
        public int Port { get; init; } = 25565;
        public string? Motd { get; init; }
        public int OnlinePlayers { get; init; }
        public int MaxPlayers { get; init; }
        public string? Error { get; init; }
    }

    public static class MinecraftServerPingService
    {
        public static (string Host, int Port) ParseAddress(string address)
        {
            if (string.IsNullOrWhiteSpace(address))
                return ("localhost", 25565);

            address = address.Trim();
            var colonIndex = address.IndexOf(':');
            if (colonIndex > 0)
            {
                var host = address[..colonIndex].Trim();
                var portStr = address[(colonIndex + 1)..].Trim();
                if (int.TryParse(portStr, out var port) && port is > 0 and <= 65535)
                {
                    return (host, port);
                }
                return (host, 25565);
            }

            return (address, 25565);
        }

        /// <summary>
        /// Pings the Minecraft server asynchronously. Checks TCP reachability and performs
        /// standard Minecraft status handshake if possible to verify the server exists.
        /// </summary>
        public static async Task<ServerPingResult> PingAsync(string address, int timeoutMs = 3000)
        {
            var (host, port) = ParseAddress(address);

            try
            {
                using var cts = new CancellationTokenSource(timeoutMs);

                // 1. Resolve host
                var addresses = await Dns.GetHostAddressesAsync(host, cts.Token);
                if (addresses.Length == 0)
                {
                    return new ServerPingResult
                    {
                        Success = false,
                        Host = host,
                        Port = port,
                        Error = "Could not resolve server address."
                    };
                }

                // 2. Attempt TCP socket connection
                using var tcp = new TcpClient();
                await tcp.ConnectAsync(host, port, cts.Token);

                if (!tcp.Connected)
                {
                    return new ServerPingResult
                    {
                        Success = false,
                        Host = host,
                        Port = port,
                        Error = "Connection refused or server is offline."
                    };
                }

                // 3. Perform Minecraft 1.7+ handshake to retrieve MOTD and player count
                try
                {
                    tcp.ReceiveTimeout = timeoutMs;
                    tcp.SendTimeout = timeoutMs;
                    var stream = tcp.GetStream();

                    // Handshake packet (ID: 0x00, protocol: -1, host, port, state: 1)
                    using var ms = new MemoryStream();
                    WriteVarInt(ms, 0x00); // Packet ID
                    WriteVarInt(ms, 47);   // Protocol version (1.8 fallback)
                    WriteString(ms, host); // Host
                    ms.Write(BitConverter.GetBytes((ushort)IPAddress.HostToNetworkOrder((short)port))); // Port
                    WriteVarInt(ms, 1);    // Next state: 1 (status)

                    var handshakeData = ms.ToArray();
                    WriteVarInt(stream, handshakeData.Length);
                    await stream.WriteAsync(handshakeData, cts.Token);

                    // Status request packet (length: 1, ID: 0x00)
                    WriteVarInt(stream, 1);
                    stream.WriteByte(0x00);
                    await stream.FlushAsync(cts.Token);

                    // Read response
                    var packetLength = ReadVarInt(stream);
                    var packetId = ReadVarInt(stream);

                    if (packetId == 0x00)
                    {
                        var jsonLength = ReadVarInt(stream);
                        var buffer = new byte[jsonLength];
                        var read = 0;
                        while (read < jsonLength)
                        {
                            var r = await stream.ReadAsync(buffer.AsMemory(read, jsonLength - read), cts.Token);
                            if (r <= 0) break;
                            read += r;
                        }

                        var json = Encoding.UTF8.GetString(buffer, 0, read);
                        using var doc = JsonDocument.Parse(json);
                        var root = doc.RootElement;

                        var motd = "";
                        if (root.TryGetProperty("description", out var desc))
                        {
                            if (desc.ValueKind == JsonValueKind.String)
                                motd = desc.GetString();
                            else if (desc.TryGetProperty("text", out var textProp))
                                motd = textProp.GetString();
                        }

                        int online = 0, max = 0;
                        if (root.TryGetProperty("players", out var players))
                        {
                            if (players.TryGetProperty("online", out var onProp))
                                online = onProp.GetInt32();
                            if (players.TryGetProperty("max", out var maxProp))
                                max = maxProp.GetInt32();
                        }

                        return new ServerPingResult
                        {
                            Success = true,
                            Host = host,
                            Port = port,
                            Motd = StripMinecraftColorCodes(motd),
                            OnlinePlayers = online,
                            MaxPlayers = max
                        };
                    }
                }
                catch
                {
                    // If handshake fails (e.g. BungeeCord firewall, custom anti-bot),
                    // the TCP connect still succeeded so the server DOES exist!
                }

                return new ServerPingResult
                {
                    Success = true,
                    Host = host,
                    Port = port
                };
            }
            catch (OperationCanceledException)
            {
                return new ServerPingResult
                {
                    Success = false,
                    Host = host,
                    Port = port,
                    Error = "Server timed out (offline or unreachable)."
                };
            }
            catch (Exception ex)
            {
                return new ServerPingResult
                {
                    Success = false,
                    Host = host,
                    Port = port,
                    Error = ex.Message
                };
            }
        }

        /// <summary>
        /// Pre-warms Windows DNS cache and network sockets so that when Minecraft starts,
        /// connecting to the server does not suffer from DNS resolution or IPv6 probe latency.
        /// </summary>
        public static void PrewarmDnsAndRouteAsync(string host, int port)
        {
            Task.Run(async () =>
            {
                try
                {
                    _ = await Dns.GetHostAddressesAsync(host);
                    using var tcp = new TcpClient();
                    using var cts = new CancellationTokenSource(1500);
                    await tcp.ConnectAsync(host, port, cts.Token);
                }
                catch
                {
                    // Silent background pre-warm
                }
            });
        }

        private static void WriteVarInt(Stream stream, int value)
        {
            while ((value & -128) != 0)
            {
                stream.WriteByte((byte)(value & 127 | 128));
                value = (int)((uint)value >> 7);
            }
            stream.WriteByte((byte)value);
        }

        private static int ReadVarInt(Stream stream)
        {
            int numRead = 0;
            int result = 0;
            byte read;
            do
            {
                var val = stream.ReadByte();
                if (val == -1) throw new EndOfStreamException();
                read = (byte)val;
                int value = read & 0b0111_1111;
                result |= value << 7 * numRead;
                numRead++;
                if (numRead > 5) throw new InvalidOperationException("VarInt is too big");
            } while ((read & 0b1000_0000) != 0);

            return result;
        }

        private static void WriteString(Stream stream, string text)
        {
            var bytes = Encoding.UTF8.GetBytes(text);
            WriteVarInt(stream, bytes.Length);
            stream.Write(bytes, 0, bytes.Length);
        }

        private static string? StripMinecraftColorCodes(string? text)
        {
            if (string.IsNullOrEmpty(text)) return text;
            var sb = new StringBuilder();
            for (int i = 0; i < text.Length; i++)
            {
                if (text[i] == '§' && i + 1 < text.Length)
                {
                    i++; // skip formatting code
                    continue;
                }
                sb.Append(text[i]);
            }
            return sb.ToString().Trim();
        }
    }
}
