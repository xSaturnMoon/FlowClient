using System.Reflection;

var dll = args.Length > 0 ? args[0] : "CmlLib.Core.dll";
var asm = Assembly.LoadFrom(Path.GetFullPath(dll));
foreach (var t in asm.GetTypes().Where(t => t.Name.Contains("MinecraftLauncher") || t.Name.Contains("Java") || t.Name.Contains("Install")))
{
    Console.WriteLine("TYPE: " + t.FullName);
    foreach (var m in t.GetMethods(BindingFlags.Public | BindingFlags.Instance | BindingFlags.Static | BindingFlags.DeclaredOnly))
        Console.WriteLine("  " + m);
}
