using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Windows.Media.Control;
using Windows.Storage.Streams;

var manager = await GlobalSystemMediaTransportControlsSessionManager.RequestAsync();
var session = manager.GetCurrentSession();
if (session is null)
{
    Console.WriteLine("{\"active\":false}");
    return;
}

var properties = await session.TryGetMediaPropertiesAsync();
var timeline = session.GetTimelineProperties();
var duration = (timeline.EndTime - timeline.StartTime).TotalSeconds;
if (duration < 0)
{
    duration = 0;
}

var title = properties.Title ?? string.Empty;
var artist = properties.Artist ?? string.Empty;
var album = properties.AlbumTitle ?? string.Empty;
var trackId = Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes($"{title}|{artist}|{album}")))[..16];

var artDirectory = Path.Combine(
    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
    "FlowLauncher",
    "bin",
    "FlowMediaProbe");
Directory.CreateDirectory(artDirectory);

var artPath = Path.Combine(artDirectory, "current-art.dat");
var artMetaPath = Path.Combine(artDirectory, "current-art.id");

if (properties.Thumbnail is not null)
{
    try
    {
        using IRandomAccessStreamWithContentType stream = await properties.Thumbnail.OpenReadAsync();
        var reader = new DataReader(stream);
        await reader.LoadAsync((uint)stream.Size);
        var bytes = new byte[stream.Size];
        reader.ReadBytes(bytes);
        var existingId = File.Exists(artMetaPath) ? await File.ReadAllTextAsync(artMetaPath) : string.Empty;
        if (!string.Equals(existingId, trackId, StringComparison.Ordinal))
        {
            await File.WriteAllBytesAsync(artPath, bytes);
            await File.WriteAllTextAsync(artMetaPath, trackId);
        }
    }
    catch
    {
        // Keep the previous artwork if thumbnail decoding fails.
    }
}

var payload = new
{
    active = true,
    title,
    artist,
    album,
    trackId,
    artPath = File.Exists(artPath) ? artPath : string.Empty,
    position = Math.Round(timeline.Position.TotalSeconds, 1),
    duration = Math.Round(duration, 1),
    playing = session.GetPlaybackInfo().PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing,
    app = session.SourceAppUserModelId ?? string.Empty
};

Console.WriteLine(JsonSerializer.Serialize(payload));
