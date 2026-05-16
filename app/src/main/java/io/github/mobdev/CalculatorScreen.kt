package io.github.mobdev

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val ColorOperator  = Color(0xFFFF9F0A)
private val ColorFunction  = Color(0xFF636366)
private val ColorDigit     = Color(0xFF1C1C1E)
private val ColorDisplayBg = Color(0xFF000000)
private val ColorText      = Color(0xFFFFFFFF)

private data class Key(
    val label: String,
    val type: KeyType,
    val span: Int = 1,
    val action: () -> Unit
)
private enum class KeyType { Digit, Operator, Function }

@Composable
fun CalculatorScreen(vm: CalculatorViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val rows = remember(vm) {
        listOf(
            listOf(
                Key("AC",  KeyType.Function) { vm.onClear() },
                Key("+/−", KeyType.Function) { vm.onToggleSign() },
                Key("%",   KeyType.Function) { vm.onPercent() },
                Key("÷",   KeyType.Operator) { vm.onOperator('÷') }
            ),
            listOf(
                Key("7", KeyType.Digit) { vm.onDigit("7") },
                Key("8", KeyType.Digit) { vm.onDigit("8") },
                Key("9", KeyType.Digit) { vm.onDigit("9") },
                Key("×", KeyType.Operator) { vm.onOperator('×') }
            ),
            listOf(
                Key("4", KeyType.Digit) { vm.onDigit("4") },
                Key("5", KeyType.Digit) { vm.onDigit("5") },
                Key("6", KeyType.Digit) { vm.onDigit("6") },
                Key("−", KeyType.Operator) { vm.onOperator('-') }
            ),
            listOf(
                Key("1", KeyType.Digit) { vm.onDigit("1") },
                Key("2", KeyType.Digit) { vm.onDigit("2") },
                Key("3", KeyType.Digit) { vm.onDigit("3") },
                Key("+", KeyType.Operator) { vm.onOperator('+') }
            ),
            listOf(
                Key("0", KeyType.Digit, span = 2) { vm.onDigit("0") },
                Key(",", KeyType.Digit) { vm.onDot() },
                Key("=", KeyType.Operator) { vm.onEquals() }
            )
        )
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorDisplayBg)
                .padding(paddingValues)
        ) {
            if (isLandscape) {
                LandscapeLayout(display = state.display, rows = rows)
            } else {
                PortraitLayout(display = state.display, rows = rows)
            }
        }
    }
}

// ─── Portrait ────────────────────────────────────────────────────────────────

@Composable
private fun PortraitLayout(display: String, rows: List<List<Key>>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        DisplayText(
            text = display,
            fontSize = 72.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp, bottom = 8.dp)
        )
        ButtonGrid(
            rows = rows,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}

// ─── Landscape ───────────────────────────────────────────────────────────────

@Composable
private fun LandscapeLayout(display: String, rows: List<List<Key>>) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left: display panel
        Box(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .padding(start = 12.dp, end = 4.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            DisplayText(
                text = display,
                fontSize = 52.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }
        // Right: button grid
        ButtonGrid(
            rows = rows,
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

// ─── Shared components ───────────────────────────────────────────────────────

@Composable
private fun DisplayText(text: String, fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier) {
    Text(
        text = text,
        color = ColorText,
        fontSize = fontSize,
        fontWeight = FontWeight.Light,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun ButtonGrid(rows: List<List<Key>>, modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    CalcButton(
                        key = key,
                        modifier = Modifier
                            .weight(key.span.toFloat())
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcButton(key: Key, modifier: Modifier = Modifier) {
    val bgColor = when (key.type) {
        KeyType.Operator -> ColorOperator
        KeyType.Function -> ColorFunction
        KeyType.Digit    -> ColorDigit
    }
    val textColor = when (key.type) {
        KeyType.Function -> ColorDisplayBg
        else             -> ColorText
    }
    Surface(
        onClick = key.action,
        shape = RoundedCornerShape(50),
        color = bgColor,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = key.label,
                color = textColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
