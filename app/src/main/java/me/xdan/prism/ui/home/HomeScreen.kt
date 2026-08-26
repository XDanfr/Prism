package me.xdan.prism.ui.home

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.xdan.prism.model.AppConfig
import me.xdan.prism.util.AppConfigManager

private const val PACKAGE_PREFIX = "me.xdan.prism."

data class PrismInstalledApp(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable,
    val config: AppConfig?
)

@Composable
fun HomeScreen(
    refreshKey: Int,
    onCreate: () -> Unit,
    onConfigure: (AppConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val configManager = remember { AppConfigManager(context) }
    var apps by remember { mutableStateOf<List<PrismInstalledApp>>(emptyList()) }

    fun refresh() {
        val configs = configManager.listConfigs().associateBy { it.packageName }
        apps = packageManager
            .getInstalledApplications(PackageManager.MATCH_ALL)
            .asSequence()
            .filter { it.packageName.startsWith(PACKAGE_PREFIX) && it.packageName != "me.xdan.prism" }
            .map {
                PrismInstalledApp(
                    packageName = it.packageName,
                    label = it.loadLabel(packageManager).toString(),
                    icon = it.loadIcon(packageManager),
                    config = configs[it.packageName]
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    LaunchedEffect(refreshKey) { refresh() }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Your apps", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (apps.isEmpty()) "Nothing made with Prism yet." else "${apps.size} Prism app${if (apps.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(onClick = onCreate) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Create")
            }
        }

        if (apps.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Web, contentDescription = null)
                    Text("Create your first app", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
                    Text("Enter a website, choose an icon and Prism will build a standalone APK.", modifier = Modifier.padding(top = 6.dp))
                    Button(onClick = onCreate, modifier = Modifier.padding(top = 18.dp)) { Text("Create an app") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.titleMedium)
                                Text(app.config?.targetUrl ?: app.packageName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                            IconButton(onClick = {
                                packageManager.getLaunchIntentForPackage(app.packageName)?.let(context::startActivity)
                            }) { Icon(Icons.Default.Web, contentDescription = "Open") }
                            IconButton(onClick = { app.config?.let(onConfigure) }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                            IconButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_DELETE).apply { data = Uri.parse("package:${app.packageName}") })
                                refresh()
                            }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Uninstall") }
                        }
                    }
                }
            }
        }
    }
}
