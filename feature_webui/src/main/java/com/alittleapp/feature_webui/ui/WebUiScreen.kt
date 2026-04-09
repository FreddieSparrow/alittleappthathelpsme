package com.alittleapp.feature_webui.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alittleapp.feature_webui.WebUiViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebUiScreen(viewModel: WebUiViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Go Live") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                if (state.isLive) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (state.isLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )

            Text(
                "Go Live — Browser Dashboard",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Text(
                "Start a local web server so you can view your notes and tasks in any browser on the same Wi-Fi network. Read-only. No internet required.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            if (!state.isLive) {
                if (state.isLoading) {
                    CircularProgressIndicator()
                    Text("Starting server…")
                } else {
                    Button(
                        onClick = viewModel::goLive,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Go Live")
                    }
                }
            } else {
                // Show URL + QR code
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FiberManualRecord, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Live", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }

                        Text(
                            state.url,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )

                        val qrBitmap = remember(state.url) { generateQrBitmap(state.url) }
                        qrBitmap?.let {
                            Card(modifier = Modifier.size(200.dp)) {
                                Image(it.asImageBitmap(), "QR",
                                    modifier = Modifier.fillMaxSize().padding(8.dp))
                            }
                        }

                        Text(
                            "Scan or type the URL in any browser on the same Wi-Fi",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::openInBrowser,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Open")
                    }
                    Button(
                        onClick = viewModel::stopLive,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }

            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            // Info card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Privacy note", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "The web dashboard is read-only and only accessible on your local Wi-Fi. " +
                        "Nothing is sent to the internet. Stop the server when not in use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int = 400): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.MARGIN to 1))
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size)
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        bitmap
    } catch (e: Exception) { null }
}
