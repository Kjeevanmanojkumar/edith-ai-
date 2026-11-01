package com.edith.assistant.forge

object ForgeHtml {

    fun stripFences(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```html", ignoreCase = true)) {
            text = text.substring(7)
        } else if (text.startsWith("```")) {
            text = text.substring(3)
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length - 3)
        }
        return text.trim()
    }

    fun looksLikeHtml(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower.contains("<!doctype html") ||
                lower.contains("<html") ||
                (lower.contains("<head") && lower.contains("<body"))
    }

    fun repairTruncated(html: String): String {
        val stripped = stripFences(html)
        if (stripped.contains("</html>", ignoreCase = true)) {
            return stripped
        }

        val sb = StringBuilder(stripped)
        val lower = stripped.lowercase()

        // Close open script tags if missing
        val openScripts = Regex("<script\\b", RegexOption.IGNORE_CASE).findAll(lower).count()
        val closeScripts = Regex("</script>", RegexOption.IGNORE_CASE).findAll(lower).count()
        if (openScripts > closeScripts) {
            sb.append("\n</script>")
        }

        // Close open style tags if missing
        val openStyles = Regex("<style\\b", RegexOption.IGNORE_CASE).findAll(lower).count()
        val closeStyles = Regex("</style>", RegexOption.IGNORE_CASE).findAll(lower).count()
        if (openStyles > closeStyles) {
            sb.append("\n</style>")
        }

        if (!lower.contains("</body>", ignoreCase = true)) {
            sb.append("\n</body>")
        }
        if (!lower.contains("</html>", ignoreCase = true)) {
            sb.append("\n</html>")
        }

        return sb.toString()
    }

    fun withSafetyNet(html: String): String {
        val safetyNetScript = """
<script id="forge-safety-net">
(function() {
    if (window.__forgeSafetyNetInstalled) return;
    window.__forgeSafetyNetInstalled = true;
    setTimeout(function() {
        var elements = document.querySelectorAll('body *:not(script):not(style):not(canvas)');
        for (var i = 0; i < elements.length; i++) {
            var el = elements[i];
            var style = window.getComputedStyle(el);
            if (style.opacity === '0' || style.visibility === 'hidden') {
                if (style.position !== 'fixed' && el.getAttribute('aria-hidden') !== 'true') {
                    el.style.opacity = '1';
                    el.style.visibility = 'visible';
                }
            }
        }
    }, 1400);
})();
</script>
""".trimIndent()

        return if (html.contains("</body>", ignoreCase = true)) {
            html.replace("</body>", "$safetyNetScript\n</body>", ignoreCase = true)
        } else {
            "$html\n$safetyNetScript"
        }
    }
}
