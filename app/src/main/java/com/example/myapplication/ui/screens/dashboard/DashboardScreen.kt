package com.example.myapplication.ui.screens.dashboard

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.ScanResult
import com.example.myapplication.data.model.ScanType
import com.example.myapplication.viewmodel.DashboardViewModel
import com.example.myapplication.receiver.SmsReceiver

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

                val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true

                if (state.isProtectionActive && (!hasSms || !hasNotif)) {
                    viewModel.toggleProtection(false)
                    setSmsReceiverEnabled(context, false)
                    Toast.makeText(context, "Permissions revoked. Protection Paused.", Toast.LENGTH_LONG).show()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true &&
                permissions[Manifest.permission.READ_SMS] == true

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        if (smsGranted && notifGranted) {
            viewModel.toggleProtection(true)
            setSmsReceiverEnabled(context, true)
        } else {
            viewModel.toggleProtection(false)
            setSmsReceiverEnabled(context, false)
            Toast.makeText(context, "Background protection requires all permissions.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "System Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProtectionStatusCard(
            isActive = state.isProtectionActive,
            onToggle = { isChecked ->
                if (isChecked) {
                    val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                } else {
                    viewModel.toggleProtection(false)
                    setSmsReceiverEnabled(context, false)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Manual Link Scanner",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TextField(
            value = state.manualUrlInput,
            onValueChange = { viewModel.updateUrlInput(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            placeholder = {
                Text(
                    "Paste link to check security...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.manualUrlInput.isNotEmpty() && !state.isScanningUrl) {
                        IconButton(onClick = { viewModel.updateUrlInput("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                        }
                    }

                    if (state.manualUrlInput.isNotEmpty()) {
                        Surface(
                            onClick = { viewModel.scanManualUrl() },
                            enabled = !state.isScanningUrl,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.padding(end = 8.dp).size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (state.isScanningUrl) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Run Scan", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            shape = CircleShape,
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        state.urlScanResult?.let { result ->
            val resultColor = if (state.isUrlSafe) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            Card(
                colors = CardDefaults.cardColors(containerColor = resultColor.copy(alpha = 0.1f)),
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                border = BorderStroke(1.dp, resultColor.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (state.isUrlSafe) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = resultColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = result, color = resultColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Recent Manual Scans", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)

            if (state.manualScans.isNotEmpty()) {
                IconButton(onClick = {
                    viewModel.clearAllHistory()
                    Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.manualScans.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "No manual scans performed yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.manualScans) { scan ->

                    // We extract the URL and display details logic here
                    // so we can pass clean data to the Card and Dialog
                    val targetUrl = if (scan.isSafe) {
                        scan.analysisDetails
                    } else {
                        scan.analysisDetails.substringAfter("URL: ", scan.analysisDetails)
                    }

                    val displayDetails = if (scan.isSafe) {
                        scan.analysisDetails
                    } else {
                        scan.analysisDetails.substringBefore(" | URL:")
                    }

                    DashboardThreatItem(
                        scan = scan.copy(analysisDetails = displayDetails),
                        scannedUrl = targetUrl
                    )
                }
            }
        }
    }
}


@Composable
fun ProtectionStatusCard(isActive: Boolean, onToggle: (Boolean) -> Unit) {
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val statusText = if (isActive) "Background Protection Active" else "Protection Paused"
    val subText = if (isActive) "Actively analyzing incoming SMS" else "Background scanning is offline"

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = statusText, style = MaterialTheme.typography.titleMedium, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subText, style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.85f))
            }
            Switch(checked = isActive, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun DashboardThreatItem(scan: ScanResult, scannedUrl: String) {
    // State to control the visibility of the popup dialog
    var showDialog by remember { mutableStateOf(false) }

    val safetyIcon = if (scan.isSafe) Icons.Default.CheckCircle else Icons.Default.Warning
    val typeIcon = if (scan.type == ScanType.URL) Icons.Default.Link else Icons.Default.Email
    val iconColor = if (scan.isSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    val titleText = when (scan.type) {
        ScanType.URL if scan.isSafe -> "Safe Link Verified"
        ScanType.URL if true -> "Malicious URL Blocked"
        ScanType.SMS if scan.isSafe -> "Safe Message Scanned"
        else -> "Suspicious Message Intercepted"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }, // Opens the dialog on click
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Icon(typeIcon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(28.dp).padding(bottom = 4.dp, end = 4.dp))
                Icon(safetyIcon, null, tint = iconColor, modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titleText, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = scan.analysisDetails, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    // The Popup Dialog
    if (showDialog) {
        ScanReportDialog(
            scan = scan,
            scannedUrl = scannedUrl,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun ScanReportDialog(scan: ScanResult, scannedUrl: String, onDismiss: () -> Unit) {
    val isSafe = scan.isSafe
    val titleColor = if (isSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val statusText = if (isSafe) "VERIFIED SECURE" else "CRITICAL THREAT"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Row with Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan Report",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Detail Rows
                ReportDetailRow(label = "Target", value = scannedUrl)
                ReportDetailRow(
                    label = "Security Status",
                    value = statusText,
                    valueColor = titleColor
                )
                ReportDetailRow(label = "Analysis Details", value = scan.analysisDetails)
                ReportDetailRow(label = "Confidence Score", value = "${scan.confidence}%")
                ReportDetailRow(label = "Scan ID", value = scan.id, isSmall = true)
                ReportDetailRow(label = "Source", value = if (scan.type == ScanType.URL) "Manual Web Scan" else "SMS Intercept", isSmall = true)
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

private fun setSmsReceiverEnabled(context: Context, isEnabled: Boolean) {
    val componentName = ComponentName(context, SmsReceiver::class.java)
    val packageManager = context.packageManager
    val state = if (isEnabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    packageManager.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP)
}