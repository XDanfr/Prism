package me.xdan.prism.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    @Serializable
    data object Dashboard : Route

    @Serializable
    data class Progress(
        val url: String,
        val appName: String,
        val iconUri: String?,
        val accentColor: Int,
        val navMode: String
    ) : Route

    @Serializable
    data class Result(
        val success: Boolean,
        val apkPath: String? = null,
        val errorMessage: String? = null
    ) : Route
}
