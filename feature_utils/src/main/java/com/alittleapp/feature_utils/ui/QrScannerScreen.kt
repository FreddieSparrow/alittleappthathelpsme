package com.alittleapp.feature_utils.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var scannedResult by remember { mutableStateOf<String?>(null) }
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (!result.contents.isNullOrBlank()) scannedResult = result.contents
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Scanner") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (cameraPermission.status.isGranted) {
                        scanLauncher.launch(
                            ScanOptions().apply {
                                setPrompt("Scan a QR code")
                                setBeepEnabled(false)
                                setOrientationLocked(false)
                            }
                        )
                    } else {
                        cameraPermission.launchPermissionRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan QR Code")
            }

            scannedResult?.let { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Result", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(result, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("QR Result", result))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy")
                            }
                            if (result.startsWith("http")) {
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result)))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Open")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
