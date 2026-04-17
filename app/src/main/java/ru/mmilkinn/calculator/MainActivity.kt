package ru.mmilkinn.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView

    private var expression = ""
    private var currentInput = ""
    private var firstNumber: Double? = null
    private var operation: String? = null
    private var isNewInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)

        restoreState(savedInstanceState)
        bindNumberButtons()
        bindActionButtons()
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return

        expression = savedInstanceState.getString("expression", "")
        currentInput = savedInstanceState.getString("currentInput", "")
        operation = savedInstanceState.getString("operation")
        isNewInput = savedInstanceState.getBoolean("isNewInput", false)

        firstNumber = if (savedInstanceState.containsKey("firstNumber")) {
            savedInstanceState.getDouble("firstNumber")
        } else {
            null
        }

        updateDisplay()
    }

    private fun bindNumberButtons() {
        val numberButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        for (id in numberButtons) {
            findViewById<Button>(id).setOnClickListener {
                appendNumber((it as Button).text.toString())
            }
        }
    }

    private fun bindActionButtons() {
        bindOperationButton(R.id.btnPlus, "+")
        bindOperationButton(R.id.btnMinus, "-")
        bindOperationButton(R.id.btnUmnozhenie, "*")
        bindOperationButton(R.id.btnDelenie, "/")

        findViewById<Button>(R.id.btnRes).setOnClickListener { calculateResult() }
        findViewById<Button>(R.id.btnAC).setOnClickListener { clearAll() }
        findViewById<Button>(R.id.btnClearOne).setOnClickListener { deleteOneSymbol() }
        findViewById<Button>(R.id.btnFloat).setOnClickListener { appendDot() }
        findViewById<Button>(R.id.btnSqrt).setOnClickListener { applyUnaryOperation(::sqrt) }
        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener { applyUnaryOperation { -it } }
    }

    private fun bindOperationButton(buttonId: Int, op: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            setOperation(op)
        }
    }

    private fun appendNumber(number: String) {
        if (currentInput.length >= 20) return

        if (isNewInput) {
            expression = ""
            currentInput = ""
            firstNumber = null
            operation = null
            isNewInput = false
        }

        currentInput += number
        expression += number
        tvDisplay.text = expression
    }

    private fun appendDot() {
        if (isNewInput) {
            expression = ""
            currentInput = ""
            firstNumber = null
            operation = null
            isNewInput = false
        }

        if (currentInput.contains(".")) return
        if (currentInput.length >= 20) return

        if (currentInput.isEmpty()) {
            currentInput = "0."
            expression += "0."
        } else {
            currentInput += "."
            expression += "."
        }

        tvDisplay.text = expression
    }

    private fun setOperation(op: String) {
        if (currentInput.isEmpty()) return

        val currentNumber = currentInput.toDoubleOrNull() ?: return

        if (firstNumber == null) {
            firstNumber = currentNumber
        } else if (operation != null) {
            val result = calculateBinary(firstNumber!!, currentNumber, operation!!) ?: return
            firstNumber = result
            expression = formatResult(result)
        }

        operation = op
        currentInput = ""
        expression += " $op "
        tvDisplay.text = expression
    }

    private fun calculateResult() {
        val secondNumber = currentInput.toDoubleOrNull() ?: return
        val first = firstNumber ?: return
        val op = operation ?: return

        val result = calculateBinary(first, secondNumber, op) ?: return

        expression = formatResult(result)
        currentInput = expression
        firstNumber = null
        operation = null
        isNewInput = true

        tvDisplay.text = expression
    }

    private fun calculateBinary(first: Double, second: Double, op: String): Double? {
        return when (op) {
            "+" -> first + second
            "-" -> first - second
            "*" -> first * second
            "/" -> {
                if (second == 0.0) {
                    showError()
                    null
                } else {
                    first / second
                }
            }
            else -> null
        }
    }

    private fun applyUnaryOperation(action: (Double) -> Double) {
        val number = currentInput.toDoubleOrNull() ?: return

        if (number < 0 && action == ::sqrt) {
            showError()
            return
        }

        val result = action(number)
        currentInput = formatResult(result)
        replaceCurrentInputInExpression()
        updateDisplay()
    }

    private fun replaceCurrentInputInExpression() {
        expression = if (operation == null) {
            currentInput
        } else {
            val parts = expression.split(" ")
            if (parts.size >= 2) {
                "${parts[0]} ${parts[1]} $currentInput"
            } else {
                currentInput
            }
        }
    }

    private fun deleteOneSymbol() {
        if (expression.isEmpty()) return

        expression = expression.dropLast(1)

        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
        }

        updateDisplay()
    }

    private fun clearAll() {
        expression = ""
        currentInput = ""
        firstNumber = null
        operation = null
        isNewInput = false
        updateDisplay()
    }

    private fun resetForNewInput() {
        expression = ""
        currentInput = ""
        firstNumber = null
        operation = null
        isNewInput = false
    }

    private fun showError() {
        tvDisplay.text = "Ошибка"
        resetForNewInput()
        isNewInput = true
    }

    private fun updateDisplay() {
        tvDisplay.text = if (expression.isEmpty()) "0" else expression
    }

    private fun formatResult(result: Double): String {
        return if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            String.format("%.8f", result).trimEnd('0').trimEnd('.')
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("expression", expression)
        outState.putString("currentInput", currentInput)
        outState.putString("operation", operation)
        outState.putBoolean("isNewInput", isNewInput)

        firstNumber?.let {
            outState.putDouble("firstNumber", it)
        }
    }
}