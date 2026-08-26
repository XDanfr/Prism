package me.xdan.prism.util

import android.content.Context
import android.net.Uri
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.xdan.prism.model.AccentSource
import me.xdan.prism.model.AppConfig
import me.xdan.prism.model.NavigationMode
import java.io.File

class AppConfigManager(private val context: Context) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(AppConfigDto::class.java)
    private val configDir = File(context.filesDir, "app_configs").apply { mkdirs() }

    fun saveConfig(packageName: String, config: AppConfig) {
        val safePackage = packageName.replace(Regex("[^A-Za-z0-9._]"), "_")
        val dto = AppConfigDto(
            packageName = packageName,
            targetUrl = config.targetUrl,
            appName = config.appName,
            iconUri = config.iconUri?.toString(),
            faviconUrl = config.faviconUrl,
            accentColor = config.accentColor,
            accentSource = config.accentSource.name,
            navMode = config.navMode.name,
            sandboxed = config.sandboxed,
            sharedProfileId = config.sharedProfileId,
            userScripts = config.userScripts,
            userScriptUrls = config.userScriptUrls,
            blocklists = config.blocklists,
            blocklistUrls = config.blocklistUrls
        )
        File(configDir, "$safePackage.json").writeText(adapter.toJson(dto))
    }

    fun loadConfig(packageName: String): AppConfig? {
        val safePackage = packageName.replace(Regex("[^A-Za-z0-9._]"), "_")
        val file = File(configDir, "$safePackage.json")
        if (!file.exists()) return null

        return runCatching {
            val dto = adapter.fromJson(file.readText()) ?: return null
            AppConfig(
                packageName = dto.packageName.ifBlank { packageName },
                targetUrl = dto.targetUrl,
                appName = dto.appName,
                iconUri = dto.iconUri?.let(Uri::parse),
                faviconUrl = dto.faviconUrl,
                accentColor = dto.accentColor,
                accentSource = runCatching { AccentSource.valueOf(dto.accentSource) }.getOrDefault(AccentSource.MATERIAL_YOU),
                navMode = runCatching { NavigationMode.valueOf(dto.navMode) }.getOrDefault(NavigationMode.WEB_VIEW),
                sandboxed = dto.sandboxed,
                sharedProfileId = dto.sharedProfileId,
                userScripts = dto.userScripts,
                userScriptUrls = dto.userScriptUrls,
                blocklists = dto.blocklists,
                blocklistUrls = dto.blocklistUrls
            )
        }.getOrNull()
    }

    fun listConfigs(): List<AppConfig> = configDir
        .listFiles { file -> file.extension == "json" }
        ?.mapNotNull { loadConfig(it.nameWithoutExtension) }
        ?.sortedBy { it.appName.lowercase() }
        ?: emptyList()

    fun deleteConfig(packageName: String) {
        val safePackage = packageName.replace(Regex("[^A-Za-z0-9._]"), "_")
        File(configDir, "$safePackage.json").delete()
    }
}

@JsonClass(generateAdapter = true)
data class AppConfigDto(
    val packageName: String = "",
    val targetUrl: String = "",
    val appName: String = "",
    val iconUri: String? = null,
    val faviconUrl: String? = null,
    val accentColor: Int = 0xFF6750A4.toInt(),
    val accentSource: String = AccentSource.MATERIAL_YOU.name,
    val navMode: String = NavigationMode.WEB_VIEW.name,
    val sandboxed: Boolean = true,
    val sharedProfileId: String? = null,
    val userScripts: List<String> = emptyList(),
    val userScriptUrls: List<String> = emptyList(),
    val blocklists: List<String> = emptyList(),
    val blocklistUrls: List<String> = emptyList()
)
