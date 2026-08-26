package me.xdan.prism.template

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class HostBlocker {
    private val blockedHosts = ConcurrentHashMap.newKeySet<String>()

    fun loadAsync(sourceIds: Set<String>, customUrls: List<String>) {
        Thread {
            (sourceIds.flatMap(::urlsFor) + customUrls).distinct().forEach { url ->
                runCatching { downloadHosts(url).forEach(blockedHosts::add) }
            }
        }.start()
    }

    fun shouldBlock(request: WebResourceRequest): Boolean {
        val host = request.url.host?.lowercase() ?: return false
        return blockedHosts.any { blocked -> host == blocked || host.endsWith(".$blocked") }
    }

    fun emptyResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain", "UTF-8", 204, "No Content", emptyMap(), ByteArrayInputStream(ByteArray(0))
    )

    private fun urlsFor(id: String): List<String> = when (id) {
        "steven-black" -> listOf("https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts")
        "adaway" -> listOf("https://adaway.org/hosts.txt")
        "urlhaus" -> listOf("https://urlhaus.abuse.ch/downloads/hostfile/")
        else -> emptyList()
    }

    private fun downloadHosts(url: String): Set<String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "PrismWebApp/1.0")
        return try {
            if (connection.responseCode !in 200..299) return emptySet()
            connection.inputStream.bufferedReader().useLines { lines -> lines.mapNotNull(::parseHost).toSet() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseHost(line: String): String? {
        val clean = line.substringBefore('#').trim()
        if (clean.isEmpty()) return null
        val parts = clean.split(Regex("\\s+"))
        val host = when {
            parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1" || parts[0] == "::1") -> parts[1]
            parts.size == 1 && parts[0].contains('.') -> parts[0]
            else -> return null
        }.lowercase().trimEnd('.')
        return if (host == "localhost" || host.startsWith("127.") || host.contains(':')) null else host
    }
}
