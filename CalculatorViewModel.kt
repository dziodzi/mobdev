package com.helpmethen.ksushacalculator

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mariuszgromada.math.mxparser.Expression

class CalculatorViewModel : ViewModel() {

    private val _state: MutableStateFlow<CalculatorState> = MutableStateFlow(
        CalculatorState.Initial
    )

    val state = _state.asStateFlow()

    private var expression = ""

    fun processCommand(command: CalculatorCommand) {
        when (command) {
            CalculatorCommand.Clear -> {
                expression = ""
                _state.value = CalculatorState.Initial
            }

            CalculatorCommand.Evaluate -> {
                val result = evaluate()
                _state.value = if (result != null) {
                    CalculatorState.Success(result = result)
                } else {
                    CalculatorState.Error(expression = expression)
                }
            }
            is CalculatorCommand.Input -> {
                val symbol: String = if (command.symbol != Symbol.PARENTHESIS) {
                    command.symbol.value
                } else {
                    getCorrectParenthesis()
                }
                expression += command.symbol.value

                _state.value = CalculatorState.Input(
                    expression = expression,
                    resultPreview = evaluate() ?: ""
                )
            }
        }
    }

    private fun evaluate() : String? {
        return expression.replace('x', '*')
            .let{ Expression(it) }
            .calculate()
            .takeIf { it.isFinite() } ?.toString()
        return Expression(expression).calculate().toString()
    }

    private fun getCorrectParenthesis(): String {
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }

        return when {
            expression.isEmpty() -> "("
            expression.last().let { !it.isDigit() && it != ')' && it != 'π' }
                -> ")"

            openCount > closeCount -> ")"
            else -> "("
        }
    }

    sealed interface CalculatorCommand {

        data object Clear : CalculatorCommand
        data object Evaluate : CalculatorCommand
        data class Input(val symbol: CalculatorViewModel.Symbol) : CalculatorCommand
    }

    sealed interface CalculatorState {

        data object Initial : CalculatorState

        data class Input(
            val expression: String,
            val resultPreview: String
        ) : CalculatorState

        data class Success(val result: String) : CalculatorState

        data class Error(val expression: String) : CalculatorState
    }

    enum class Symbol(val value: String) {
        DIGIT_0("0"),
        DIGIT_1("1"),
        DIGIT_2("2"),
        DIGIT_3("3"),
        DIGIT_4("4"),
        DIGIT_5("5"),
        DIGIT_6("6"),
        DIGIT_7("7"),
        DIGIT_8("8"),
        DIGIT_9("9"),
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("x"),
        DIVIDE("/"),
        PERCENT("%"),
        POWER("^"),
        FACTORIAL("!"),
        SQRT("√"),
        PI("π"),
        DOT("."),
        PARENTHESIS("()")

    }
}

data class Display(
    val expression: String,
    val resultPreview: String
)