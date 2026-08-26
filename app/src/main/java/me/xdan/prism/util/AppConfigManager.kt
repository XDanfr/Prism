package me.xdan.prism.util

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.xdan.prism.model.AppConfig
import me.xdan.prism.model.NavigationMode
import java.io.File

class AppConfigManager(private val context: Context) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(AppConfigDto::class.java)

    private val configDir = File(context.filesDir, "app_configs").apply {
        if (!exists()) mkdirs()
    }

    fun saveConfig(packageName: String, config: AppConfig) {
        val dto = AppConfigDto(
            targetUrl = config.targetUrl,
            appName = config.appName,
            iconUri = config.iconUri?.toString(),
            accentColor = config.accentColor,
            navMode = config.navMode.name
        )
        val json = adapter.toJson(dto)
        File(configDir, "$packageName.json").writeText(json)
    }

    fun loadConfig(packageName: String): AppConfig? {
        val file = File(configDir, "$packageName.json")
        if (!file.exists()) return null
        return try {
            val json = file.readText()
            val dto = adapter.fromJson(json) ?: return null
            AppConfig(
                targetUrl = dto.targetUrl,
                appName = dto.appName,
                iconUri = dto.iconUri?.let { android.net.Uri.parse(it) },
                accentColor = dto.accentColor,
                navMode = NavigationMode.valueOf(dto.navMode)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun deleteConfig(packageName: String) {
        File(configDir, "$packageName.json").delete()
    }
}

data class AppConfigDto(
    val targetUrl: String,
    val appName: String,
    val iconUri: String?,
    val accentColor: Int,
    val navMode: String
)
