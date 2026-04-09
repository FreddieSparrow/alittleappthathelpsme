package com.alittleapp.feature_transfer.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alittleapp.feature_transfer.TransferState
import com.alittleapp.feature_transfer.TransferViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(viewModel: TransferViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.prepareSend(it) }
    }
    val qrScanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (!result.contents.isNullOrBlank()) {
            val saveDir = File(context.getExternalFilesDir(null), "Received")
            saveDir.mkdirs()
            viewModel.receiveFromQr(result.contents, saveDir)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Transfer") },
                actions = {
                    if (state !is TransferState.Idle) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Refresh, "Reset")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(targetState = state, label = "transfer_state") { currentState ->
                when (currentState) {
                    is TransferState.Idle -> IdlePanel(
                        onSend = { filePicker.launch("*/*") },
                        onReceive = {
                            qrScanner.launch(
                                ScanOptions().apply {
                                    setPrompt("Scan sender's QR code")
                                    setBeepEnabled(false)
                                }
                            )
                        }
                    )
                    is TransferState.GeneratingQr -> LoadingPanel("Preparing…")
                    is TransferState.WaitingForReceiver -> SenderQrPanel(currentState.qrContent)
                    is TransferState.Sending -> ProgressPanel("Sending…", currentState.progress)
                    is TransferState.Receiving -> ProgressPanel("Receiving…", currentState.progress)
                    is TransferState.Done -> ResultPanel(currentState.message, Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary)
                    is TransferState.Error -> ResultPanel(currentState.message, Icons.Default.Error, MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun IdlePanel(onSend: () -> Unit, onReceive: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(Icons.Default.Security, contentDescription = null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Encrypted File Transfer", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            "Files are encrypted with AES-256-GCM on your device before leaving.\nNo servers. No tracking. No cloud.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
        Button(onClick = onSend, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Upload, null)
            Spacer(Modifier.width(8.dp))
            Text("Send a File")
        }
        OutlinedButton(onClick = onReceive, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.QrCodeScanner, null)
            Spacer(Modifier.width(8.dp))
            Text("Receive (Scan QR)")
        }
    }
}

@Composable
private fun SenderQrPanel(qrContent: String) {
    val bitmap = remember(qrContent) { generateQrBitmap(qrContent) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        Text("Show this QR to receiver", style = MaterialTheme.typography.titleMedium)
        bitmap?.let {
            Card(modifier = Modifier.size(280.dp)) {
                Image(it.asImageBitmap(), contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize().padding(12.dp))
            }
        }
        Text(
            "The QR code contains the encryption key.\nReceiver must scan it within the same Wi-Fi network.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text("Waiting for receiver to connect…", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ProgressPanel(label: String, progress: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp)
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun LoadingPanel(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator()
        Text(message)
    }
}

@Composable
private fun ResultPanel(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = tint)
        Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
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
