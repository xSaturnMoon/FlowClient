using System.Text.Json;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Devices.Enumeration;
using Windows.Storage.Streams;

var devices = new List<DeviceDto>();
var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
var selector = BluetoothLEDevice.GetDeviceSelectorFromPairingState(true);
var infos = await DeviceInformation.FindAllAsync(selector);

foreach (var info in infos)
{
    using var ble = await BluetoothLEDevice.FromIdAsync(info.Id);
    var name = string.IsNullOrWhiteSpace(ble.Name) ? info.Name : ble.Name;
    if (string.IsNullOrWhiteSpace(name) || !seen.Add(name))
    {
        continue;
    }

    int? percent = null;
    var services = await ble.GetGattServicesAsync(BluetoothCacheMode.Uncached);
    if (services.Status != GattCommunicationStatus.Success)
    {
        continue;
    }

    foreach (var svc in services.Services)
    {
        if (!svc.Uuid.ToString().Contains("180F", StringComparison.OrdinalIgnoreCase))
        {
            svc.Dispose();
            continue;
        }

        var chars = await svc.GetCharacteristicsAsync(BluetoothCacheMode.Uncached);
        foreach (var ch in chars.Characteristics)
        {
            if (!ch.Uuid.ToString().Contains("2A19", StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }

            var read = await ch.ReadValueAsync(BluetoothCacheMode.Uncached);
            if (read.Status == GattCommunicationStatus.Success && read.Value.Length > 0)
            {
                var reader = DataReader.FromBuffer(read.Value);
                var bytes = new byte[read.Value.Length];
                reader.ReadBytes(bytes);
                percent = bytes[0];
            }
        }

        svc.Dispose();
    }

    if (percent is > 0 and <= 100)
    {
        devices.Add(new DeviceDto(name, percent.Value, false, "connected"));
    }
}

Console.WriteLine(JsonSerializer.Serialize(new { devices }));

record DeviceDto(string name, int percent, bool charging, string type);
