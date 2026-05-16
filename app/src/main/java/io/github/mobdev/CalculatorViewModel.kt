package io.github.mobdev

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CalcState(
    val display: String = "0",
    val firstOperand: Double? = null,
    val pendingOp: Char? = null,
    val justEvaluated: Boolean = false
)

class CalculatorViewModel : ViewModel() {

    private val _state = MutableStateFlow(CalcState())
    val state: StateFlow<CalcState> = _state.asStateFlow()

    fun onDigit(digit: String) {
        _state.value = _state.value.let { s ->
            val newDisplay = when {
                s.justEvaluated || s.display == "0" -> digit
                else -> s.display + digit
            }
            s.copy(display = newDisplay, justEvaluated = false)
        }
    }

    fun onDot() {
        _state.value = _state.value.let { s ->
            val current = if (s.justEvaluated) "0" else s.display
            val newDisplay = if ('.' in current) current else "$current."
            s.copy(display = newDisplay, justEvaluated = false)
        }
    }

    fun onOperator(op: Char) {
        _state.value = _state.value.let { s ->
            val current = s.display.toDoubleOrNull() ?: 0.0
            val first = if (s.firstOperand != null && !s.justEvaluated) {
                evaluate(s.firstOperand, current, s.pendingOp)
            } else {
                current
            }
            s.copy(
                display = formatResult(first),
                firstOperand = first,
                pendingOp = op,
                justEvaluated = true
            )
        }
    }

    fun onEquals() {
        _state.value = _state.value.let { s ->
            val first = s.firstOperand ?: return
            val second = s.display.toDoubleOrNull() ?: 0.0
            val result = evaluate(first, second, s.pendingOp)
            s.copy(
                display = formatResult(result),
                firstOperand = null,
                pendingOp = null,
                justEvaluated = true
            )
        }
    }

    fun onClear() {
        _state.value = CalcState()
    }

    fun onToggleSign() {
        _state.value = _state.value.let { s ->
            val value = s.display.toDoubleOrNull() ?: return
            s.copy(display = formatResult(-value))
        }
    }

    fun onPercent() {
        _state.value = _state.value.let { s ->
            val value = s.display.toDoubleOrNull() ?: return
            s.copy(display = formatResult(value / 100.0))
        }
    }

    private fun evaluate(a: Double, b: Double, op: Char?): Double = when (op) {
        '+' -> a + b
        '-' -> a - b
        '×' -> a * b
        '÷' -> if (b != 0.0) a / b else Double.NaN
        else -> b
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN()) return "Ошибка"
        if (value.isInfinite()) return "Ошибка"
        return if (value == kotlin.math.floor(value) && !value.isInfinite())
            value.toLong().toString()
        else
            value.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}
