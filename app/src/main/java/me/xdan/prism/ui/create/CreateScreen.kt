package me.xdan.prism.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.xdan.prism.model.AccentSource
import me.xdan.prism.model.AppConfig
import me.xdan.prism.model.NavigationMode
import me.xdan.prism.util.PackageNameGenerator
import java.net.URI

private val AccentChoices = listOf(
    Color(0xFF6750A4), Color(0xFF006A6A), Color(0xFF386A20),
    Color(0xFF8B5000), Color(0xFF984061), Color(0xFF4A5F7A)
)

private data class ToggleOption(val id: String, val title: String, val description: String)

private val ScriptOptions = listOf(
    ToggleOption("google-translate", "Google Translate", "Add translation controls to pages."),
    ToggleOption("disable-video-autoplay", "Disable video autoplay", "Prevent common sites from automatically playing media."),
    ToggleOption("force-copy", "Force text copying", "Make normally blocked text selection/copying possible."),
    ToggleOption("force-zoom", "Force page zoom", "Re-enable pinch and browser zoom where sites disable it."),
    ToggleOption("remove-footers", "Remove footers", "Hide repetitive or unwanted page footers.")
)

private val BlocklistOptions = listOf(
    ToggleOption("steven-black", "Steven Black hosts", "Large general-purpose host blocklist."),
    ToggleOption("adaway", "AdAway hosts", "Ad and tracker blocking hosts."),
    ToggleOption("urlhaus", "Abuse.ch URLhaus", "Known malicious URL blocking source.")
)

@Composable
fun CreateScreen(
    onGenerate: (AppConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf("") }
    var appName by remember { mutableStateOf("") }
    var iconUri by remember { mutableStateOf<Uri?>(null) }
    var iconMime by remember { mutableStateOf<String?>(null) }
    var accentSource by remember { mutableStateOf(AccentSource.MATERIAL_YOU) }
    var accentColor by remember { mutableStateOf(AccentChoices.first()) }
    var navMode by remember { mutableStateOf(NavigationMode.WEB_VIEW) }
    var sandboxed by remember { mutableStateOf(true) }
    var sharedProfileId by remember { mutableStateOf<String?>(null) }
    var scripts by remember { mutableStateOf(setOf<String>()) }
    var scriptUrls by remember { mutableStateOf(emptyList<String>()) }
    var blocklists by remember { mutableStateOf(setOf<String>()) }
    var blocklistUrls by remember { mutableStateOf(emptyList<String>()) }
    var customScriptUrl by remember { mutableStateOf("") }
    var customBlocklistUrl by remember { mutableStateOf("") }
    var nameTouched by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            iconUri = uri
            iconMime = null
        }
    }
    val svgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            iconUri = uri
            iconMime = "image/svg+xml"
        }
    }

    val faviconUrl = remember(url) {
        runCatching {
            val parsed = URI(url)
            if (parsed.scheme != "http" && parsed.scheme != "https" || parsed.host.isNullOrBlank()) null
            else "${parsed.scheme}://${parsed.host}/favicon.ico"
        }.getOrNull()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Create", style = MaterialTheme.typography.headlineMedium)
            Text("Make a website feel like an app.", style = MaterialTheme.typography.bodyLarge)
        }
        item {
            OutlinedTextField(
                value = url,
                onValueChange = { value ->
                    url = value
                    if (!nameTouched && appName.isBlank()) {
                        appName = runCatching { URI(value).host.orEmpty().removePrefix("www.").substringBefore('.').replaceFirstChar { it.uppercase() } }.getOrDefault("")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Website URL") },
                placeholder = { Text("https://example.com") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            OutlinedTextField(
                value = appName,
                onValueChange = { nameTouched = true; appName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("App name") },
                placeholder = { Text("My website") },
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Icon", style = MaterialTheme.typography.titleLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                iconUri != null -> AsyncImage(model = iconUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                                faviconUrl != null -> AsyncImage(model = faviconUrl, contentDescription = null, modifier = Modifier.size(56.dp))
                                else -> Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(42.dp))
                            }
                        }
                        Column(modifier = Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (iconUri != null) "Custom icon selected" else if (faviconUrl != null) "Favicon suggestion" else "Globe fallback")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { svgPicker.launch("image/svg+xml") }) { Icon(Icons.Default.Code, contentDescription = null); Text(" SVG") }
                                OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null); Text(" Image") }
                            }
                        }
                    }
                    Text(if (iconMime == "image/svg+xml") "SVG will be rendered into the adaptive and monochrome layers." else "PNG/JPEG images are used as the icon artwork. Prism will also generate the monochrome layer.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Accent colour", style = MaterialTheme.typography.titleLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = accentSource == AccentSource.MATERIAL_YOU, onClick = { accentSource = AccentSource.MATERIAL_YOU }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) { Text("Material You") }
                        SegmentedButton(selected = accentSource == AccentSource.CUSTOM, onClick = { accentSource = AccentSource.CUSTOM }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) { Text("Custom") }
                    }
                    if (accentSource == AccentSource.MATERIAL_YOU) {
                        Text("Uses the phone's current dynamic primary colour.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AccentChoices.forEach { choice ->
                                Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(choice).clickable { accentColor = choice }.border(if (choice == accentColor) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape))
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Security, contentDescription = null); Text("Privacy & navigation", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 12.dp)) }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) { Text("Sandbox this app"); Text("Keep cookies, local storage and browser data isolated.", style = MaterialTheme.typography.bodySmall) }
                        Switch(checked = sandboxed, onCheckedChange = { sandboxed = it })
                    }
                    if (!sandboxed) {
                        OutlinedTextField(value = sharedProfileId.orEmpty(), onValueChange = { sharedProfileId = it.ifBlank { null } }, modifier = Modifier.fillMaxWidth(), label = { Text("Shared profile name") }, placeholder = { Text("default") })
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = navMode == NavigationMode.WEB_VIEW, onClick = { navMode = NavigationMode.WEB_VIEW }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) { Text("WebView") }
                        SegmentedButton(selected = navMode == NavigationMode.CHROME_CUSTOM_TABS, onClick = { navMode = NavigationMode.CHROME_CUSTOM_TABS }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) { Text("Custom Tab") }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Tune, contentDescription = null); Text("User Scripts", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 12.dp)) }
                    ScriptOptions.forEach { option ->
                        FilterChip(selected = option.id in scripts, onClick = { scripts = if (option.id in scripts) scripts - option.id else scripts + option.id }, label = { Text(option.title) }, leadingIcon = if (option.id in scripts) ({ Icon(Icons.Default.Security, contentDescription = null) }) else null)
                        Text(option.description, style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    Text("Custom source", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = customScriptUrl, onValueChange = { customScriptUrl = it }, modifier = Modifier.weight(1f), label = { Text("Script URL") }, placeholder = { Text("GreasyFork .user.js URL") }, singleLine = true)
                        TextButton(onClick = { if (customScriptUrl.startsWith("http://") || customScriptUrl.startsWith("https://")) { scriptUrls = (scriptUrls + customScriptUrl.trim()).distinct(); customScriptUrl = "" } }) { Text("Add") }
                    }
                    scriptUrls.forEach { source -> Text("• $source", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ad blocking", style = MaterialTheme.typography.titleLarge)
                    BlocklistOptions.forEach { option ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) { Text(option.title); Text(option.description, style = MaterialTheme.typography.bodySmall) }
                            Switch(checked = option.id in blocklists, onCheckedChange = { blocklists = if (option.id in blocklists) blocklists - option.id else blocklists + option.id })
                        }
                    }
                    HorizontalDivider()
                    Text("Custom blocklist", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = customBlocklistUrl, onValueChange = { customBlocklistUrl = it }, modifier = Modifier.weight(1f), label = { Text("Hosts/blocklist URL") }, placeholder = { Text("https://…/hosts.txt") }, singleLine = true)
                        TextButton(onClick = { if (customBlocklistUrl.startsWith("http://") || customBlocklistUrl.startsWith("https://")) { blocklistUrls = (blocklistUrls + customBlocklistUrl.trim()).distinct(); customBlocklistUrl = "" } }) { Text("Add") }
                    }
                    blocklistUrls.forEach { source -> Text("• $source", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            val packageName = PackageNameGenerator.forUrl(url)
            val canBuild = (url.startsWith("http://") || url.startsWith("https://")) && appName.isNotBlank()
            val materialYouAccent = MaterialTheme.colorScheme.primary
            Button(
                onClick = {
                    val resolvedAccent = if (accentSource == AccentSource.CUSTOM) accentColor else materialYouAccent
                    onGenerate(AppConfig(
                        packageName = packageName,
                        targetUrl = url.trim(),
                        appName = appName.trim(),
                        iconUri = iconUri,
                        faviconUrl = faviconUrl,
                        accentColor = resolvedAccent.toArgb(),
                        accentSource = accentSource,
                        navMode = navMode,
                        sandboxed = sandboxed,
                        sharedProfileId = sharedProfileId,
                        userScripts = scripts.toList().sorted(),
                        userScriptUrls = scriptUrls,
                        blocklists = blocklists.toList().sorted(),
                        blocklistUrls = blocklistUrls
                    ))
                },
                enabled = canBuild,
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) { Text("Build app", style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
