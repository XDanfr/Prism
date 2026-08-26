package me.xdan.prism.ui.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.xdan.prism.model.AppConfig
import me.xdan.prism.model.NavigationMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onGenerate: (AppConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf("") }
    var appName by remember { mutableStateOf("") }
    var iconUri by remember { mutableStateOf<Uri?>(null) }
    var accentColor by remember { mutableStateOf(Color(0xFF6750A4)) }
    var navMode by remember { mutableStateOf(NavigationMode.WEB_VIEW) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        iconUri = uri
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Prism", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            Text(
                text = "Create your WebAPK",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Target Web URL") },
                placeholder = { Text("https://example.com") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                shape = RoundedCornerShape(24.dp)
            )

            OutlinedTextField(
                value = appName,
                onValueChange = { appName = it },
                label = { Text("App Name") },
                placeholder = { Text("My Awesome App") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                shape = RoundedCornerShape(24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("App Icon", style = MaterialTheme.typography.titleMedium)
                    
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { launcher.launch("image/*") }
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (iconUri != null) {
                            AsyncImage(
                                model = iconUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Button(
                        onClick = { launcher.launch("image/*") },
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text(if (iconUri == null) "Pick from Gallery" else "Change Icon")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Accent Color", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val colors = listOf(
                        Color(0xFF6750A4), Color(0xFFB58392), Color(0xFF625B71),
                        Color(0xFF7D5260), Color(0xFF006A6A), Color(0xFF984061)
                    )
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { accentColor = color }
                                .border(
                                    width = if (accentColor == color) 3.dp else 0.dp,
                                    color = if (accentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Navigation Mode", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = navMode == NavigationMode.WEB_VIEW,
                        onClick = { navMode = NavigationMode.WEB_VIEW },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("WebView")
                    }
                    SegmentedButton(
                        selected = navMode == NavigationMode.CHROME_CUSTOM_TABS,
                        onClick = { navMode = NavigationMode.CHROME_CUSTOM_TABS },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Custom Tabs")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onGenerate(
                        AppConfig(
                            targetUrl = url,
                            appName = appName,
                            iconUri = iconUri,
                            accentColor = accentColor.toArgb(),
                            navMode = navMode
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                enabled = url.isNotBlank() && appName.isNotBlank(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Generate App", style = MaterialTheme.typography.titleLarge)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
}
