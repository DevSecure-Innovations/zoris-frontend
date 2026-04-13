package com.example.myapplication.ui.screens.threats

// --- Core Compose Imports ---
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Data model for the UI
data class ThreatMessage(val id: Int, val sender: String, val snippet: String, val time: String)

@Composable
fun ThreatsScreen() {
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    val detectedThreats = emptyList<ThreatMessage>()

    // --- FILE EXPORT LAUNCHERS ---
    // These open the native Android "Save As" screen
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val csvData = generateCsvData(detectedThreats)
            saveFileToUri(context, it, csvData)
        }
    }

    val txtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let {
            val txtData = generateTxtData(detectedThreats)
            saveFileToUri(context, it, txtData)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- HEADER WITH EXPORT BUTTON ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Detected Threats", style = MaterialTheme.typography.headlineMedium)
                // Updated to semantic color for dark mode support
                Text(
                    "Messages flagged as scams.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Export Menu Button
            Box {
                IconButton(onClick = { showExportMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Export Options")
                }
                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Export as CSV (Excel)") },
                        onClick = {
                            showExportMenu = false
                            csvLauncher.launch("PhishGuard_Report.csv")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as TXT") },
                        onClick = {
                            showExportMenu = false
                            txtLauncher.launch("PhishGuard_Report.txt")
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- LIST CONTENT ---
        if (detectedThreats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No Threats",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("0 Threats Detected", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Waiting for backend data...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(detectedThreats) { threat ->
                    ThreatCard(threat)
                }
            }
        }
    }
}

// --- UI CARD COMPONENT (DARK MODE FIXED) ---
@Composable
fun ThreatCard(threat: ThreatMessage) {
    Card(
        // Semantic colors automatically switch based on Light/Dark mode!
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = threat.sender, fontWeight = FontWeight.Bold)
                    Text(
                        text = threat.time,
                        style = MaterialTheme.typography.bodySmall,
                        // Automatically fades the correct text color
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = threat.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- DATA FORMATTING HELPERS ---

fun generateCsvData(threats: List<ThreatMessage>): String {
    val builder = StringBuilder()
    builder.append("ID,Sender,Time,Snippet\n")

    for (threat in threats) {
        val safeSnippet = threat.snippet.replace("\"", "\"\"")
        builder.append("${threat.id},\"${threat.sender}\",\"${threat.time}\",\"${safeSnippet}\"\n")
    }
    return builder.toString()
}

fun generateTxtData(threats: List<ThreatMessage>): String {
    val builder = StringBuilder()
    builder.append("--- PHISHGUARD THREAT REPORT ---\n\n")

    for (threat in threats) {
        builder.append("Sender: ${threat.sender}\n")
        builder.append("Time: ${threat.time}\n")
        builder.append("Message: ${threat.snippet}\n")
        builder.append("--------------------------------\n")
    }
    return builder.toString()
}

// --- FILE WRITING HELPER ---
fun saveFileToUri(context: Context, uri: Uri, content: String) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.bufferedWriter().use { writer ->
                writer.write(content)
            }
        }
        Toast.makeText(context, "Export Successful", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to export file", Toast.LENGTH_LONG).show()
    }
}