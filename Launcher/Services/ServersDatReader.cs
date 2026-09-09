using System.IO;
using System.IO.Compression;
using System.Text;

namespace Launcher.Services
{
    public sealed class SavedServerEntry
    {
        public string Name { get; init; } = "";
        public string Host { get; init; } = "";
        public int Port { get; init; } = 25565;
        public byte[]? IconPng { get; init; }
    }

    public static class ServersDatReader
    {
        private const byte TagEnd = 0;
        private const byte TagByte = 1;
        private const byte TagShort = 2;
        private const byte TagInt = 3;
        private const byte TagLong = 4;
        private const byte TagFloat = 5;
        private const byte TagDouble = 6;
        private const byte TagByteArray = 7;
        private const byte TagString = 8;
        private const byte TagList = 9;
        private const byte TagCompound = 10;
        private const byte TagIntArray = 11;
        private const byte TagLongArray = 12;

        public static IReadOnlyList<SavedServerEntry> Read(string path)
        {
            if (!File.Exists(path))
                return Array.Empty<SavedServerEntry>();

            try
            {
                var bytes = File.ReadAllBytes(path);
                using var stream = OpenNbtStream(bytes);
                using var reader = new BinaryReader(stream, Encoding.UTF8, leaveOpen: false);
                return ParseRoot(reader);
            }
            catch
            {
                return Array.Empty<SavedServerEntry>();
            }
        }

        private static Stream OpenNbtStream(byte[] bytes)
        {
            if (bytes.Length >= 2 && bytes[0] == 0x1F && bytes[1] == 0x8B)
                return new GZipStream(new MemoryStream(bytes), CompressionMode.Decompress);

            return new MemoryStream(bytes);
        }

        private static List<SavedServerEntry> ParseRoot(BinaryReader reader)
        {
            var tagType = reader.ReadByte();
            if (tagType != TagCompound)
                return new List<SavedServerEntry>();

            _ = ReadString(reader);
            return ParseServersList(reader);
        }

        private static List<SavedServerEntry> ParseServersList(BinaryReader reader)
        {
            var results = new List<SavedServerEntry>();

            while (true)
            {
                var type = reader.ReadByte();
                if (type == TagEnd)
                    break;

                var name = ReadString(reader);
                if (type == TagList && name == "servers")
                {
                    results.AddRange(ReadServerList(reader));
                    continue;
                }

                SkipPayload(reader, type);
            }

            return results;
        }

        private static IEnumerable<SavedServerEntry> ReadServerList(BinaryReader reader)
        {
            var elementType = reader.ReadByte();
            var count = ReadInt32BE(reader);
            if (elementType != TagCompound)
            {
                for (var i = 0; i < count; i++)
                    SkipPayload(reader, elementType);
                yield break;
            }

            for (var i = 0; i < count; i++)
            {
                var entry = ReadServerCompound(reader);
                if (entry != null)
                    yield return entry;
            }
        }

        private static SavedServerEntry? ReadServerCompound(BinaryReader reader)
        {
            string? name = null;
            string? ip = null;
            byte[]? icon = null;

            while (true)
            {
                var type = reader.ReadByte();
                if (type == TagEnd)
                    break;

                var field = ReadString(reader);
                switch (field)
                {
                    case "name" when type == TagString:
                        name = ReadString(reader);
                        break;
                    case "ip" when type == TagString:
                        ip = ReadString(reader);
                        break;
                    case "icon" when type == TagByteArray:
                        icon = ReadByteArray(reader);
                        break;
                    default:
                        SkipPayload(reader, type);
                        break;
                }
            }

            if (string.IsNullOrWhiteSpace(ip))
                return null;

            var (host, port) = ParseAddress(ip);
            if (string.IsNullOrWhiteSpace(host))
                return null;

            return new SavedServerEntry
            {
                Name = string.IsNullOrWhiteSpace(name) ? host : name.Trim(),
                Host = host,
                Port = port,
                IconPng = icon is { Length: > 0 } ? icon : null
            };
        }

        public static (string Host, int Port) ParseAddress(string address)
        {
            var trimmed = address.Trim();
            if (trimmed.Length == 0)
                return ("", 25565);

            var lastColon = trimmed.LastIndexOf(':');
            if (lastColon > 0 && lastColon < trimmed.Length - 1)
            {
                var hostPart = trimmed[..lastColon];
                var portPart = trimmed[(lastColon + 1)..];
                if (int.TryParse(portPart, out var port) && port is > 0 and <= 65535)
                    return (hostPart, port);
            }

            return (trimmed, 25565);
        }

        private static byte[] ReadByteArray(BinaryReader reader)
        {
            var length = ReadInt32BE(reader);
            if (length <= 0)
                return Array.Empty<byte>();

            return reader.ReadBytes(length);
        }

        private static void SkipPayload(BinaryReader reader, byte type)
        {
            switch (type)
            {
                case TagByte:
                    reader.ReadByte();
                    break;
                case TagShort:
                    reader.ReadBytes(2);
                    break;
                case TagInt:
                    reader.ReadBytes(4);
                    break;
                case TagLong:
                    reader.ReadBytes(8);
                    break;
                case TagFloat:
                    reader.ReadBytes(4);
                    break;
                case TagDouble:
                    reader.ReadBytes(8);
                    break;
                case TagByteArray:
                    reader.ReadBytes(ReadInt32BE(reader));
                    break;
                case TagString:
                    _ = ReadString(reader);
                    break;
                case TagList:
                    SkipList(reader);
                    break;
                case TagCompound:
                    SkipCompound(reader);
                    break;
                case TagIntArray:
                    reader.ReadBytes(ReadInt32BE(reader) * 4);
                    break;
                case TagLongArray:
                    reader.ReadBytes(ReadInt32BE(reader) * 8);
                    break;
            }
        }

        private static void SkipList(BinaryReader reader)
        {
            var elementType = reader.ReadByte();
            var count = ReadInt32BE(reader);
            for (var i = 0; i < count; i++)
                SkipPayload(reader, elementType);
        }

        private static void SkipCompound(BinaryReader reader)
        {
            while (true)
            {
                var type = reader.ReadByte();
                if (type == TagEnd)
                    break;
                _ = ReadString(reader);
                SkipPayload(reader, type);
            }
        }

        private static string ReadString(BinaryReader reader)
        {
            var length = ReadUInt16BE(reader);
            if (length == 0)
                return "";

            var bytes = reader.ReadBytes(length);
            return Encoding.UTF8.GetString(bytes);
        }

        private static ushort ReadUInt16BE(BinaryReader reader)
        {
            var bytes = reader.ReadBytes(2);
            if (bytes.Length < 2)
                return 0;
            return (ushort)((bytes[0] << 8) | bytes[1]);
        }

        private static int ReadInt32BE(BinaryReader reader)
        {
            var bytes = reader.ReadBytes(4);
            if (bytes.Length < 4)
                return 0;
            return (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];
        }
    }
}
