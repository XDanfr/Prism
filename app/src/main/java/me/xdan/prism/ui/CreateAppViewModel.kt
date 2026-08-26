package me.xdan.prism.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.xdan.prism.compiler.BinaryCompilerEngine
import me.xdan.prism.compiler.IconPipeline
import me.xdan.prism.model.AppConfig
import me.xdan.prism.util.AppConfigDto
import me.xdan.prism.util.AppConfigManager
import me.xdan.prism.util.PackageNameGenerator
import java.io.File
import java.io.FileOutputStream

class CreateAppViewModel(application: Application) : AndroidViewModel(application) {
    private val compiler = BinaryCompilerEngine(application)
    private val context = application
    private val configManager = AppConfigManager(application)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val configAdapter = moshi.adapter(AppConfigDto::class.java)

    private val _uiState = MutableStateFlow<CompilationProgress>(CompilationProgress.Idle)
    val uiState: StateFlow<CompilationProgress> = _uiState.asStateFlow()

    sealed class CompilationProgress {
        data object Idle : CompilationProgress()
        data class Running(val step: String, val progress: Float) : CompilationProgress()
        data class Finished(val success: Boolean, val apkFile: File? = null, val error: String? = null) : CompilationProgress()
    }

    fun startCompilation(config: AppConfig) {
        if (config.targetUrl.isBlank() || config.appName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                _uiState.value = CompilationProgress.Running("Preparing app…", 0.1f)
                val existingPackages = context.packageManager
                    .getInstalledApplications(android.content.pm.PackageManager.MATCH_ALL)
                    .asSequence()
                    .map { it.packageName }
                    .toSet()
                val targetPackage = if (config.packageName.startsWith("me.xdan.prism.")) {
                    PackageNameGenerator.ensureUnique(config.packageName, existingPackages::contains)
                } else {
                    PackageNameGenerator.ensureUnique(PackageNameGenerator.forUrl(config.targetUrl), existingPackages::contains)
                }
                val resolvedConfig = config.copy(packageName = targetPackage)

                val iconInput = when {
                    resolvedConfig.iconUri != null -> {
                        val file = File(context.cacheDir, "temp_icon_${System.currentTimeMillis()}")
                        context.contentResolver.openInputStream(resolvedConfig.iconUri)?.use { input ->
                            FileOutputStream(file).use { output -> input.copyTo(output) }
                        } ?: throw IllegalStateException("Unable to read selected icon")
                        val mime = context.contentResolver.getType(resolvedConfig.iconUri)
                        if (mime == "image/svg+xml" || resolvedConfig.iconUri.toString().contains(".svg", ignoreCase = true)) {
                            IconPipeline.IconInput.Svg(file)
                        } else {
                            IconPipeline.IconInput.Png(file)
                        }
                    }
                    else -> IconPipeline.IconInput.Url(resolvedConfig.targetUrl)
                }

                _uiState.value = CompilationProgress.Running("Patching APK…", 0.35f)
                val configDto = AppConfigDto(
                    packageName = resolvedConfig.packageName,
                    targetUrl = resolvedConfig.targetUrl,
                    appName = resolvedConfig.appName,
                    iconUri = resolvedConfig.iconUri?.toString(),
                    faviconUrl = resolvedConfig.faviconUrl,
                    accentColor = resolvedConfig.accentColor,
                    accentSource = resolvedConfig.accentSource.name,
                    navMode = resolvedConfig.navMode.name,
                    sandboxed = resolvedConfig.sandboxed,
                    sharedProfileId = resolvedConfig.sharedProfileId,
                    userScripts = resolvedConfig.userScripts,
                    userScriptUrls = resolvedConfig.userScriptUrls,
                    blocklists = resolvedConfig.blocklists,
                    blocklistUrls = resolvedConfig.blocklistUrls
                )
                val result = compiler.compile(
                    baseApkAssetPath = "generated/base-release.apk",
                    targetPackageName = resolvedConfig.packageName,
                    targetAppName = resolvedConfig.appName,
                    targetUrl = resolvedConfig.targetUrl,
                    iconInput = iconInput,
                    iconBackgroundColor = String.format("#%06X", 0xFFFFFF and resolvedConfig.accentColor),
                    configJson = configAdapter.toJson(configDto),
                    oldPackageHint = "me.xdan.prism.template"
                )
                if (result.success) {
                    configManager.saveConfig(resolvedConfig.packageName, resolvedConfig)
                    _uiState.value = CompilationProgress.Finished(true, result.outputApk)
                } else {
                    _uiState.value = CompilationProgress.Finished(false, error = result.error)
                }
            }.onFailure { error ->
                _uiState.value = CompilationProgress.Finished(false, error = error.message ?: error.toString())
            }
        }
    }

    fun reset() {
        _uiState.value = CompilationProgress.Idle
    }
}
