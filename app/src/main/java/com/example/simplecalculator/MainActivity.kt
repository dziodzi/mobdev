package com.example.simplecalculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var currentValue = ""
    private var firstValue: Double? = null
    private var operation: Char? = null
    private var isNewOperation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.display)
        
        // Восстановление состояния после поворота
        savedInstanceState?.let {
            currentValue = it.getString("currentValue", "")
            firstValue = if (it.containsKey("firstValue")) it.getDouble("firstValue") else null
            operation = if (it.containsKey("operation")) it.getChar("operation") else null
            isNewOperation = it.getBoolean("isNewOperation", true)
        }
        
        setupButtons()
    }

    private fun setupButtons() {
        val buttonIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide,
            R.id.btnEquals, R.id.btnClear, R.id.btnDot, R.id.btnSqrt
        )

        buttonIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener { view ->
                onButtonClick(view)
            }
        }
    }

    private fun onButtonClick(view: View) {
        val button = view as Button
        val text = button.text.toString()

        when {
            text.all { it.isDigit() } -> onNumberClick(text)
            text == "." -> onDotClick()
            text == "C" -> onClearClick()
            text == "=" -> onEqualsClick()
            text == "√" -> onSqrtClick()
            text == "×" -> onOperationClick('*')
            text == "÷" -> onOperationClick('/')
            text == "+" -> onOperationClick('+')
            text == "−" -> onOperationClick('-')
        }
    }

    private fun onNumberClick(number: String) {
        if (isNewOperation) {
            currentValue = number
            isNewOperation = false
        } else {
            if (currentValue == "0") {
                currentValue = number
            } else {
                currentValue += number
            }
        }
        updateDisplay()
    }

    private fun onDotClick() {
        if (isNewOperation) {
            currentValue = "0."
            isNewOperation = false
        } else if (!currentValue.contains(".")) {
            currentValue += "."
        }
        updateDisplay()
    }

    private fun onClearClick() {
        currentValue = ""
        firstValue = null
        operation = null
        isNewOperation = true
        updateDisplay()
    }

    private fun onSqrtClick() {
        if (currentValue.isNotEmpty()) {
            val currentDouble = currentValue.toDoubleOrNull() ?: 0.0
            if (currentDouble >= 0) {
                currentValue = formatResult(kotlin.math.sqrt(currentDouble))
            } else {
                currentValue = "Ошибка"
            }
            isNewOperation = true
            updateDisplay()
        }
    }

    private fun onOperationClick(op: Char) {
        if (currentValue.isNotEmpty()) {
            val currentDouble = currentValue.toDoubleOrNull() ?: 0.0
            
            if (firstValue != null && operation != null && !isNewOperation) {
                firstValue = calculate(firstValue!!, currentDouble, operation!!)
            } else {
                firstValue = currentDouble
            }
            
            operation = op
            isNewOperation = true
            updateDisplay()
        } else if (operation != null) {
            // Смена операции если число не введено
            operation = op
        }
    }

    private fun onEqualsClick() {
        if (firstValue != null && operation != null && currentValue.isNotEmpty()) {
            val secondValue = currentValue.toDoubleOrNull() ?: 0.0
            val result = calculate(firstValue!!, secondValue, operation!!)
            
            currentValue = formatResult(result)
            firstValue = null
            operation = null
            isNewOperation = true
            updateDisplay()
        }
    }

    private fun calculate(first: Double, second: Double, op: Char): Double {
        return when (op) {
            '+' -> first + second
            '-' -> first - second
            '*' -> first * second
            '/' -> if (second != 0.0) first / second else Double.NaN
            else -> second
        }
    }

    private fun formatResult(result: Double): String {
        return if (result.isNaN()) {
            "Ошибка"
        } else if (result.isInfinite()) {
            "∞"
        } else {
            val formatted = String.format("%.8f", result)
            formatted.replace(Regex("0*$"), "").replace(Regex("\\.$"), "")
        }
    }

    private fun updateDisplay() {
        display.text = if (currentValue.isEmpty()) "0" else currentValue
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentValue", currentValue)
        firstValue?.let { outState.putDouble("firstValue", it) }
        operation?.let { outState.putChar("operation", it) }
        outState.putBoolean("isNewOperation", isNewOperation)
    }
}
