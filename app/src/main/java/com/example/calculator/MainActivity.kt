package com.example.calculator

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Indigo = Color(0xFF6C63FF)
private val DeepSlate = Color(0xFF1E1E2E)
private val SteelBlue = Color(0xFF45475A)

private enum class KeyRole { DIGIT, UTILITY, OPERATION }
private data class CalcKey(val symbol: String, val role: KeyRole)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorScreen()
        }
    }
}

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = viewModel()) {
    val keys = listOf(
        listOf(
            CalcKey("⌫", KeyRole.UTILITY),
            CalcKey("AC", KeyRole.UTILITY),
            CalcKey("%", KeyRole.UTILITY),
            CalcKey("÷", KeyRole.OPERATION)
        ),
        listOf(
            CalcKey("7", KeyRole.DIGIT),
            CalcKey("8", KeyRole.DIGIT),
            CalcKey("9", KeyRole.DIGIT),
            CalcKey("×", KeyRole.OPERATION)
        ),
        listOf(
            CalcKey("4", KeyRole.DIGIT),
            CalcKey("5", KeyRole.DIGIT),
            CalcKey("6", KeyRole.DIGIT),
            CalcKey("−", KeyRole.OPERATION)
        ),
        listOf(
            CalcKey("1", KeyRole.DIGIT),
            CalcKey("2", KeyRole.DIGIT),
            CalcKey("3", KeyRole.DIGIT),
            CalcKey("+", KeyRole.OPERATION)
        ),
        listOf(
            CalcKey("±", KeyRole.UTILITY),
            CalcKey("0", KeyRole.DIGIT),
            CalcKey(",", KeyRole.DIGIT),
            CalcKey("=", KeyRole.OPERATION)
        )
    )

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(
                horizontal = if (isLandscape) 24.dp else 16.dp,
                vertical = if (isLandscape) 4.dp else 12.dp
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isLandscape) 1.2f else 2f)
                .padding(end = 8.dp, bottom = if (isLandscape) 4.dp else 12.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = viewModel.displayResult,
                color = Color.White,
                fontSize = if (isLandscape) 36.sp else 72.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }

        for (row in keys) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = if (isLandscape) 1.dp else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 12.dp)
            ) {
                for (key in row) {
                    val bgColor = when (key.role) {
                        KeyRole.UTILITY -> SteelBlue
                        KeyRole.DIGIT -> DeepSlate
                        KeyRole.OPERATION -> Indigo
                    }

                    Button(
                        onClick = { viewModel.onButton(key.symbol) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = key.symbol,
                            fontSize = if (isLandscape) 16.sp else 28.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
