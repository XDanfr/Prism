package me.xdan.prism.ui.result

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ResultScreen(
    success: Boolean,
    errorMessage: String? = null,
    onInstall: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = if (success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (success) "App Ready!" else "Something went wrong",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (success) {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Install Now")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Go Back")
            }
        }
    }
}
