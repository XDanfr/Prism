package me.xdan.prism.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.xdan.prism.ui.onboarding.dataStore

private val DefaultSandboxKey = booleanPreferencesKey("default_sandboxed")

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences by context.dataStore.data.collectAsStateWithLifecycle(
        initialValue = androidx.datastore.preferences.core.emptyPreferences()
    )
    val defaultSandboxed = preferences[DefaultSandboxKey] ?: true

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Text("Prism itself, rather than an individual web app.", style = MaterialTheme.typography.bodyLarge)
        }
        item {
            SettingCard(Icons.Default.Palette, "Material You", "Prism uses the phone's dynamic colour as the default accent for the manager and generated apps.")
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sandbox new apps", style = MaterialTheme.typography.titleMedium)
                        Text("New Prism apps start isolated from one another.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = defaultSandboxed,
                        onCheckedChange = { value ->
                            scope.launch { context.dataStore.edit { it[DefaultSandboxKey] = value } }
                        }
                    )
                }
            }
        }
        item {
            SettingCard(Icons.Default.Security, "Power features", "User scripts and host blocklists are configured per generated app, with Prism keeping the source choices in its local configuration.")
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                        Text("Support Prism", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                    }
                    Text("Prism is open source. Donations help keep the project moving.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    TextButton(onClick = { openUrl(context, "https://github.com/sponsors/XDanfr") }) {
                        Text("Donate on GitHub Sponsors")
                    }
                }
            }
        }
        item {
            SettingCard(Icons.Default.Info, "About Prism", "On-device web app creation by XDanfr. Generated apps use separate package identities so they never replace Prism itself.")
        }
    }
}

@Composable
private fun SettingCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null)
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
