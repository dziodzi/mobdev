package io.github.mobdev

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CalculatorViewModel : ViewModel() {

    var displayResult by mutableStateOf("0")
        private set

    private var currentInput = ""
    private var pendingOperator = ""
    private var firstOperand = 0.0
    private var hasFirstOperand = false
    private var resetOnNextDigit = false

    fun onButton(label: String) {
        when (label) {
            in "0".."9" -> onDigit(label)
            "," -> onDot()
            "AC" -> onClear()
            "⌫" -> onBackspace()
            "%" -> onPercent()
            "±" -> onPlusMinus()
            "=" -> onEquals()
            "+", "−", "×", "÷" -> onOperator(label)
        }
    }

    private fun onDigit(digit: String) {
        if (resetOnNextDigit) {
            currentInput = ""
            resetOnNextDigit = false
        }
        if (digit == "0" && currentInput == "0") return
        currentInput = if (currentInput == "0") digit else currentInput + digit
        displayResult = toDisplay(currentInput)
    }

    private fun onDot() {
        if (resetOnNextDigit) {
            currentInput = "0"
            resetOnNextDigit = false
        }
        if (currentInput.isEmpty()) currentInput = "0"
        if (!currentInput.contains(".")) {
            currentInput += "."
        }
        displayResult = toDisplay(currentInput)
    }

    private fun onOperator(op: String) {
        val current = currentInput.toDoubleOrNull() ?: 0.0

        if (hasFirstOperand && pendingOperator.isNotEmpty() && !resetOnNextDigit) {
            firstOperand = calculate(firstOperand, current, pendingOperator)
            displayResult = toDisplay(formatResult(firstOperand))
        } else {
            firstOperand = current
        }

        hasFirstOperand = true
        pendingOperator = op
        resetOnNextDigit = true
    }

    private fun onEquals() {
        if (!hasFirstOperand || pendingOperator.isEmpty()) return

        val current = currentInput.toDoubleOrNull() ?: 0.0
        val result = calculate(firstOperand, current, pendingOperator)

        displayResult = toDisplay(formatResult(result))
        firstOperand = result
        currentInput = ""
        pendingOperator = ""
        hasFirstOperand = false
        resetOnNextDigit = true
    }

    private fun onClear() {
        currentInput = ""
        pendingOperator = ""
        firstOperand = 0.0
        hasFirstOperand = false
        displayResult = "0"
        resetOnNextDigit = false
    }

    private fun onBackspace() {
        if (resetOnNextDigit) return
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            if (currentInput.isEmpty() || currentInput == "-") {
                currentInput = ""
                displayResult = "0"
            } else {
                displayResult = toDisplay(currentInput)
            }
        }
    }

    private fun onPlusMinus() {
        if (currentInput.isEmpty() || currentInput == "0") return
        currentInput = if (currentInput.startsWith("-")) {
            currentInput.substring(1)
        } else {
            "-$currentInput"
        }
        displayResult = toDisplay(currentInput)
    }

    private fun onPercent() {
        val value = currentInput.toDoubleOrNull() ?: return
        val result = value / 100.0
        currentInput = formatResult(result)
        displayResult = toDisplay(currentInput)
    }

    private fun calculate(a: Double, b: Double, op: String): Double {
        return when (op) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            "÷" -> if (b != 0.0) a / b else Double.NaN
            else -> b
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        return if (value % 1.0 == 0.0 && value in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun toDisplay(value: String): String = value.replace(".", ",")
}
