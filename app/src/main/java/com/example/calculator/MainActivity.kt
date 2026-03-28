package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView
    private lateinit var tvExpression: TextView

    private var currentInput = ""
    private var firstOperand = ""
    private var operator = ""
    private var isNewInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)
        tvExpression = findViewById(R.id.tvExpression)

        val numberButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )
        numberButtons.forEach { id ->
            findViewById<Button>(id).setOnClickListener { onDigit((it as Button).text.toString()) }
        }

        findViewById<Button>(R.id.btnDot).setOnClickListener { onDot() }
        findViewById<Button>(R.id.btnPlus).setOnClickListener { onOperator("+") }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { onOperator("−") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperator("×") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperator("÷") }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { onEquals() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { onClear() }
        findViewById<Button>(R.id.btnSign).setOnClickListener { onSign() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { onPercent() }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { onBackspace() }
    }

    private fun onDigit(digit: String) {
        if (isNewInput) {
            currentInput = digit
            isNewInput = false
        } else {
            if (currentInput == "0") {
                currentInput = digit
            } else {
                if (currentInput.replace("-", "").replace(".", "").length < 9) {
                    currentInput += digit
                }
            }
        }
        updateDisplay()
    }

    private fun onDot() {
        if (isNewInput) {
            currentInput = "0."
            isNewInput = false
        } else if (!currentInput.contains(".")) {
            currentInput = if (currentInput.isEmpty() || currentInput == "-") "${currentInput}0." else "$currentInput."
        }
        updateDisplay()
    }

    private fun onOperator(op: String) {
        if (currentInput.isEmpty() && firstOperand.isEmpty()) return

        if (firstOperand.isNotEmpty() && currentInput.isNotEmpty() && !isNewInput) {
            val result = calculate()
            firstOperand = result
            tvExpression.text = "$result $op"
        } else {
            firstOperand = if (currentInput.isNotEmpty()) currentInput else firstOperand
            tvExpression.text = "$firstOperand $op"
        }

        operator = op
        isNewInput = true
        tvDisplay.text = formatNumber(firstOperand)
    }

    private fun onEquals() {
        if (firstOperand.isEmpty() || operator.isEmpty() || (currentInput.isEmpty() && isNewInput)) return

        val secondOperand = if (currentInput.isEmpty()) firstOperand else currentInput
        val result = calculate(secondOperand)

        tvExpression.text = "$firstOperand $operator $secondOperand ="
        currentInput = result
        firstOperand = ""
        operator = ""
        isNewInput = true
        tvDisplay.text = formatNumber(result)
    }

    private fun calculate(second: String = currentInput): String {
        val a = firstOperand.toDoubleOrNull() ?: return currentInput
        val b = second.toDoubleOrNull() ?: return currentInput

        val result = when (operator) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0.0) return "Ошибка" else a / b
            else -> return currentInput
        }

        return try {
            BigDecimal(result).setScale(10, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        } catch (e: Exception) {
            result.toBigDecimal().toPlainString()
        }
    }

    private fun onClear() {
        currentInput = ""
        firstOperand = ""
        operator = ""
        isNewInput = false
        tvDisplay.text = "0"
        tvExpression.text = ""
    }

    private fun onSign() {
        if (currentInput.isEmpty() || currentInput == "0") return
        currentInput = if (currentInput.startsWith("-")) {
            currentInput.substring(1)
        } else {
            "-$currentInput"
        }
        updateDisplay()
    }

    private fun onPercent() {
        val value = currentInput.toDoubleOrNull() ?: return
        currentInput = BigDecimal(value / 100)
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        updateDisplay()
    }

    private fun onBackspace() {
        if (isNewInput || currentInput.isEmpty()) return
        currentInput = currentInput.dropLast(1)
        updateDisplay()
    }

    private fun updateDisplay() {
        tvDisplay.text = if (currentInput.isEmpty()) "0" else formatNumber(currentInput)
    }

    private fun formatNumber(number: String): String {
        if (number == "Ошибка") return number
        val isNegative = number.startsWith("-")
        val abs = if (isNegative) number.substring(1) else number
        val parts = abs.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) ".${parts[1]}" else ""

        val formatted = if (intPart.length > 3) {
            intPart.reversed().chunked(3).joinToString(" ").reversed()
        } else intPart

        return "${if (isNegative) "-" else ""}$formatted$decPart"
    }
}
