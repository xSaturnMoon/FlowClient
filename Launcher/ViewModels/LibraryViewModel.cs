using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Linq;
using System.Runtime.CompilerServices;
using Launcher.Helpers;
using Launcher.Models;
using Launcher.Services;

namespace Launcher.ViewModels
{
    public class LibraryViewModel : INotifyPropertyChanged
    {
        private readonly InstanceService _instanceService = new();
        private bool _isViewingSettings;
        private LibraryCardItemViewModel? _selectedSettingsInstance;
        private string _searchQuery = "";

        public ObservableCollection<LibraryCardItemViewModel> AllVersions { get; } = new();
        public ObservableCollection<LibraryCardItemViewModel> FilteredVersions { get; } = new();

        public bool IsViewingSettings
        {
            get => _isViewingSettings;
            set
            {
                if (_isViewingSettings != value)
                {
                    _isViewingSettings = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(IsViewingGrid));
                }
            }
        }

        public bool IsViewingGrid => !_isViewingSettings;

        public LibraryCardItemViewModel? SelectedSettingsInstance
        {
            get => _selectedSettingsInstance;
            set
            {
                if (_selectedSettingsInstance != value)
                {
                    _selectedSettingsInstance = value;
                    OnPropertyChanged();
                }
            }
        }

        public string SearchQuery
        {
            get => _searchQuery;
            set
            {
                if (_searchQuery != value)
                {
                    _searchQuery = value;
                    OnPropertyChanged();
                    ApplyFilter();
                }
            }
        }

        public bool HasVersions => FilteredVersions.Count > 0;
        public bool IsEmpty => FilteredVersions.Count == 0;

        public event PropertyChangedEventHandler? PropertyChanged;

        public LibraryViewModel()
        {
            LoadInstances();
            InstanceService.InstancesChanged += OnInstancesChanged;
            InstanceSelectionService.SelectedInstanceChanged += OnSelectedInstanceChanged;
        }

        private void OnInstancesChanged()
        {
            System.Windows.Application.Current?.Dispatcher.BeginInvoke(LoadInstances);
        }

        private void OnSelectedInstanceChanged(string id)
        {
            System.Windows.Application.Current?.Dispatcher.BeginInvoke(() =>
            {
                foreach (var item in AllVersions)
                    item.IsSelected = item.Id == id;
            });
        }

        public void LoadInstances()
        {
            var instances = _instanceService.GetAll()
                .OrderByDescending(i => i.IsFavorite)
                .ThenByDescending(i => i.LastPlayedAt ?? DateTime.MinValue)
                .ToList();

            var currentSelected = InstanceSelectionService.SelectedInstanceId;
            if (string.IsNullOrEmpty(currentSelected) && instances.Count > 0)
            {
                currentSelected = instances[0].Id;
                InstanceSelectionService.ForceSelect(currentSelected);
            }

            AllVersions.Clear();
            foreach (var inst in instances)
            {
                AllVersions.Add(new LibraryCardItemViewModel
                {
                    Id = inst.Id,
                    Name = inst.Name,
                    MinecraftVersion = inst.MinecraftVersion,
                    Loader = inst.Loader,
                    LoaderIconUri = LoaderBranding.GetIconUri(inst.Loader),
                    ArtworkUri = ResolveArtworkUri(inst.MinecraftVersion, inst.Name),
                    LastPlayedText = inst.LastPlayedAt.HasValue
                        ? inst.LastPlayedAt.Value.ToString("MMM dd, yyyy")
                        : "Never played",
                    IsFavorite = inst.IsFavorite,
                    IsSelected = inst.Id == currentSelected,
                    RamMb = inst.RamMb,
                    JavaPath = inst.JavaPath,
                    JvmArgs = inst.JvmArgs
                });
            }

            ApplyFilter();
        }

        public static string ResolveArtworkUri(string mcVersion, string name)
        {
            var v = (mcVersion ?? "").ToLowerInvariant().Trim();
            var n = (name ?? "").ToLowerInvariant().Trim();

            if (v.StartsWith("1.21") || v.StartsWith("26.") || n.Contains("1.21") || n.Contains("26.") || n.Contains("trial"))
                return "pack://application:,,,/Assets/Versions/1_21.jpg";

            if (v.StartsWith("1.20") || n.Contains("1.20") || n.Contains("trail"))
                return "pack://application:,,,/Assets/Versions/1_20.jpg";

            if (v.StartsWith("1.19") || n.Contains("1.19") || n.Contains("wild"))
                return "pack://application:,,,/Assets/Versions/1_19.jpg";

            if (v.StartsWith("1.18") || v.StartsWith("1.17") || n.Contains("1.18") || n.Contains("1.17") || n.Contains("cave"))
                return "pack://application:,,,/Assets/Versions/1_18.jpg";

            if (v.StartsWith("1.16") || v.StartsWith("1.15") || n.Contains("1.16") || n.Contains("1.15") || n.Contains("nether"))
                return "pack://application:,,,/Assets/Versions/1_16.jpg";

            if (v.StartsWith("1.14") || v.StartsWith("1.13") || n.Contains("1.14") || n.Contains("1.13") || n.Contains("village"))
                return "pack://application:,,,/Assets/Versions/1_14.jpg";

            return "pack://application:,,,/Assets/Versions/default.jpg";
        }

        public void ApplyFilter()
        {
            FilteredVersions.Clear();
            var q = (_searchQuery ?? "").Trim().ToLowerInvariant();
            var matched = string.IsNullOrEmpty(q)
                ? AllVersions
                : AllVersions.Where(v => v.Name.ToLowerInvariant().Contains(q) ||
                                         v.MinecraftVersion.ToLowerInvariant().Contains(q) ||
                                         v.Loader.ToLowerInvariant().Contains(q));

            foreach (var item in matched)
                FilteredVersions.Add(item);

            OnPropertyChanged(nameof(HasVersions));
            OnPropertyChanged(nameof(IsEmpty));
        }

        public void OpenSettings(LibraryCardItemViewModel item)
        {
            SelectedSettingsInstance = item;
            IsViewingSettings = true;
        }

        public void CloseSettings()
        {
            IsViewingSettings = false;
            SelectedSettingsInstance = null;
        }

        protected void OnPropertyChanged([CallerMemberName] string? propName = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propName));
    }

    public class LibraryCardItemViewModel : INotifyPropertyChanged
    {
        private bool _isSelected;
        private bool _isFavorite;

        public string Id { get; set; } = "";
        public string Name { get; set; } = "Instance";
        public string MinecraftVersion { get; set; } = "1.20.1";
        public string Loader { get; set; } = "Fabric";
        public string? LoaderIconUri { get; set; }
        public string ArtworkUri { get; set; } = "pack://application:,,,/Assets/Versions/default.jpg";
        public string LastPlayedText { get; set; } = "Never played";
        public int RamMb { get; set; } = 4096;
        public string JavaPath { get; set; } = "";
        public string JvmArgs { get; set; } = "";

        public string DisplayTitle
        {
            get
            {
                var n = (Name ?? "").Trim();
                // If it's a distinctive custom name like "Beauty" or "new build", display it prominently
                if (!string.IsNullOrWhiteSpace(n) &&
                    !n.Equals("New Instance", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals(MinecraftVersion, StringComparison.OrdinalIgnoreCase))
                {
                    return n.ToUpperInvariant();
                }

                // Otherwise display FLOW {Major.Minor}
                var v = MinecraftVersion;
                if (v.StartsWith("1."))
                {
                    var parts = v.Split('.');
                    if (parts.Length >= 2) v = parts[0] + "." + parts[1];
                }
                return $"FLOW {v}";
            }
        }

        public string SubtitleText
        {
            get
            {
                var n = (Name ?? "").Trim();
                if (!string.IsNullOrWhiteSpace(n) &&
                    !n.Equals("New Instance", StringComparison.OrdinalIgnoreCase) &&
                    !n.Equals(MinecraftVersion, StringComparison.OrdinalIgnoreCase))
                {
                    return $"{Loader} • {MinecraftVersion}";
                }
                return $"{Loader} {MinecraftVersion}";
            }
        }

        public bool IsSelected
        {
            get => _isSelected;
            set
            {
                if (_isSelected != value)
                {
                    _isSelected = value;
                    OnPropertyChanged();
                }
            }
        }

        public bool IsFavorite
        {
            get => _isFavorite;
            set
            {
                if (_isFavorite != value)
                {
                    _isFavorite = value;
                    OnPropertyChanged();
                }
            }
        }

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? propName = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propName));
    }
}
