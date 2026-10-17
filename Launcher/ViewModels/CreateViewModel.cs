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
    public class LoaderOptionViewModel : INotifyPropertyChanged
    {
        public string Name { get; set; } = "";
        public string? IconUri { get; set; }
        private bool _isSelected;
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

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? name = null) =>
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class VersionOptionViewModel : INotifyPropertyChanged
    {
        public string Version { get; set; } = "";
        private bool _isSelected;
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

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? name = null) =>
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class CreateViewModel : INotifyPropertyChanged
    {
        private readonly InstanceService _instanceService = new();
        private string _instanceName = "Fabric 1.21.1";
        private bool _hasUserCustomizedName;
        private string _selectedLoader = "Fabric";
        private string _selectedVersion = "1.21.1";
        private string _customVersion = "";

        public ObservableCollection<LoaderOptionViewModel> Loaders { get; } = new();
        public ObservableCollection<VersionOptionViewModel> Versions { get; } = new();

        public CreateViewModel()
        {
            string[] loaderNames = ["Fabric", "NeoForge", "Forge", "Vanilla", "Quilt"];
            foreach (var l in loaderNames)
            {
                Loaders.Add(new LoaderOptionViewModel
                {
                    Name = l,
                    IconUri = LoaderBranding.GetIconUri(l),
                    IsSelected = l == _selectedLoader
                });
            }

            string[] popular = ["1.21.4", "1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5", "1.12.2", "1.8.9"];
            foreach (var v in popular)
            {
                Versions.Add(new VersionOptionViewModel
                {
                    Version = v,
                    IsSelected = v == _selectedVersion
                });
            }
        }

        public string InstanceName
        {
            get => _instanceName;
            set
            {
                if (_instanceName != value)
                {
                    _instanceName = value;
                    _hasUserCustomizedName = true;
                    OnPropertyChanged();
                }
            }
        }

        public string SelectedLoader
        {
            get => _selectedLoader;
            set
            {
                if (_selectedLoader != value)
                {
                    _selectedLoader = value;
                    foreach (var l in Loaders)
                    {
                        l.IsSelected = string.Equals(l.Name, value, StringComparison.OrdinalIgnoreCase);
                    }
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(SelectedLoaderIconUri));
                    OnPropertyChanged(nameof(SelectedLoaderUpper));
                    UpdateDefaultName();
                }
            }
        }

        public string SelectedVersion
        {
            get => _selectedVersion;
            set
            {
                if (_selectedVersion != value)
                {
                    _selectedVersion = value;
                    foreach (var v in Versions)
                    {
                        v.IsSelected = string.Equals(v.Version, value, StringComparison.OrdinalIgnoreCase);
                    }
                    OnPropertyChanged();
                    UpdateDefaultName();
                }
            }
        }

        public string CustomVersion
        {
            get => _customVersion;
            set
            {
                if (_customVersion != value)
                {
                    _customVersion = value;
                    OnPropertyChanged();
                    if (!string.IsNullOrWhiteSpace(value))
                    {
                        SelectedVersion = value.Trim();
                    }
                }
            }
        }

        public string? SelectedLoaderIconUri => LoaderBranding.GetIconUri(_selectedLoader);
        public string SelectedLoaderUpper => _selectedLoader.ToUpperInvariant();

        private void UpdateDefaultName()
        {
            if (!_hasUserCustomizedName || string.IsNullOrWhiteSpace(_instanceName))
            {
                _instanceName = $"{_selectedLoader} {_selectedVersion}";
                OnPropertyChanged(nameof(InstanceName));
                _hasUserCustomizedName = false;
            }
        }

        public void SelectLoader(string loader)
        {
            SelectedLoader = loader;
        }

        public void SelectVersion(string version)
        {
            SelectedVersion = version;
            _customVersion = "";
            OnPropertyChanged(nameof(CustomVersion));
        }

        public MinecraftInstance? CreateInstance()
        {
            var name = string.IsNullOrWhiteSpace(_instanceName) ? $"{_selectedLoader} {_selectedVersion}" : _instanceName.Trim();
            var version = string.IsNullOrWhiteSpace(_selectedVersion) ? "1.21.1" : _selectedVersion.Trim();
            var loader = string.IsNullOrWhiteSpace(_selectedLoader) ? "Vanilla" : _selectedLoader.Trim();

            var instance = _instanceService.Create(name, version, loader);
            InstanceSelectionService.ForceSelect(instance.Id);
            return instance;
        }

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? name = null) =>
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
