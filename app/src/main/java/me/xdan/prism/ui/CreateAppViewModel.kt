package me.xdan.prism.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.xdan.prism.compiler.BinaryCompilerEngine
import me.xdan.prism.compiler.IconPipeline
import me.xdan.prism.model.AppConfig
import me.xdan.prism.model.NavigationMode
import java.io.File
import java.io.FileOutputStream

class CreateAppViewModel(application: Application) : AndroidViewModel(application) {

    private val compiler = BinaryCompilerEngine(application)
    private val context = application

    private val _uiState = MutableStateFlow<CompilationProgress>(CompilationProgress.Idle)
    val uiState: StateFlow<CompilationProgress> = _uiState.asStateFlow()

    sealed class CompilationProgress {
        data object Idle : CompilationProgress()
        data class Running(val step: String, val progress: Float) : CompilationProgress()
        data class Finished(val success: Boolean, val apkFile: File? = null, val error: String? = null) : CompilationProgress()
    }

    fun startCompilation(config: AppConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = CompilationProgress.Running("Initializing...", 0.1f)
            
            val iconInput = when {
                config.iconUri != null -> {
                    val file = File(context.cacheDir, "temp_icon_${System.currentTimeMillis()}")
                    context.contentResolver.openInputStream(config.iconUri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (config.iconUri.toString().endsWith(".svg")) {
                        IconPipeline.IconInput.Svg(file)
                    } else {
                        IconPipeline.IconInput.Png(file)
                    }
                }
                else -> IconPipeline.IconInput.Url(config.targetUrl)
            }

            _uiState.value = CompilationProgress.Running("Patching APK...", 0.4f)
            
            val hexColor = String.format("#%06X", 0xFFFFFF and config.accentColor)
            
            val result = compiler.compile(
                baseApkAssetPath = "base-release.apk", // Assuming this exists in assets
                targetPackageName = "me.prism.app.${System.currentTimeMillis()}",
                targetAppName = config.appName,
                targetUrl = config.targetUrl,
                iconInput = iconInput,
                iconBackgroundColor = hexColor
            )

            if (result.success) {
                _uiState.value = CompilationProgress.Finished(true, result.outputApk)
            } else {
                _uiState.value = CompilationProgress.Finished(false, error = result.error)
            }
        }
    }
}
