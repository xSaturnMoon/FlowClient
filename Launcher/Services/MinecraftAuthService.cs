using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading;
using System.Threading.Tasks;

namespace Launcher.Services
{
    // ── Response models ──────────────────────────────────────────────────────

    public class DeviceCodeResponse
    {
        [JsonPropertyName("device_code")]  public string DeviceCode    { get; set; } = string.Empty;
        [JsonPropertyName("user_code")]    public string UserCode       { get; set; } = string.Empty;
        [JsonPropertyName("verification_uri")] public string VerificationUri { get; set; } = string.Empty;
        [JsonPropertyName("expires_in")]   public int    ExpiresIn      { get; set; }
        [JsonPropertyName("interval")]     public int    Interval        { get; set; } = 5;
    }

    public class MinecraftSkin
    {
        [JsonPropertyName("id")]      public string Id      { get; set; } = string.Empty;
        [JsonPropertyName("state")]   public string State   { get; set; } = string.Empty;
        [JsonPropertyName("url")]     public string Url     { get; set; } = string.Empty;
        [JsonPropertyName("variant")] public string Variant { get; set; } = string.Empty;
    }

    public class MinecraftCape
    {
        [JsonPropertyName("id")]    public string Id    { get; set; } = string.Empty;
        [JsonPropertyName("state")] public string State { get; set; } = string.Empty;
        [JsonPropertyName("url")]   public string Url   { get; set; } = string.Empty;
        [JsonPropertyName("alias")] public string Alias { get; set; } = string.Empty;
    }

    public class MinecraftProfile
    {
        [JsonPropertyName("id")]    public string Id    { get; set; } = string.Empty;
        [JsonPropertyName("name")]  public string Name  { get; set; } = string.Empty;
        [JsonPropertyName("skins")] public List<MinecraftSkin> Skins { get; set; } = new();
        [JsonPropertyName("capes")] public List<MinecraftCape> Capes { get; set; } = new();
    }

    // ── Saved account (persisted on disk) ────────────────────────────────────

    public class SavedAccount
    {
        public string MinecraftUsername  { get; set; } = string.Empty;
        public string MinecraftUuid      { get; set; } = string.Empty;
        public string MinecraftToken     { get; set; } = string.Empty;
        public string MsaRefreshToken    { get; set; } = string.Empty;
        /// <summary>Full 64x64 skin PNG URL (textures.minecraft.net) for the 3D viewer.</summary>
        public string SkinTextureUrl     { get; set; } = string.Empty;
        public string MicrosoftEmail     { get; set; } = string.Empty;
        public string SkinVariant        { get; set; } = "classic";
        public DateTime TokenExpiry      { get; set; }
        public DateTime LinkedAt         { get; set; }
        public DateTime LastLoginAt      { get; set; }
        public DateTime LastRefreshAt    { get; set; }
    }

    // ── Service ───────────────────────────────────────────────────────────────

    public class MinecraftAuthService
    {
        // ---- Official Minecraft / Xbox Live legacy MSA client -----------------
        // This is the same Client ID used by the official Minecraft Launcher,
        // MultiMC, ATLauncher and many other community launchers.
        // It works with login.live.com and the MBI_SSL scope.
        private const string ClientId = "00000000402b5328";
        private const string MsaScope = "service::user.auth.xboxlive.com::MBI_SSL offline_access";

        // Endpoints
        private const string DeviceCodeUrl = "https://login.live.com/oauth20_connect.srf";
        private const string TokenUrl      = "https://login.live.com/oauth20_token.srf";
        private const string XblUrl        = "https://user.auth.xboxlive.com/user/authenticate";
        private const string XstsUrl       = "https://xsts.auth.xboxlive.com/xsts/authorize";
        private const string McLoginUrl    = "https://api.minecraftservices.com/authentication/login_with_xbox";
        private const string McProfileUrl  = "https://api.minecraftservices.com/minecraft/profile";
        private const string ServicesUserAgent = "MinecraftLauncher/3.12.15";

        // Persistent storage path
        public static readonly string SavePath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "account.json");

        private static readonly HttpClient Http = new();

        private static readonly JsonSerializerOptions ProfileJsonOptions = new()
        {
            PropertyNameCaseInsensitive = true
        };

        static MinecraftAuthService()
        {
            Http.DefaultRequestHeaders.UserAgent.ParseAdd(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) FlowLauncher/1.0");
            Http.Timeout = TimeSpan.FromSeconds(30);
        }

        // ── Persistence ──────────────────────────────────────────────────────

        public SavedAccount? LoadSavedAccount()
        {
            try
            {
                if (!File.Exists(SavePath)) return null;
                var json = File.ReadAllText(SavePath);
                return JsonSerializer.Deserialize<SavedAccount>(json);
            }
            catch { return null; }
        }

        public void SaveAccount(SavedAccount account)
        {
            try
            {
                var existing = LoadSavedAccount();
                if (existing != null && existing.LinkedAt != default)
                    account.LinkedAt = existing.LinkedAt;
                else if (account.LinkedAt == default)
                    account.LinkedAt = DateTime.UtcNow;

                account.LastLoginAt = DateTime.UtcNow;
                Directory.CreateDirectory(Path.GetDirectoryName(SavePath)!);
                File.WriteAllText(SavePath, JsonSerializer.Serialize(account,
                    new JsonSerializerOptions { WriteIndented = true }));
            }
            catch { /* non bloccante */ }
        }

        public void DeleteSavedAccount()
        {
            try { if (File.Exists(SavePath)) File.Delete(SavePath); }
            catch { }
        }

        // ── Step 1 – Request Device Code ─────────────────────────────────────

        public async Task<DeviceCodeResponse> RequestDeviceCodeAsync()
        {
            var body = new FormUrlEncodedContent(new Dictionary<string, string>
            {
                { "client_id",     ClientId },
                { "scope",         MsaScope },
                { "response_type", "device_code" }
            });

            var resp = await Http.PostAsync(DeviceCodeUrl, body);
            var raw  = await resp.Content.ReadAsStringAsync();

            if (!resp.IsSuccessStatusCode)
                throw new Exception($"Errore Microsoft (step 1 – Device Code):\n{raw}");

            var result = JsonSerializer.Deserialize<DeviceCodeResponse>(raw);
            return result ?? throw new Exception("Risposta vuota dal server Microsoft.");
        }

        // ── Step 2 – Poll for MSA Token ───────────────────────────────────────

        private async Task<(string accessToken, string refreshToken)> PollForMsaTokenAsync(
            DeviceCodeResponse dc, Action<string> status, CancellationToken ct)
        {
            int interval = dc.Interval > 0 ? dc.Interval : 5;

            while (true)
            {
                ct.ThrowIfCancellationRequested();
                await Task.Delay(TimeSpan.FromSeconds(interval), ct);

                var body = new FormUrlEncodedContent(new Dictionary<string, string>
                {
                    { "grant_type",  "device_code" },
                    { "client_id",   ClientId },
                    { "device_code", dc.DeviceCode }
                });

                var resp = await Http.PostAsync(TokenUrl, body, ct);
                var raw  = await resp.Content.ReadAsStringAsync(ct);

                using var doc  = JsonDocument.Parse(raw);
                var root = doc.RootElement;

                if (root.TryGetProperty("access_token", out var at))
                {
                    var access  = at.GetString()  ?? throw new Exception("access_token null");
                    var refresh = root.TryGetProperty("refresh_token", out var rt)
                                  ? (rt.GetString() ?? string.Empty) : string.Empty;
                    return (access, refresh);
                }

                if (root.TryGetProperty("error", out var err))
                {
                    var errorCode = err.GetString();
                    switch (errorCode)
                    {
                        case "authorization_pending":
                            status("In attesa della conferma su Microsoft...");
                            break;
                        case "slow_down":
                            interval += 5;
                            status("Attendere...");
                            break;
                        case "expired_token":
                            throw new Exception("Il codice è scaduto. Riprova.");
                        case "access_denied":
                            throw new Exception("Accesso negato. L'utente ha rifiutato l'autorizzazione.");
                        default:
                            throw new Exception($"Errore Microsoft (step 2): {errorCode}");
                    }
                }
            }
        }

        // ── Step 3 – Refresh MSA Token ────────────────────────────────────────

        private async Task<(string accessToken, string refreshToken)> RefreshMsaTokenAsync(
            string refreshToken, CancellationToken ct)
        {
            var body = new FormUrlEncodedContent(new Dictionary<string, string>
            {
                { "grant_type",    "refresh_token" },
                { "client_id",     ClientId },
                { "refresh_token", refreshToken },
                { "scope",         MsaScope }
            });

            var resp = await Http.PostAsync(TokenUrl, body, ct);
            var raw  = await resp.Content.ReadAsStringAsync(ct);

            if (!resp.IsSuccessStatusCode)
                throw new Exception($"Errore nel rinnovo del token Microsoft:\n{raw}");

            using var doc  = JsonDocument.Parse(raw);
            var root = doc.RootElement;

            var access  = root.GetProperty("access_token").GetString()
                          ?? throw new Exception("access_token null dopo refresh.");
            var newRefresh = root.TryGetProperty("refresh_token", out var rt)
                             ? (rt.GetString() ?? refreshToken) : refreshToken;

            return (access, newRefresh);
        }

        // ── Step 4 – Xbox Live ────────────────────────────────────────────────

        private async Task<(string xblToken, string uhs)> AuthXboxLiveAsync(
            string msAccessToken, CancellationToken ct)
        {
            // IMPORTANT: For login.live.com MSA tokens the prefix MUST be "t="
            // The "d=" prefix is only for Azure AD v2 (login.microsoftonline.com) tokens.
            var body = new
            {
                Properties = new
                {
                    AuthMethod = "RPS",
                    SiteName   = "user.auth.xboxlive.com",
                    RpsTicket  = $"t={msAccessToken}"
                },
                RelyingParty = "http://auth.xboxlive.com",
                TokenType    = "JWT"
            };

            var req = new HttpRequestMessage(HttpMethod.Post, XblUrl)
            {
                Content = new StringContent(JsonSerializer.Serialize(body), Encoding.UTF8, "application/json")
            };
            req.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

            var resp = await Http.SendAsync(req, ct);
            var raw  = await resp.Content.ReadAsStringAsync(ct);

            if (!resp.IsSuccessStatusCode)
                throw new Exception($"Errore Xbox Live (step 4, HTTP {(int)resp.StatusCode}):\n{raw}");

            using var doc  = JsonDocument.Parse(raw);
            var root = doc.RootElement;

            var token = root.GetProperty("Token").GetString()
                        ?? throw new Exception("XBL Token null.");
            var uhs   = root.GetProperty("DisplayClaims")
                            .GetProperty("xui")[0]
                            .GetProperty("uhs").GetString()
                        ?? throw new Exception("XBL User Hash null.");

            return (token, uhs);
        }

        // ── Step 5 – XSTS ─────────────────────────────────────────────────────

        private async Task<string> AuthXstsAsync(string xblToken, CancellationToken ct)
        {
            var body = new
            {
                Properties = new
                {
                    SandboxId  = "RETAIL",
                    UserTokens = new[] { xblToken }
                },
                RelyingParty = "rp://api.minecraftservices.com/",
                TokenType    = "JWT"
            };

            var req = new HttpRequestMessage(HttpMethod.Post, XstsUrl)
            {
                Content = new StringContent(JsonSerializer.Serialize(body), Encoding.UTF8, "application/json")
            };
            req.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

            var resp = await Http.SendAsync(req, ct);
            var raw  = await resp.Content.ReadAsStringAsync(ct);

            if (resp.StatusCode == HttpStatusCode.Unauthorized)
            {
                // XErr codes: https://wiki.vg/Microsoft_Authentication_Scheme#Errors
                string hint = "L'account Xbox non è autorizzato.";
                try
                {
                    using var doc = JsonDocument.Parse(raw);
                    if (doc.RootElement.TryGetProperty("XErr", out var xerr))
                    {
                        long code = xerr.GetInt64();
                        hint = code switch
                        {
                            2148916233 => "L'account Microsoft non ha un account Xbox. Crea un account Xbox su xbox.com.",
                            2148916235 => "Xbox non è disponibile nel tuo paese.",
                            2148916236 or
                            2148916237 => "L'account richiede la verifica dell'età di un adulto.",
                            2148916238 => "Questo account è di un minore. Associalo a un account familiare Xbox.",
                            _          => $"Errore Xbox (XErr={code})."
                        };
                    }
                }
                catch { }
                throw new Exception(hint);
            }

            if (!resp.IsSuccessStatusCode)
                throw new Exception($"Errore XSTS (HTTP {(int)resp.StatusCode}):\n{raw}");

            using var doc2 = JsonDocument.Parse(raw);
            return doc2.RootElement.GetProperty("Token").GetString()
                   ?? throw new Exception("XSTS Token null.");
        }

        // ── Step 6 – Minecraft Login ──────────────────────────────────────────

        private async Task<string> LoginMinecraftAsync(
            string xstsToken, string uhs, CancellationToken ct)
        {
            var body = new { identityToken = $"XBL3.0 x={uhs};{xstsToken}" };

            var req = new HttpRequestMessage(HttpMethod.Post, McLoginUrl)
            {
                Content = new StringContent(JsonSerializer.Serialize(body), Encoding.UTF8, "application/json")
            };

            var resp = await Http.SendAsync(req, ct);
            var raw  = await resp.Content.ReadAsStringAsync(ct);

            if (!resp.IsSuccessStatusCode)
                throw new Exception($"Errore Minecraft Services (HTTP {(int)resp.StatusCode}):\n{raw}");

            using var doc = JsonDocument.Parse(raw);
            return doc.RootElement.GetProperty("access_token").GetString()
                   ?? throw new Exception("Minecraft access_token null.");
        }

        // ── Step 7 – Minecraft Profile ────────────────────────────────────────

        public async Task<MinecraftProfile> GetProfileAsync(string mcToken, CancellationToken ct)
        {
            var req = CreateServicesRequest(HttpMethod.Get, McProfileUrl);
            using var resp = await SendServicesAsync(req, mcToken, ct);

            if (resp.StatusCode == HttpStatusCode.NotFound)
                throw new Exception("Questo account Microsoft non possiede Minecraft Java Edition.");

            var raw = await resp.Content.ReadAsStringAsync(ct);
            if (!resp.IsSuccessStatusCode)
                throw new Exception($"Errore recupero profilo Minecraft (HTTP {(int)resp.StatusCode}):\n{raw}");

            var profile = JsonSerializer.Deserialize<MinecraftProfile>(raw, ProfileJsonOptions);
            return profile ?? throw new Exception("Risposta profilo Minecraft non valida.");
        }

        // ── Public: Full Device Code auth flow ────────────────────────────────

        public async Task<(MinecraftProfile profile, SavedAccount account)> AuthenticateAsync(
            DeviceCodeResponse deviceCode, Action<string> status, CancellationToken ct)
        {
            status("In attesa della conferma su Microsoft...");
            var (msAccess, msRefresh) = await PollForMsaTokenAsync(deviceCode, status, ct);

            status("Autenticazione Xbox Live...");
            var (xblToken, uhs) = await AuthXboxLiveAsync(msAccess, ct);

            status("Richiesta token XSTS...");
            var xstsToken = await AuthXstsAsync(xblToken, ct);

            status("Accesso a Minecraft Services...");
            var mcToken = await LoginMinecraftAsync(xstsToken, uhs, ct);

            status("Recupero profilo Minecraft...");
            var profile = await GetProfileAsync(mcToken, ct);

            var saved = new SavedAccount
            {
                MinecraftUsername = profile.Name,
                MinecraftUuid     = profile.Id,
                MinecraftToken    = mcToken,
                MsaRefreshToken   = msRefresh,
                SkinTextureUrl    = ResolveSkinUrl(profile),
                MicrosoftEmail    = "Microsoft account linked",
                SkinVariant       = "classic",
                TokenExpiry       = DateTime.UtcNow.AddHours(23),
                LinkedAt          = DateTime.UtcNow,
                LastRefreshAt     = DateTime.UtcNow
            };

            SaveAccount(saved);
            return (profile, saved);
        }

        // ── Public: Refresh existing account ──────────────────────────────────

        public async Task<(MinecraftProfile profile, SavedAccount account)> RefreshAccountAsync(
            SavedAccount saved, Action<string> status, CancellationToken ct)
        {
            if (string.IsNullOrEmpty(saved.MsaRefreshToken))
                throw new Exception("Nessun refresh token disponibile. Effettua di nuovo l'accesso.");

            status("Rinnovo sessione Microsoft...");
            var (msAccess, newRefresh) = await RefreshMsaTokenAsync(saved.MsaRefreshToken, ct);

            status("Autenticazione Xbox Live...");
            var (xblToken, uhs) = await AuthXboxLiveAsync(msAccess, ct);

            status("Richiesta token XSTS...");
            var xstsToken = await AuthXstsAsync(xblToken, ct);

            status("Accesso a Minecraft Services...");
            var mcToken = await LoginMinecraftAsync(xstsToken, uhs, ct);

            status("Recupero profilo Minecraft...");
            var profile = await GetProfileAsync(mcToken, ct);

            var updated = new SavedAccount
            {
                MinecraftUsername = profile.Name,
                MinecraftUuid     = profile.Id,
                MinecraftToken    = mcToken,
                MsaRefreshToken   = newRefresh,
                SkinTextureUrl    = ResolveSkinUrl(profile),
                MicrosoftEmail    = !string.IsNullOrEmpty(saved.MicrosoftEmail) ? saved.MicrosoftEmail : "Microsoft account linked",
                SkinVariant       = saved.SkinVariant,
                TokenExpiry       = DateTime.UtcNow.AddHours(23),
                LinkedAt          = saved.LinkedAt,
                LastRefreshAt     = DateTime.UtcNow
            };

            SaveAccount(updated);
            return (profile, updated);
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        /// <summary>
        /// Returns the actual 64x64 skin texture URL from textures.minecraft.net.
        /// Falls back to the mc-heads render if not available.
        /// </summary>
        public static string ResolveSkinUrl(MinecraftProfile profile)
        {
            if (profile.Skins != null && profile.Skins.Count > 0)
            {
                var active = profile.Skins.Find(s => s.State == "ACTIVE") ?? profile.Skins[0];
                if (!string.IsNullOrEmpty(active.Url))
                    return active.Url; // e.g. http://textures.minecraft.net/texture/<hash>
            }
            // Fallback: mc-heads renders a 3D body from the username
            return $"https://mc-heads.net/skin/{profile.Name}";
        }

        public static string ResolveActiveSkinVariant(MinecraftProfile profile)
        {
            var active = profile.Skins?.Find(s => s.State == "ACTIVE") ?? profile.Skins?.FirstOrDefault();
            return active?.Variant?.Equals("slim", StringComparison.OrdinalIgnoreCase) == true
                ? "slim"
                : "classic";
        }

        public static MinecraftCape? ResolveActiveCape(MinecraftProfile profile)
            => profile.Capes?.Find(c => c.State == "ACTIVE");

        public static string? ResolveActiveCapeUrl(MinecraftProfile profile)
            => ResolveActiveCape(profile)?.Url;

        public static string NormalizeCapeId(string capeId)
        {
            if (string.IsNullOrWhiteSpace(capeId))
                return capeId;

            var clean = capeId.Replace("-", "");
            if (clean.Length == 32 && Guid.TryParse(clean, out var guid))
                return guid.ToString("D");

            return capeId;
        }

        private static string ParseServicesError(string raw, HttpStatusCode status)
        {
            try
            {
                using var doc = JsonDocument.Parse(raw);
                if (doc.RootElement.TryGetProperty("errorMessage", out var msg))
                    return msg.GetString() ?? raw;
                if (doc.RootElement.TryGetProperty("developerMessage", out var dev))
                    return dev.GetString() ?? raw;
            }
            catch { }

            if (status == HttpStatusCode.Locked)
                return "Il profilo è temporaneamente bloccato. Attendi qualche minuto dopo un cambio skin, poi riprova.";

            if (status == HttpStatusCode.TooManyRequests)
                return "Troppe richieste. Mojang limita i cambi skin/mantello — riprova tra 1-2 minuti.";

            return string.IsNullOrWhiteSpace(raw) ? status.ToString() : raw;
        }

        private static void EnsureSuccess(HttpResponseMessage resp, string raw)
        {
            if (resp.IsSuccessStatusCode)
                return;

            var message = ParseServicesError(raw, resp.StatusCode);
            throw new ProfileApiException(resp.StatusCode,
                $"Richiesta profilo fallita (HTTP {(int)resp.StatusCode}): {message}");
        }

        private async Task<HttpResponseMessage> SendServicesAsync(
            HttpRequestMessage req,
            string? bearerToken,
            CancellationToken ct)
        {
            if (!string.IsNullOrEmpty(bearerToken))
                req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", bearerToken);

            return await Http.SendAsync(req, ct);
        }

        private HttpRequestMessage CreateServicesRequest(HttpMethod method, string url, HttpContent? content = null)
        {
            var req = new HttpRequestMessage(method, url) { Content = content };
            req.Headers.TryAddWithoutValidation("User-Agent", ServicesUserAgent);
            req.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            return req;
        }

        public async Task<string> DownloadSkinToTempAsync(string skinUrl, CancellationToken ct)
        {
            if (string.IsNullOrWhiteSpace(skinUrl))
                throw new Exception("Nessuna skin attiva da applicare.");

            var bytes = await Http.GetByteArrayAsync(skinUrl, ct);
            var path = Path.Combine(Path.GetTempPath(), $"flow_skin_{Guid.NewGuid():N}.png");
            await File.WriteAllBytesAsync(path, bytes, ct);
            return path;
        }

        // ── Skin upload ───────────────────────────────────────────────────────

        /// <summary>
        /// Uploads a local PNG file as the player's Minecraft skin.
        /// variant = "classic" (Steve) or "slim" (Alex).
        /// </summary>
        public async Task<MinecraftProfile> UploadSkinAsync(
            string minecraftToken,
            string pngFilePath,
            string variant,
            CancellationToken ct)
        {
            if (!File.Exists(pngFilePath))
                throw new Exception("File non trovato: " + pngFilePath);

            if (new FileInfo(pngFilePath).Length > 1024 * 64)
                throw new Exception("Il file della skin è troppo grande (max 64 KB).");

            using var content = new MultipartFormDataContent();
            content.Add(new StringContent(variant), "variant");

            var fileBytes = await File.ReadAllBytesAsync(pngFilePath, ct);
            var fileContent = new ByteArrayContent(fileBytes);
            fileContent.Headers.ContentType = new MediaTypeHeaderValue("image/png");
            content.Add(fileContent, "file", Path.GetFileName(pngFilePath));

            var req = CreateServicesRequest(HttpMethod.Post,
                "https://api.minecraftservices.com/minecraft/profile/skins", content);

            using var resp = await SendServicesAsync(req, minecraftToken, ct);
            var raw = await resp.Content.ReadAsStringAsync(ct);
            EnsureSuccess(resp, raw);

            return JsonSerializer.Deserialize<MinecraftProfile>(raw, ProfileJsonOptions)
                   ?? throw new Exception("Risposta profilo non valida dopo upload skin.");
        }

        public async Task<MinecraftProfile> SetActiveCapeAsync(string minecraftToken, string capeId, CancellationToken ct)
        {
            var payload = JsonSerializer.Serialize(new { capeId = NormalizeCapeId(capeId) });
            var content = new StringContent(payload, Encoding.UTF8, "application/json");
            var req = CreateServicesRequest(HttpMethod.Put,
                "https://api.minecraftservices.com/minecraft/profile/capes/active", content);

            using var resp = await SendServicesAsync(req, minecraftToken, ct);
            var raw  = await resp.Content.ReadAsStringAsync(ct);
            EnsureSuccess(resp, raw);

            return JsonSerializer.Deserialize<MinecraftProfile>(raw, ProfileJsonOptions)
                   ?? throw new Exception("Risposta profilo non valida dopo cambio mantello.");
        }

        public async Task<MinecraftProfile> DeactivateCapeAsync(string minecraftToken, CancellationToken ct)
        {
            var req = CreateServicesRequest(HttpMethod.Delete,
                "https://api.minecraftservices.com/minecraft/profile/capes/active");

            using var resp = await SendServicesAsync(req, minecraftToken, ct);
            var raw  = await resp.Content.ReadAsStringAsync(ct);
            EnsureSuccess(resp, raw);

            if (string.IsNullOrWhiteSpace(raw))
            {
                return await GetProfileAsync(minecraftToken, ct);
            }

            return JsonSerializer.Deserialize<MinecraftProfile>(raw, ProfileJsonOptions)
                   ?? await GetProfileAsync(minecraftToken, ct);
        }
    }
}
