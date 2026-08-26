package me.xdan.prism

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import me.xdan.prism.navigation.Route
import me.xdan.prism.ui.CreateAppViewModel
import me.xdan.prism.ui.dashboard.DashboardScreen
import me.xdan.prism.ui.progress.ProgressScreen
import me.xdan.prism.ui.result.ResultScreen
import me.xdan.prism.ui.theme.PrismTheme
import me.xdan.prism.util.InstallManager
import java.io.File
import me.xdan.prism.model.AppConfig
import me.xdan.prism.model.NavigationMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrismTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val backStack = rememberNavBackStack(Route.Dashboard)
    val installManager = InstallManager(androidx.compose.ui.platform.LocalContext.current)

    Surface(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
        ) { key ->
            when (key) {
                is Route.Dashboard -> {
                    NavEntry(key) {
                        DashboardScreen(
                            onGenerate = { config ->
                                backStack.add(
                                    Route.Progress(
                                        url = config.targetUrl,
                                        appName = config.appName,
                                        iconUri = config.iconUri?.toString(),
                                        accentColor = config.accentColor,
                                        navMode = config.navMode.name
                                    )
                                )
                            }
                        )
                    }
                }
                is Route.Progress -> {
                    NavEntry(key) {
                        val viewModel: CreateAppViewModel = viewModel()
                        val uiState by viewModel.uiState.collectAsState()
                        
                        LaunchedEffect(Unit) {
                            viewModel.startCompilation(
                                AppConfig(
                                    targetUrl = key.url,
                                    appName = key.appName,
                                    iconUri = key.iconUri?.let { android.net.Uri.parse(it) },
                                    accentColor = key.accentColor,
                                    navMode = NavigationMode.valueOf(key.navMode)
                                )
                            )
                        }

                        when (val state = uiState) {
                            is CreateAppViewModel.CompilationProgress.Idle -> {
                                ProgressScreen(step = "Initializing...", progress = 0f)
                            }
                            is CreateAppViewModel.CompilationProgress.Running -> {
                                ProgressScreen(step = state.step, progress = state.progress)
                            }
                            is CreateAppViewModel.CompilationProgress.Finished -> {
                                LaunchedEffect(state) {
                                    backStack.removeLastOrNull()
                                    backStack.add(
                                        Route.Result(
                                            success = state.success,
                                            apkPath = state.apkFile?.absolutePath,
                                            errorMessage = state.error
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                is Route.Result -> {
                    NavEntry(key) {
                        ResultScreen(
                            success = key.success,
                            errorMessage = key.errorMessage,
                            onInstall = {
                                key.apkPath?.let { path ->
                                    installManager.installApk(File(path))
                                }
                            },
                            onBack = {
                                backStack.removeLastOrNull()
                            }
                        )
                    }
                }
                else -> NavEntry(key) {
                    Text("Unknown route")
                }
            }
        }
    }
}
