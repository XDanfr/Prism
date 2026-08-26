package me.xdan.prism.model

import android.net.Uri

enum class AccentSource {
    MATERIAL_YOU,
    CUSTOM
}

data class AppConfig(
    val packageName: String = "",
    val targetUrl: String = "",
    val appName: String = "",
    val iconUri: Uri? = null,
    val faviconUrl: String? = null,
    val accentColor: Int = 0xFF6750A4.toInt(),
    val accentSource: AccentSource = AccentSource.MATERIAL_YOU,
    val navMode: NavigationMode = NavigationMode.WEB_VIEW,
    val sandboxed: Boolean = true,
    val sharedProfileId: String? = null,
    val userScripts: List<String> = emptyList(),
    val blocklists: List<String> = emptyList()
)
