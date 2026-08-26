package me.xdan.prism.util

import java.net.URI

object PackageNameGenerator {
    private const val PREFIX = "me.xdan.prism"

    fun forUrl(url: String): String {
        val host = runCatching { URI(url).host.orEmpty() }.getOrDefault("")
            .removePrefix("www.")
        val slug = host
            .split('.')
            .firstOrNull { it.isNotBlank() }
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9_]"), "")
            .orEmpty()
            .ifBlank { "webapp" }
            .let { if (it.first().isDigit()) "app$it" else it }
        return "$PREFIX.$slug"
    }

    fun ensureUnique(candidate: String, isInstalled: (String) -> Boolean): String {
        if (!isInstalled(candidate)) return candidate
        var index = 2
        while (isInstalled("$candidate$index")) index++
        return "$candidate$index"
    }
}
