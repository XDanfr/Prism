package me.xdan.prism.template

import android.app.Activity
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private val hostBlocker = HostBlocker()
    private var scriptIds: Set<String> = emptySet()
    private var scriptUrls: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = readConfig()
        scriptIds = config.readStringSet("userScripts")
        scriptUrls = config.readStringList("userScriptUrls")
        val blocklistIds = config.readStringSet("blocklists")
        val blocklistUrls = config.readStringList("blocklistUrls")
        hostBlocker.loadAsync(blocklistIds, blocklistUrls)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): android.webkit.WebResourceResponse? =
                    if (hostBlocker.shouldBlock(request)) hostBlocker.emptyResponse() else super.shouldInterceptRequest(view, request)

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    UserScriptEngine.inject(view, scriptIds, scriptUrls)
                }
            }
        }

        setContentView(webView)
        webView.loadUrl(config.optString("targetUrl").ifBlank { "https://example.com" })
    }

    private fun readConfig(): JSONObject = runCatching {
        assets.open("prism-config.json").bufferedReader().use { JSONObject(it.readText()) }
    }.getOrElse { JSONObject() }

    private fun JSONObject.readStringSet(key: String): Set<String> = readStringList(key).toSet()

    private fun JSONObject.readStringList(key: String): List<String> = optJSONArray(key)?.let { array ->
        buildList { for (i in 0 until array.length()) add(array.optString(i)) }
    } ?: emptyList()

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
