package com.barboza.bingomanager.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barboza.bingomanager.model.BingoTemplate
import com.barboza.bingomanager.model.CellKind
import com.barboza.bingomanager.ui.theme.BingoRedSoft
import com.barboza.bingomanager.ui.theme.LightGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) { Text("‹ Volver") }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding -> content(Modifier.padding(padding)) }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 15.dp),
    ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun <T> SelectionDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionText: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = selected?.let(optionText) ?: "Seleccionar",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                Text("▾")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionText(option)) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun EditableCardGrid(
    template: BingoTemplate,
    values: List<String>,
    onValueChange: (Int, String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(template.rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(template.columns) { column ->
                        val index = row * template.columns + column
                        when (template.cells[index]) {
                            CellKind.NUMBER -> OutlinedTextField(
                                value = values.getOrElse(index) { "" },
                                onValueChange = { input ->
                                    if (input.length <= 4 && input.all(Char::isDigit)) onValueChange(index, input)
                                },
                                modifier = Modifier.size(62.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            CellKind.FREE -> GridCell(text = "LIBRE", background = BingoRedSoft)
                            CellKind.DISABLED -> Box(Modifier.size(62.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayCardGrid(
    template: BingoTemplate,
    numbers: List<Int?>,
    markedIndices: Set<Int>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val cellSize = if (compact) 42.dp else 50.dp
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(template.rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(template.columns) { column ->
                        val index = row * template.columns + column
                        if (template.cells[index] == CellKind.DISABLED) {
                            Box(Modifier.size(cellSize))
                        } else {
                            val marked = index in markedIndices
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .background(
                                        if (marked) MaterialTheme.colorScheme.primary else Color.White,
                                        RoundedCornerShape(9.dp),
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (template.cells[index] == CellKind.FREE) "★" else numbers.getOrNull(index)?.toString().orEmpty(),
                                    color = if (marked) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (compact) 13.sp else 15.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCell(text: String, background: Color) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .background(background, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun PatternCell(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    size: Int = 40,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(
                when {
                    !enabled -> Color.Transparent
                    selected -> MaterialTheme.colorScheme.primary
                    else -> LightGray
                },
                RoundedCornerShape(8.dp),
            )
            .then(if (enabled) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    }
}
