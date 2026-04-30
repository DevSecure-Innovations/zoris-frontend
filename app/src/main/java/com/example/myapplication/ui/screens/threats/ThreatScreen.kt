package com.example.myapplication.ui.screens.threats

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.data.model.ScanResult
import com.example.myapplication.data.model.ScanType
import com.example.myapplication.viewmodel.DashboardViewModel

@Composable
fun ThreatsScreen(viewModel: DashboardViewModel) {

    val context = LocalContext.current

    var showExportMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val state by viewModel.uiState.collectAsState()
    val detectedThreats = state.realtimeThreats

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("This will permanently delete all scanned history. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { saveFileToUri(context, it, generateCsvData(detectedThreats)) }
    }

    val txtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { saveFileToUri(context, it, generateTxtData(detectedThreats)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Detected Threats", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Intercepted malicious SMS messages.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box {
                IconButton(onClick = { showExportMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Export Options")
                }
                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Export as CSV") },
                        onClick = {
                            showExportMenu = false
                            csvLauncher.launch("Zoris_Report.csv")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as TXT") },
                        onClick = {
                            showExportMenu = false
                            txtLauncher.launch("Zoris_Report.txt")
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("Clear History", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showExportMenu = false
                            showClearDialog = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    Text("Your device is currently secure.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(detectedThreats) { threat ->
                    val sender = if (threat.analysisDetails.contains("From ")) {
                        threat.analysisDetails.substringAfter("From ").substringBefore(":")
                    } else "Unknown Sender"

                    val messageSnippet = if (threat.analysisDetails.contains(": ")) {
                        threat.analysisDetails.substringAfter(": ")
                    } else threat.analysisDetails

                    ThreatCard(
                        threat = threat.copy(analysisDetails = messageSnippet),
                        sourceTarget = sender
                    )
                }
            }
        }
    }
}

@Composable
fun ThreatCard(threat: ScanResult, sourceTarget: String) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true } // Opens the dialog
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
                    Text(text = "Suspicious SMS", fontWeight = FontWeight.Bold)

                    Text(
                        text = "Confidence: ${threat.confidence}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = threat.analysisDetails,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showDialog) {
        ScanReportDialog(
            scan = threat,
            scannedUrl = sourceTarget,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun ScanReportDialog(scan: ScanResult, scannedUrl: String, onDismiss: () -> Unit) {
    val isSafe = scan.isSafe
    val titleColor = if (isSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val statusText = if (isSafe) "VERIFIED SECURE" else "CRITICAL THREAT"
    val sourceText = if (scan.type == ScanType.URL) "Manual Web Scan" else "SMS Intercept"
    val targetLabel = if (scan.type == ScanType.URL) "Target URL" else "Sender ID"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Threat Report",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))

                ReportDetailRow(label = targetLabel, value = scannedUrl)
                ReportDetailRow(
                    label = "Security Status",
                    value = statusText,
                    valueColor = titleColor
                )
                ReportDetailRow(label = "Message Content", value = scan.analysisDetails)
                ReportDetailRow(label = "Confidence Score", value = "${scan.confidence}%")
                ReportDetailRow(label = "Scan ID", value = scan.id, isSmall = true)
                ReportDetailRow(label = "Source", value = sourceText, isSmall = true)
            }
        }
    }
}

@Composable
fun ReportDetailRow(label: String, value: String, valueColor: Color = Color.Unspecified, isSmall: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor,
            fontSize = if (isSmall) 11.sp else 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun generateCsvData(threats: List<ScanResult>): String {
    val builder = StringBuilder()
    builder.append("ID,Type,Confidence,Details\n")
    for (threat in threats) {
        val safeDetails = threat.analysisDetails.replace("\"", "\"\"")
        builder.append("${threat.id},\"${threat.type.name}\",\"${threat.confidence}%\",\"${safeDetails}\"\n")
    }
    return builder.toString()
}

fun generateTxtData(threats: List<ScanResult>): String {
    val builder = StringBuilder()
    builder.append("--- ZORIS THREAT REPORT ---\n\n")
    for (threat in threats) {
        builder.append("Type: ${threat.type.name}\n")
        builder.append("Confidence: ${threat.confidence}%\n")
        builder.append("Details: ${threat.analysisDetails}\n")
        builder.append("--------------------------------\n")
    }
    return builder.toString()
}

fun saveFileToUri(context: Context, uri: Uri, content: String) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
        Toast.makeText(context, "Export Successful", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to export", Toast.LENGTH_LONG).show()
    }
}