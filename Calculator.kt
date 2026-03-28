package com.helpmethen.ksushacalculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun Calculator(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel()
) {
    val state = viewModel.state.collectAsState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            when(val currentState = state.value){
                is CalculatorViewModel.CalculatorState.Error -> {
                    Text(
                        text = currentState.expression,
                        lineHeight = 36.sp,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "",
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                CalculatorViewModel.CalculatorState.Initial -> {
                }
                is CalculatorViewModel.CalculatorState.Input -> {
                    Text(
                        text = currentState.expression,
                        fontSize = 36.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = currentState.resultPreview,
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                is CalculatorViewModel.CalculatorState.Success -> {
                    Text(
                        text = currentState.result,
                        fontSize = 36.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "",
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "√",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.SQRT
                                )
                            )
                        },
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "π",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.PI
                                )
                            )
                        },
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "^",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.POWER
                                )
                            )
                        },
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "!",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.FACTORIAL
                                )
                            )
                        },
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Clear
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AC",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.PARENTHESIS
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "()",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }


                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.PERCENT
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIVIDE
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "÷",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIGIT_7
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "7",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIGIT_8
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "8",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIGIT_9
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "9",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.MULTIPLY
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIGIT_4
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "4",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIGIT_5
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "5",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIGIT_6
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "6",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.SUBTRACT
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "-",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIGIT_1
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIGIT_2
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "2",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIGIT_3
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.ADD
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DIGIT_0
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "0",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Input(
                                    CalculatorViewModel.Symbol.DOT
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ",",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            viewModel.processCommand(
                                CalculatorViewModel.CalculatorCommand.Evaluate
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "=",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
