package me.xdan.prism

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import me.xdan.prism.model.AppConfig
import me.xdan.prism.ui.CreateAppViewModel
import me.xdan.prism.ui.create.CreateScreen
import me.xdan.prism.ui.home.HomeScreen
import me.xdan.prism.ui.onboarding.OnboardingScreen
import me.xdan.prism.ui.onboarding.OnboardingViewModel
import me.xdan.prism.ui.settings.SettingsScreen
import me.xdan.prism.ui.theme.PrismTheme
import me.xdan.prism.util.AppConfigManager
import me.xdan.prism.util.InstallManager
import java.io.File

private enum class PrismTab { HOME, CREATE, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PrismTheme { PrismApp() } }
    }
}

@Composable
private fun PrismApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val onboardingViewModel: OnboardingViewModel = viewModel()
    val onboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState(initial = false)

    if (!onboardingCompleted) {
        OnboardingScreen(onComplete = onboardingViewModel::completeOnboarding)
    } else {
        PrismShell()
    }
}

@Composable
private fun PrismShell() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val createViewModel: CreateAppViewModel = viewModel()
    val compilationState by createViewModel.uiState.collectAsState()
    val installManager = remember { InstallManager(context) }
    val configManager = remember { AppConfigManager(context) }

    var tab by rememberSaveable { mutableStateOf(PrismTab.HOME) }
    var selectedConfig by remember { mutableStateOf<AppConfig?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }
    val compiling = compilationState !is CreateAppViewModel.CompilationProgress.Idle

    if (compiling) {
        when (val state = compilationState) {
            is CreateAppViewModel.CompilationProgress.Running -> {
                me.xdan.prism.ui.progress.ProgressScreen(step = state.step, progress = state.progress)
            }
            is CreateAppViewModel.CompilationProgress.Finished -> {
                me.xdan.prism.ui.result.ResultScreen(
                    success = state.success,
                    errorMessage = state.error,
                    onInstall = {
                        state.apkFile?.let(installManager::installApk)
                    },
                    onBack = {
                        createViewModel.reset()
                        refreshToken++
                        tab = PrismTab.HOME
                    }
                )
            }
            CreateAppViewModel.CompilationProgress.Idle -> Unit
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == PrismTab.HOME,
                    onClick = { tab = PrismTab.HOME },
                    icon = { androidx.compose.material3.Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == PrismTab.CREATE,
                    onClick = { tab = PrismTab.CREATE },
                    icon = { androidx.compose.material3.Icon(Icons.Default.AddCircleOutline, contentDescription = null) },
                    label = { Text("Create") }
                )
                NavigationBarItem(
                    selected = tab == PrismTab.SETTINGS,
                    onClick = { tab = PrismTab.SETTINGS },
                    icon = { androidx.compose.material3.Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        when (tab) {
            PrismTab.HOME -> HomeScreen(
                onCreate = { tab = PrismTab.CREATE },
                onConfigure = { selectedConfig = it },
                modifier = Modifier.padding(innerPadding)
            )
            PrismTab.CREATE -> CreateScreen(
                onGenerate = {
                    createViewModel.startCompilation(it)
                },
                modifier = Modifier.padding(innerPadding)
            )
            PrismTab.SETTINGS -> SettingsScreen(modifier = Modifier.padding(innerPadding))
        }
    }

    selectedConfig?.let { config ->
        AppSettingsDialog(
            config = config,
            onDismiss = { selectedConfig = null },
            onSave = { updated ->
                configManager.saveConfig(updated.packageName, updated)
                selectedConfig = null
                refreshToken++
            }
        )
    }
}

@Composable
private fun AppSettingsDialog(
    config: AppConfig,
    onDismiss: () -> Unit,
    onSave: (AppConfig) -> Unit
) {
    var sandboxed by remember(config.packageName) { mutableStateOf(config.sandboxed) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(config.appName) },
        text = {
            Column {
                Text(config.targetUrl, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text("Isolation", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("Keep cookies and local browser data isolated from other Prism apps.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    Switch(checked = sandboxed, onCheckedChange = { sandboxed = it })
                }
                Text("Package: ${config.packageName}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                Text("Scripts: ${config.userScripts.size} enabled", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                Text("Blocklists: ${config.blocklists.size} enabled", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(config.copy(sandboxed = sandboxed)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
