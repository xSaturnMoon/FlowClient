using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using Launcher.Models;

namespace Launcher.ViewModels
{
    public class ExploreViewModel : INotifyPropertyChanged
    {
        private string _searchQuery = "";
        private ExploreSortMode _sortMode = ExploreSortMode.LastUsed;
        private string? _selectedInstanceId;
        private ExploreDetailTab _detailTab = ExploreDetailTab.Catalog;
        private ExploreMainMode _mainMode = ExploreMainMode.Empty;
        private bool _isCreating;
        private bool _showCatalog;
        private CatalogType _catalogType = CatalogType.Mod;
        private string _catalogSearch = "";
        private string? _selectedCatalogProjectId;
        private string _modSearch = "";
        private bool _isBusy;
        private string? _statusMessage;

        // Create form
        private string _newName = "";
        private string _newVersion = "1.20.1";
        private string _newLoader = "Fabric";
        private string _versionTypeFilter = "release";
        private string _versionSearch = "";
        private bool _isLoadingVersions;

        private ObservableCollection<string> _filteredVersions = new();

        // Edit settings
        private string _editName = "";
        private string _editVersion = "";
        private string _editLoader = "";
        private int _editRam = 4096;
        private string _editJvmArgs = "";
        private int _editWidth = 1920;
        private int _editHeight = 1080;
        private bool _editFullscreen;

        private ObservableCollection<InstanceListItemViewModel> _instances = new();
        private ObservableCollection<InstalledContentViewModel> _mods = new();
        private ObservableCollection<InstalledContentViewModel> _resourcePacks = new();
        private ObservableCollection<InstalledContentViewModel> _shaders = new();
        private ObservableCollection<WorldItemViewModel> _worlds = new();
        private ObservableCollection<CatalogHitViewModel> _catalogHits = new();
        private ObservableCollection<CatalogVersionViewModel> _catalogVersions = new();

        public string SearchQuery
        {
            get => _searchQuery;
            set { _searchQuery = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasSearch)); }
        }

        public bool HasSearch => !string.IsNullOrWhiteSpace(_searchQuery);

        public ExploreSortMode SortMode
        {
            get => _sortMode;
            set { _sortMode = value; OnPropertyChanged(); }
        }

        public string? SelectedInstanceId
        {
            get => _selectedInstanceId;
            set
            {
                _selectedInstanceId = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(HasSelection));
                OnPropertyChanged(nameof(ShowDetail));
                OnPropertyChanged(nameof(ShowCatalogBrowser));
            }
        }

        public bool HasSelection => !string.IsNullOrEmpty(_selectedInstanceId);
        public bool ShowDetail => HasSelection && !IsCreating && _mainMode == ExploreMainMode.InstanceDetail;
        public bool ShowGlobalTextures => _mainMode == ExploreMainMode.GlobalTextures;
        public bool ShowGlobalShaders => _mainMode == ExploreMainMode.GlobalShaders;
        public bool ShowGlobalPanel => ShowGlobalTextures || ShowGlobalShaders;

        public ExploreMainMode MainMode
        {
            get => _mainMode;
            set
            {
                _mainMode = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(ShowDetail));
                OnPropertyChanged(nameof(ShowEmpty));
                OnPropertyChanged(nameof(ShowGlobalTextures));
                OnPropertyChanged(nameof(ShowGlobalShaders));
                OnPropertyChanged(nameof(ShowGlobalPanel));
                OnPropertyChanged(nameof(ShowCatalogBrowser));
                OnPropertyChanged(nameof(GlobalPanelTitle));
                OnPropertyChanged(nameof(GlobalPanelSubtitle));
            }
        }

        public string GlobalPanelTitle => _mainMode switch
        {
            ExploreMainMode.GlobalTextures => "Textures",
            ExploreMainMode.GlobalShaders => "Shaders",
            _ => ""
        };

        public string GlobalPanelSubtitle => "Shared across every installation and Minecraft version.";

        public bool ShowModsTabs => !string.Equals(_detailLoader, "Vanilla", StringComparison.OrdinalIgnoreCase);

        public ExploreDetailTab DetailTab
        {
            get => _detailTab;
            set
            {
                _detailTab = value;
                OnPropertyChanged();
                NotifyTabs();
            }
        }

        public bool IsModsTab => _detailTab == ExploreDetailTab.Mods;
        public bool ShowCatalogBrowser => ShowGlobalPanel || (ShowDetail && IsModsTab);
        public bool IsCatalogTab => _detailTab == ExploreDetailTab.Catalog;
        public bool IsOverviewTab => _detailTab == ExploreDetailTab.Overview;
        public bool IsResourcePacksTab => _detailTab == ExploreDetailTab.ResourcePacks;
        public bool IsShadersTab => _detailTab == ExploreDetailTab.Shaders;
        public bool IsWorldsTab => _detailTab == ExploreDetailTab.Worlds;
        public bool IsSettingsTab => _detailTab == ExploreDetailTab.Settings;
        public bool HasInstalledMods => _mods.Count > 0;
        public bool HasNoInstalledMods => _mods.Count == 0;

        void NotifyTabs()
        {
            OnPropertyChanged(nameof(IsModsTab));
            OnPropertyChanged(nameof(ShowCatalogBrowser));
            OnPropertyChanged(nameof(IsCatalogTab));
            OnPropertyChanged(nameof(IsOverviewTab));
            OnPropertyChanged(nameof(IsResourcePacksTab));
            OnPropertyChanged(nameof(IsShadersTab));
            OnPropertyChanged(nameof(IsWorldsTab));
            OnPropertyChanged(nameof(IsSettingsTab));
        }

        public bool IsCreating
        {
            get => _isCreating;
            set { _isCreating = value; OnPropertyChanged(); OnPropertyChanged(nameof(ShowDetail)); OnPropertyChanged(nameof(ShowEmpty)); OnPropertyChanged(nameof(ShowGlobalPanel)); OnPropertyChanged(nameof(ShowCatalogBrowser)); }
        }

        public bool ShowCatalog
        {
            get => _showCatalog;
            set { _showCatalog = value; OnPropertyChanged(); OnPropertyChanged(nameof(ShowDetail)); OnPropertyChanged(nameof(ShowEmpty)); }
        }

        public bool ShowEmpty => !HasSelection && !IsCreating && _mainMode == ExploreMainMode.Empty;

        public CatalogType CatalogType
        {
            get => _catalogType;
            set { _catalogType = value; OnPropertyChanged(); OnPropertyChanged(nameof(CatalogTitle)); }
        }

        public string CatalogTitle => _catalogType switch
        {
            CatalogType.ResourcePack => "Resource Pack Catalog",
            CatalogType.Shader => "Shader Catalog",
            _ => "Mod Catalog"
        };

        public string CatalogSearch
        {
            get => _catalogSearch;
            set { _catalogSearch = value; OnPropertyChanged(); }
        }

        public string? SelectedCatalogProjectId
        {
            get => _selectedCatalogProjectId;
            set { _selectedCatalogProjectId = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasCatalogProject)); }
        }

        public bool HasCatalogProject => !string.IsNullOrEmpty(_selectedCatalogProjectId);

        public string ModSearch
        {
            get => _modSearch;
            set { _modSearch = value; OnPropertyChanged(); }
        }

        public bool IsBusy
        {
            get => _isBusy;
            set { _isBusy = value; OnPropertyChanged(); }
        }

        public string? StatusMessage
        {
            get => _statusMessage;
            set { _statusMessage = value; OnPropertyChanged(); }
        }

        public string NewName { get => _newName; set { _newName = value; OnPropertyChanged(); } }
        public string NewVersion { get => _newVersion; set { _newVersion = value; OnPropertyChanged(); } }
        public string NewLoader { get => _newLoader; set { _newLoader = value; OnPropertyChanged(); } }
        public string VersionTypeFilter { get => _versionTypeFilter; set { _versionTypeFilter = value; OnPropertyChanged(); } }
        public string VersionSearch
        {
            get => _versionSearch;
            set { _versionSearch = value; OnPropertyChanged(); }
        }
        public bool IsLoadingVersions
        {
            get => _isLoadingVersions;
            set { _isLoadingVersions = value; OnPropertyChanged(); }
        }

        public ObservableCollection<string> FilteredVersions
        {
            get => _filteredVersions;
            set { _filteredVersions = value; OnPropertyChanged(); }
        }

        public string EditName { get => _editName; set { _editName = value; OnPropertyChanged(); } }
        public string EditVersion { get => _editVersion; set { _editVersion = value; OnPropertyChanged(); } }
        public string EditLoader { get => _editLoader; set { _editLoader = value; OnPropertyChanged(); } }
        public int EditRam { get => _editRam; set { _editRam = value; OnPropertyChanged(); } }
        public string EditJvmArgs { get => _editJvmArgs; set { _editJvmArgs = value; OnPropertyChanged(); } }
        public int EditWidth { get => _editWidth; set { _editWidth = value; OnPropertyChanged(); } }
        public int EditHeight { get => _editHeight; set { _editHeight = value; OnPropertyChanged(); } }
        public bool EditFullscreen { get => _editFullscreen; set { _editFullscreen = value; OnPropertyChanged(); } }

        // Detail display (bound when selected)
        private string _detailName = "";
        private string _detailVersion = "";
        private string _detailLoader = "";
        private string _detailSize = "";
        private string _detailPath = "";
        private string _detailCreated = "";
        private string _detailLastPlayed = "";
        private string _detailModCount = "0";
        private string _detailIconColor = "#0A84FF";
        private string _detailIconLetter = "F";
        private string? _detailLoaderIconUri;

        public string DetailName { get => _detailName; set { _detailName = value; OnPropertyChanged(); } }
        public string DetailVersion { get => _detailVersion; set { _detailVersion = value; OnPropertyChanged(); } }
        public string DetailLoader { get => _detailLoader; set { _detailLoader = value; OnPropertyChanged(); OnPropertyChanged(nameof(ShowModsTabs)); } }
        public string DetailSize { get => _detailSize; set { _detailSize = value; OnPropertyChanged(); } }
        public string DetailPath { get => _detailPath; set { _detailPath = value; OnPropertyChanged(); } }
        public string DetailCreated { get => _detailCreated; set { _detailCreated = value; OnPropertyChanged(); } }
        public string DetailLastPlayed { get => _detailLastPlayed; set { _detailLastPlayed = value; OnPropertyChanged(); } }
        public string DetailModCount { get => _detailModCount; set { _detailModCount = value; OnPropertyChanged(); } }
        public string DetailIconColor { get => _detailIconColor; set { _detailIconColor = value; OnPropertyChanged(); } }
        public string DetailIconLetter { get => _detailIconLetter; set { _detailIconLetter = value; OnPropertyChanged(); } }
        public string? DetailLoaderIconUri { get => _detailLoaderIconUri; set { _detailLoaderIconUri = value; OnPropertyChanged(); } }

        public ObservableCollection<InstanceListItemViewModel> Instances
        {
            get => _instances;
            set { _instances = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasInstances)); }
        }

        public bool HasInstances => _instances.Count > 0;

        public ObservableCollection<InstalledContentViewModel> Mods
        {
            get => _mods;
            set
            {
                _mods = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(ModCount));
                OnPropertyChanged(nameof(HasInstalledMods));
                OnPropertyChanged(nameof(HasNoInstalledMods));
            }
        }

        public int ModCount => _mods.Count;
        public ObservableCollection<InstalledContentViewModel> ResourcePacks { get => _resourcePacks; set { _resourcePacks = value; OnPropertyChanged(); } }
        public ObservableCollection<InstalledContentViewModel> Shaders { get => _shaders; set { _shaders = value; OnPropertyChanged(); } }
        public ObservableCollection<InstalledContentViewModel> GlobalInstalled { get; } = new();
        public bool HasNoGlobalInstalled => GlobalInstalled.Count == 0;
        public ObservableCollection<WorldItemViewModel> Worlds { get => _worlds; set { _worlds = value; OnPropertyChanged(); } }
        public ObservableCollection<CatalogHitViewModel> CatalogHits { get => _catalogHits; set { _catalogHits = value; OnPropertyChanged(); } }
        public ObservableCollection<CatalogVersionViewModel> CatalogVersions { get => _catalogVersions; set { _catalogVersions = value; OnPropertyChanged(); } }
        public ObservableCollection<string> CatalogDetailCategories { get; } = new();
        public ObservableCollection<CatalogContentBlockViewModel> CatalogDetailBlocks { get; } = new();
        public ObservableCollection<CatalogContentBlockViewModel> CatalogChangelogBlocks { get; } = new();

        // Selected catalog detail
        private string _catalogDetailTitle = "";
        private string _catalogDetailDesc = "";
        private string _catalogDetailBody = "";
        private string _catalogDetailChangelog = "";
        private string? _catalogDetailIcon;
        private string _catalogDetailDownloadsText = "";
        private string _catalogDetailFollowersText = "";
        private string? _catalogLatestVersionId;
        private bool _catalogIsInstalled;
        private string? _catalogInstalledFileName;
        private CatalogDetailTab _catalogDetailTab = CatalogDetailTab.Description;

        public string CatalogDetailTitle { get => _catalogDetailTitle; set { _catalogDetailTitle = value; OnPropertyChanged(); } }
        public string CatalogDetailDesc { get => _catalogDetailDesc; set { _catalogDetailDesc = value; OnPropertyChanged(); } }
        public string CatalogDetailBody { get => _catalogDetailBody; set { _catalogDetailBody = value; OnPropertyChanged(); } }
        public string CatalogDetailChangelog { get => _catalogDetailChangelog; set { _catalogDetailChangelog = value; OnPropertyChanged(); } }
        public string? CatalogDetailIcon { get => _catalogDetailIcon; set { _catalogDetailIcon = value; OnPropertyChanged(); } }
        public string CatalogDetailDownloadsText { get => _catalogDetailDownloadsText; set { _catalogDetailDownloadsText = value; OnPropertyChanged(); } }
        public string CatalogDetailFollowersText { get => _catalogDetailFollowersText; set { _catalogDetailFollowersText = value; OnPropertyChanged(); } }
        public string? CatalogLatestVersionId { get => _catalogLatestVersionId; set { _catalogLatestVersionId = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanPrimaryAction)); } }
        public bool CatalogIsInstalled
        {
            get => _catalogIsInstalled;
            set
            {
                _catalogIsInstalled = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(CatalogPrimaryActionLabel));
                OnPropertyChanged(nameof(CanPrimaryAction));
            }
        }

        public string? CatalogInstalledFileName
        {
            get => _catalogInstalledFileName;
            set { _catalogInstalledFileName = value; OnPropertyChanged(); }
        }

        public string CatalogPrimaryActionLabel => _catalogIsInstalled ? "Uninstall" : "Install";
        public bool CanPrimaryAction => _catalogIsInstalled
            ? !string.IsNullOrEmpty(_catalogInstalledFileName)
            : !string.IsNullOrEmpty(_catalogLatestVersionId);

        public CatalogDetailTab CatalogDetailTab
        {
            get => _catalogDetailTab;
            set
            {
                _catalogDetailTab = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(IsCatalogDescTab));
                OnPropertyChanged(nameof(IsCatalogChangelogTab));
                OnPropertyChanged(nameof(IsCatalogVersionsTab));
            }
        }

        public bool IsCatalogDescTab => _catalogDetailTab == CatalogDetailTab.Description;
        public bool IsCatalogChangelogTab => _catalogDetailTab == CatalogDetailTab.Changelog;
        public bool IsCatalogVersionsTab => _catalogDetailTab == CatalogDetailTab.Versions;

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? name = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));

        public void OnPropertyChangedPublic(string name)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class InstanceListItemViewModel
    {
        public string Id { get; set; } = "";
        public string Name { get; set; } = "";
        public string Version { get; set; } = "";
        public string Loader { get; set; } = "";
        public int ModCount { get; set; }
        public string LastPlayedText { get; set; } = "Never";
        public string SizeText { get; set; } = "—";
        public bool IsFavorite { get; set; }
        public string IconColor { get; set; } = "#0A84FF";
        public string IconLetter { get; set; } = "F";
        public string? LoaderIconUri { get; set; }
        public bool IsSelected { get; set; }
    }

    public class InstalledContentViewModel
    {
        public string ProjectId { get; set; } = "";
        public string Title { get; set; } = "";
        public string Author { get; set; } = "";
        public string VersionNumber { get; set; } = "";
        public string? IconUrl { get; set; }
        public bool Enabled { get; set; } = true;
        public bool IsFavorite { get; set; }
        public string? UpdateAvailable { get; set; }
        public string FileName { get; set; } = "";
        public string Slug { get; set; } = "";
    }

    public class WorldItemViewModel
    {
        public string Name { get; set; } = "";
        public string SizeText { get; set; } = "";
        public string LastOpenedText { get; set; } = "";
        public string Path { get; set; } = "";
    }

    public enum CatalogDetailTab
    {
        Description,
        Changelog,
        Versions
    }

    public class CatalogContentBlockViewModel
    {
        public bool IsImage { get; set; }
        public string? Text { get; set; }
        public string? ImageUrl { get; set; }
    }

    public class CatalogHitViewModel
    {
        public string ProjectId { get; set; } = "";
        public string Slug { get; set; } = "";
        public string Title { get; set; } = "";
        public string Description { get; set; } = "";
        public string Author { get; set; } = "";
        public string? IconUrl { get; set; }
        public string DownloadsText { get; set; } = "";
    }

    public class CatalogVersionViewModel : INotifyPropertyChanged
    {
        public string Id { get; set; } = "";
        public string Name { get; set; } = "";
        public string VersionNumber { get; set; } = "";
        public string LoadersText { get; set; } = "";
        public string? Changelog { get; set; }

        private bool _isInstalled;
        public bool IsInstalled
        {
            get => _isInstalled;
            set { _isInstalled = value; OnPropertyChanged(); }
        }

        public event PropertyChangedEventHandler? PropertyChanged;
        private void OnPropertyChanged([CallerMemberName] string? name = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
