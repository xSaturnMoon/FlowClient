using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Launcher.Services
{
    internal sealed class MinecraftSessionService
    {
        private static readonly string SessionsDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "sessions");

        private static readonly JsonSerializerOptions JsonOptions = new()
        {
            WriteIndented = true,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
        };

        public const string GameSessionFileName = ".flowclient-session.json";

        public MinecraftSessionRecord BeginSession(string instanceId, string gameDirectory, int rootPid)
        {
            Directory.CreateDirectory(SessionsDir);
            Directory.CreateDirectory(gameDirectory);

            var record = new MinecraftSessionRecord
            {
                InstanceId = instanceId,
                GameDirectory = Path.GetFullPath(gameDirectory),
                RootPid = rootPid,
                StartedAtUtc = DateTime.UtcNow,
                Pids = new List<int> { rootPid }
            };

            Save(record);
            WriteGameSessionFile(record);
            return record;
        }

        public void AddPids(MinecraftSessionRecord record, IEnumerable<int> pids)
        {
            var changed = false;
            foreach (var pid in pids)
            {
                if (pid <= 0 || record.Pids.Contains(pid))
                    continue;

                record.Pids.Add(pid);
                changed = true;
            }

            if (!changed)
                return;

            Save(record);
            WriteGameSessionFile(record);
        }

        public void EndSession(MinecraftSessionRecord? record)
        {
            if (record == null)
                return;

            foreach (var pid in record.Pids.Distinct().OrderByDescending(p => p))
                ProcessKiller.ForceKillTree(pid);

            ProcessKiller.ForceKillTree(record.RootPid);
            MinecraftJavaProcessResolver.KillForLaunch(record.RootPid, record.GameDirectory);

            DeleteGameSessionFile(record.GameDirectory);
            DeleteSessionFile(record.InstanceId);
        }

        public MinecraftSessionRecord? Load(string instanceId)
        {
            var path = GetSessionPath(instanceId);
            if (!File.Exists(path))
                return null;

            try
            {
                return JsonSerializer.Deserialize<MinecraftSessionRecord>(File.ReadAllText(path), JsonOptions);
            }
            catch
            {
                return null;
            }
        }

        public MinecraftSessionRecord? LoadFromGameDirectory(string gameDirectory)
        {
            var path = Path.Combine(gameDirectory, GameSessionFileName);
            if (!File.Exists(path))
                return null;

            try
            {
                return JsonSerializer.Deserialize<MinecraftSessionRecord>(File.ReadAllText(path), JsonOptions);
            }
            catch
            {
                return null;
            }
        }

        public void CleanupStaleSessions()
        {
            if (!Directory.Exists(SessionsDir))
            {
                ProcessKiller.KillLikelyMinecraftOrphans();
                return;
            }

            foreach (var file in Directory.GetFiles(SessionsDir, "*.json"))
            {
                try
                {
                    var record = JsonSerializer.Deserialize<MinecraftSessionRecord>(File.ReadAllText(file), JsonOptions);
                    if (record == null)
                        continue;

                    var anyAlive = record.Pids.Any(ProcessKiller.IsAlive) || ProcessKiller.IsAlive(record.RootPid);
                    if (!anyAlive)
                    {
                        File.Delete(file);
                        DeleteGameSessionFile(record.GameDirectory);
                        continue;
                    }

                    EndSession(record);
                }
                catch
                {
                    // Ignore corrupt session files.
                }
            }

            ProcessKiller.KillLikelyMinecraftOrphans();
        }

        public static HashSet<int> SnapshotJavaPids() => ProcessKiller.SnapshotAllJavaPids();

        private void Save(MinecraftSessionRecord record)
        {
            Directory.CreateDirectory(SessionsDir);
            File.WriteAllText(GetSessionPath(record.InstanceId), JsonSerializer.Serialize(record, JsonOptions));
            WriteGameSessionFile(record);
        }

        private static void WriteGameSessionFile(MinecraftSessionRecord record)
        {
            try
            {
                Directory.CreateDirectory(record.GameDirectory);
                var path = Path.Combine(record.GameDirectory, GameSessionFileName);
                File.WriteAllText(path, JsonSerializer.Serialize(record, JsonOptions));
            }
            catch
            {
                // Ignore write failures.
            }
        }

        private static void DeleteSessionFile(string instanceId)
        {
            try
            {
                var path = GetSessionPath(instanceId);
                if (File.Exists(path))
                    File.Delete(path);
            }
            catch
            {
                // Ignore delete failures.
            }
        }

        private static void DeleteGameSessionFile(string gameDirectory)
        {
            try
            {
                var path = Path.Combine(gameDirectory, GameSessionFileName);
                if (File.Exists(path))
                    File.Delete(path);
            }
            catch
            {
                // Ignore delete failures.
            }
        }

        private static string GetSessionPath(string instanceId) =>
            Path.Combine(SessionsDir, $"{instanceId}.json");
    }

    internal sealed class MinecraftSessionRecord
    {
        public string InstanceId { get; set; } = "";
        public string GameDirectory { get; set; } = "";
        public int RootPid { get; set; }
        public List<int> Pids { get; set; } = new();
        public DateTime StartedAtUtc { get; set; }
    }
}
