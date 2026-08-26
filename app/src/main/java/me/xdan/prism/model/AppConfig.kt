package me.xdan.prism.model

import android.net.Uri

data class AppConfig(
    val targetUrl: String = "",
    val appName: String = "",
    val iconUri: Uri? = null,
    val accentColor: Int = 0xFF6200EE.toInt(),
    val navMode: NavigationMode = NavigationMode.WEB_VIEW
)
