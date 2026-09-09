using System.Diagnostics;
using System.Globalization;
using System.Text;
using System.Windows;
using Microsoft.Web.WebView2.Wpf;

namespace Launcher.Helpers
{
    public static class CatalogHtmlHelper
    {
        public static async Task ShowAsync(WebView2 webView, string? html)
        {
            await EnsureCoreAsync(webView);

            var document = WrapDocument(ModrinthContentFormatter.ToDisplayHtml(html));

            var tcs = new TaskCompletionSource<bool>();
            void OnCompleted(object? sender, Microsoft.Web.WebView2.Core.CoreWebView2NavigationCompletedEventArgs e)
            {
                webView.NavigationCompleted -= OnCompleted;
                tcs.TrySetResult(true);
            }

            webView.NavigationCompleted += OnCompleted;
            webView.NavigateToString(document);
            await tcs.Task.ConfigureAwait(true);
            await ResizeToContentAsync(webView).ConfigureAwait(true);
        }

        public static async Task EnsureCoreAsync(WebView2 webView)
        {
            if (webView.CoreWebView2 != null)
                return;

            await webView.EnsureCoreWebView2Async().ConfigureAwait(true);

            var core = webView.CoreWebView2!;
            core.Settings.AreDefaultContextMenusEnabled = false;
            core.Settings.IsStatusBarEnabled = false;
            core.Settings.AreDevToolsEnabled = false;
            core.NewWindowRequested += (_, e) =>
            {
                e.Handled = true;
                if (!string.IsNullOrWhiteSpace(e.Uri))
                {
                    Process.Start(new ProcessStartInfo(e.Uri) { UseShellExecute = true });
                }
            };
        }

        private static async Task ResizeToContentAsync(WebView2 webView)
        {
            if (webView.CoreWebView2 == null)
                return;

            try
            {
                var raw = await webView.CoreWebView2.ExecuteScriptAsync(
                    "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)");

                if (double.TryParse(raw, NumberStyles.Float, CultureInfo.InvariantCulture, out var height))
                    webView.Height = Math.Clamp(height + 8, 120, 4000);
            }
            catch
            {
                webView.Height = 480;
            }
        }

        private static string WrapDocument(string body)
        {
            var sb = new StringBuilder();
            sb.AppendLine("<!DOCTYPE html><html><head><meta charset=\"utf-8\">");
            sb.AppendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
            sb.AppendLine("<style>");
            sb.AppendLine("*{box-sizing:border-box}");
            sb.AppendLine("html,body{overflow:hidden;height:auto}");
            sb.AppendLine("body{margin:0;padding:0;background:#0B0B0D;color:#AEAEB2;font:13px/1.55 'Segoe UI',system-ui,sans-serif;word-wrap:break-word}");
            sb.AppendLine("h1,h2,h3,h4,h5,h6{color:#fff;margin:1em 0 .5em;font-weight:600}");
            sb.AppendLine("a{color:#4DA3FF;text-decoration:none}a:hover{text-decoration:underline}");
            sb.AppendLine("img{max-width:100%;height:auto;border-radius:8px}");
            sb.AppendLine("p,ul,ol{margin:.65em 0}li{margin:.2em 0}");
            sb.AppendLine("code,pre{background:#161618;border-radius:6px;font-size:12px}");
            sb.AppendLine("code{padding:2px 6px}pre{padding:10px;overflow:auto}");
            sb.AppendLine("table{border-collapse:collapse;width:100%}td,th{border:1px solid #2A2A2E;padding:6px 8px}");
            sb.AppendLine("blockquote{border-left:3px solid #2A2A2E;margin:.8em 0;padding-left:12px;color:#8E8E93}");
            sb.AppendLine("</style></head><body>");
            sb.Append(body);
            sb.AppendLine("</body></html>");
            return sb.ToString();
        }
    }
}
