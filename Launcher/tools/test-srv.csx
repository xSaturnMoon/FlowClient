using Launcher.Services;
var path = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "servers.dat");
var list = ServersDatReader.Read(path);
Console.WriteLine($"Count: {list.Count}");
foreach (var s in list.Take(6)) Console.WriteLine($"{s.Name} | icon={s.IconPng?.Length ?? 0}");
