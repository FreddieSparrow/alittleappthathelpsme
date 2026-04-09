package com.alittleapp.feature_utils.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private enum class UnitCategory(val label: String) {
    LENGTH("Length"), WEIGHT("Weight"), TEMPERATURE("Temperature"),
    VOLUME("Volume"), AREA("Area"), SPEED("Speed"), DATA("Data")
}

private val unitsByCategory: Map<UnitCategory, List<String>> = mapOf(
    UnitCategory.LENGTH to listOf("Meter", "Kilometer", "Mile", "Foot", "Inch", "Centimeter", "Millimeter"),
    UnitCategory.WEIGHT to listOf("Kilogram", "Gram", "Pound", "Ounce", "Tonne"),
    UnitCategory.TEMPERATURE to listOf("Celsius", "Fahrenheit", "Kelvin"),
    UnitCategory.VOLUME to listOf("Liter", "Milliliter", "Gallon (US)", "Quart", "Pint", "Cup"),
    UnitCategory.AREA to listOf("Square Meter", "Square Kilometer", "Square Mile", "Square Foot", "Hectare", "Acre"),
    UnitCategory.SPEED to listOf("m/s", "km/h", "mph", "knot"),
    UnitCategory.DATA to listOf("Byte", "Kilobyte", "Megabyte", "Gigabyte", "Terabyte")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(onBack: () -> Unit) {
    var selectedCategory by remember { mutableStateOf(UnitCategory.LENGTH) }
    var fromUnit by remember { mutableStateOf("Meter") }
    var toUnit by remember { mutableStateOf("Kilometer") }
    var inputValue by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    val units = unitsByCategory[selectedCategory] ?: emptyList()
    val result = remember(inputValue, fromUnit, toUnit, selectedCategory) {
        val d = inputValue.toDoubleOrNull()
        if (d == null) "" else convertUnit(d, fromUnit, toUnit, selectedCategory).let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else "%.6g".format(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unit Converter") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category picker
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = selectedCategory.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    UnitCategory.values().forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.label) },
                            onClick = {
                                selectedCategory = cat
                                fromUnit = unitsByCategory[cat]!!.first()
                                toUnit = unitsByCategory[cat]!!.getOrElse(1) { unitsByCategory[cat]!!.first() }
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // From unit
                ExposedDropdownMenuBox(
                    expanded = fromExpanded,
                    onExpandedChange = { fromExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = fromUnit, onValueChange = {}, readOnly = true,
                        label = { Text("From") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(fromExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                        units.forEach { unit ->
                            DropdownMenuItem(text = { Text(unit) }, onClick = { fromUnit = unit; fromExpanded = false })
                        }
                    }
                }

                IconButton(onClick = { val tmp = fromUnit; fromUnit = toUnit; toUnit = tmp }) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Swap")
                }

                // To unit
                ExposedDropdownMenuBox(
                    expanded = toExpanded,
                    onExpandedChange = { toExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = toUnit, onValueChange = {}, readOnly = true,
                        label = { Text("To") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(toExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                        units.forEach { unit ->
                            DropdownMenuItem(text = { Text(unit) }, onClick = { toUnit = unit; toExpanded = false })
                        }
                    }
                }
            }

            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                label = { Text("Value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (result.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Result", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text("$result $toUnit", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}

private fun convertUnit(value: Double, from: String, to: String, category: UnitCategory): Double {
    if (from == to) return value
    return when (category) {
        UnitCategory.TEMPERATURE -> convertTemperature(value, from, to)
        else -> {
            val toBase = toBaseUnit(value, from, category)
            fromBaseUnit(toBase, to, category)
        }
    }
}

// Conversion factors to base unit
private fun toBaseUnit(value: Double, unit: String, category: UnitCategory): Double = when (category) {
    UnitCategory.LENGTH -> when (unit) {
        "Meter" -> value; "Kilometer" -> value * 1000; "Mile" -> value * 1609.344
        "Foot" -> value * 0.3048; "Inch" -> value * 0.0254
        "Centimeter" -> value * 0.01; "Millimeter" -> value * 0.001; else -> value
    }
    UnitCategory.WEIGHT -> when (unit) {
        "Kilogram" -> value; "Gram" -> value * 0.001; "Pound" -> value * 0.453592
        "Ounce" -> value * 0.0283495; "Tonne" -> value * 1000; else -> value
    }
    UnitCategory.VOLUME -> when (unit) {
        "Liter" -> value; "Milliliter" -> value * 0.001; "Gallon (US)" -> value * 3.78541
        "Quart" -> value * 0.946353; "Pint" -> value * 0.473176; "Cup" -> value * 0.24; else -> value
    }
    UnitCategory.AREA -> when (unit) {
        "Square Meter" -> value; "Square Kilometer" -> value * 1_000_000
        "Square Mile" -> value * 2_589_988.11; "Square Foot" -> value * 0.0929
        "Hectare" -> value * 10_000; "Acre" -> value * 4046.856; else -> value
    }
    UnitCategory.SPEED -> when (unit) {
        "m/s" -> value; "km/h" -> value / 3.6; "mph" -> value * 0.44704; "knot" -> value * 0.514444; else -> value
    }
    UnitCategory.DATA -> when (unit) {
        "Byte" -> value; "Kilobyte" -> value * 1024; "Megabyte" -> value * 1_048_576
        "Gigabyte" -> value * 1_073_741_824; "Terabyte" -> value * 1_099_511_627_776L.toDouble(); else -> value
    }
    else -> value
}

private fun fromBaseUnit(value: Double, unit: String, category: UnitCategory): Double =
    1.0 / toBaseUnit(1.0, unit, category) * value

private fun convertTemperature(value: Double, from: String, to: String): Double {
    val celsius = when (from) {
        "Celsius" -> value; "Fahrenheit" -> (value - 32) * 5 / 9; "Kelvin" -> value - 273.15; else -> value
    }
    return when (to) {
        "Celsius" -> celsius; "Fahrenheit" -> celsius * 9 / 5 + 32; "Kelvin" -> celsius + 273.15; else -> celsius
    }
}
