package me.xdan.prism.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Prism", style = MaterialTheme.typography.displaySmall)
        Text(
            "Turn websites into proper Android apps, directly on your device.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Feature(Icons.Default.Apps, "Standalone apps", "Each generated app gets its own Android package and launcher entry.")
            Feature(Icons.Default.Palette, "Material You icons", "Use the phone's dynamic colour as the default, or choose your own accent.")
            Feature(Icons.Default.Security, "Per-app control", "Choose isolated or shared browser data, scripts and blocklists for each app.")
        }

        Button(
            onClick = onComplete,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text("Get started")
        }
    }
}

@Composable
private fun Feature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
