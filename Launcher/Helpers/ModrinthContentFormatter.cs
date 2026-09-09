using System.Text.RegularExpressions;

namespace Launcher.Helpers
{
    public static class ModrinthContentFormatter
    {
        private static readonly Regex MarkdownImageRegex = new(
            @"!\[([^\]]*)\]\(([^)]+)\)",
            RegexOptions.Compiled);

        private static readonly Regex MarkdownLinkRegex = new(
            @"\[([^\]]+)\]\(([^)]+)\)",
            RegexOptions.Compiled);

        public static string ToDisplayHtml(string? content)
        {
            if (string.IsNullOrWhiteSpace(content))
                return "<p>No content available.</p>";

            var html = content.Replace("\r\n", "\n");

            html = MarkdownImageRegex.Replace(html, "<img src=\"$2\" alt=\"$1\">");
            html = Regex.Replace(html, @"^###### (.+)$", "<h6>$1</h6>", RegexOptions.Multiline);
            html = Regex.Replace(html, @"^##### (.+)$", "<h5>$1</h5>", RegexOptions.Multiline);
            html = Regex.Replace(html, @"^#### (.+)$", "<h4>$1</h4>", RegexOptions.Multiline);
            html = Regex.Replace(html, @"^### (.+)$", "<h3>$1</h3>", RegexOptions.Multiline);
            html = Regex.Replace(html, @"^## (.+)$", "<h2>$1</h2>", RegexOptions.Multiline);
            html = Regex.Replace(html, @"^# (.+)$", "<h1>$1</h1>", RegexOptions.Multiline);
            html = Regex.Replace(html, @"\*\*([^*]+)\*\*", "<strong>$1</strong>");
            html = Regex.Replace(html, @"(?<!\*)\*([^*]+)\*(?!\*)", "<em>$1</em>");
            html = MarkdownLinkRegex.Replace(html, "<a href=\"$2\">$1</a>");

            if (!LooksLikeHtml(html))
            {
                html = string.Join(
                    "",
                    html.Split("\n\n", StringSplitOptions.RemoveEmptyEntries)
                        .Select(p => $"<p>{p.Replace("\n", "<br>")}</p>"));
            }

            return html;
        }

        private static bool LooksLikeHtml(string text)
            => text.Contains('<') && text.Contains('>');
    }
}
