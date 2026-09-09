using System;
using System.Linq;
using Launcher.Services;
class T { static void Main() {
  var p = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "servers.dat");
  var list = ServersDatReader.Read(p);
  Console.WriteLine("count="+list.Count);
  foreach (var s in list.Take(5)) Console.WriteLine(s.Name + " icon=" + (s.IconPng?.Length ?? 0));
}}
