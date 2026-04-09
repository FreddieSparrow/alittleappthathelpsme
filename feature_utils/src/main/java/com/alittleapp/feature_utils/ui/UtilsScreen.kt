package com.alittleapp.feature_utils.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private enum class UtilTool { NONE, UNIT_CONVERTER, QR_SCANNER, QR_GENERATOR, PASSWORD_GEN, CALCULATOR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilsScreen() {
    var activeTool by remember { mutableStateOf(UtilTool.NONE) }

    when (activeTool) {
        UtilTool.UNIT_CONVERTER -> UnitConverterScreen(onBack = { activeTool = UtilTool.NONE })
        UtilTool.QR_SCANNER -> QrScannerScreen(onBack = { activeTool = UtilTool.NONE })
        UtilTool.QR_GENERATOR -> QrGeneratorScreen(onBack = { activeTool = UtilTool.NONE })
        UtilTool.PASSWORD_GEN -> PasswordGeneratorScreen(onBack = { activeTool = UtilTool.NONE })
        UtilTool.CALCULATOR -> CalculatorScreen(onBack = { activeTool = UtilTool.NONE })
        UtilTool.NONE -> ToolsGrid(onSelect = { activeTool = it })
    }
}

@Composable
private fun ToolsGrid(onSelect: (UtilTool) -> Unit) {
    val tools = listOf(
        Triple(UtilTool.UNIT_CONVERTER, Icons.Default.Straighten, "Unit Converter"),
        Triple(UtilTool.QR_SCANNER, Icons.Default.QrCodeScanner, "QR Scanner"),
        Triple(UtilTool.QR_GENERATOR, Icons.Default.QrCode2, "QR Generator"),
        Triple(UtilTool.PASSWORD_GEN, Icons.Default.Password, "Password Gen"),
        Triple(UtilTool.CALCULATOR, Icons.Default.Calculate, "Calculator"),
    )

    Column {
        Text(
            "Tools",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tools) { (tool, icon, label) ->
                ToolCard(icon = icon, label = label, onClick = { onSelect(tool) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolCard(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
