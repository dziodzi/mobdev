package ru.klapneva.mobdev.lab1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView// это где вводим
    private lateinit var tvExpression: TextView// это промежуточный результат

    private var currentNumber = ""
    private var currentExpression = ""
    private var operator = ""
    private var firstNumber = 0.0
    private var waitingForSecondNumber = false
    private var lastResult = 0.0  //сохраняем последний результат

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        tvExpression = findViewById(R.id.tvExpression)

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }

        updateDisplay()
        setupButtons()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentNumber", currentNumber)
        outState.putString("currentExpression", currentExpression)
        outState.putString("operator", operator)
        outState.putDouble("firstNumber", firstNumber)
        outState.putBoolean("waitingForSecondNumber", waitingForSecondNumber)
        outState.putDouble("lastResult", lastResult)
    }

    private fun restoreState(savedInstanceState: Bundle) {
        currentNumber = savedInstanceState.getString("currentNumber", "")
        currentExpression = savedInstanceState.getString("currentExpression", "")
        operator = savedInstanceState.getString("operator", "")
        firstNumber = savedInstanceState.getDouble("firstNumber", 0.0)
        waitingForSecondNumber = savedInstanceState.getBoolean("waitingForSecondNumber", false)
        lastResult = savedInstanceState.getDouble("lastResult", 0.0)
    }

    private fun setupButtons() {
        val numberButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        numberButtons.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                onNumberClick(it as Button)
            }
        }

        findViewById<Button>(R.id.btnAdd).setOnClickListener { onOperatorClick("+") }
        findViewById<Button>(R.id.btnSubtract).setOnClickListener { onOperatorClick("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperatorClick("×") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperatorClick("÷") }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { onEqualsClick() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { onClearClick() }
        findViewById<Button>(R.id.btnDecimal).setOnClickListener { onDecimalClick() }
    }

    private fun onNumberClick(button: Button) {
        val digit = button.text.toString()

        if (waitingForSecondNumber) {
            currentNumber = ""
            waitingForSecondNumber = false
        }

        if (currentNumber.length < 15) {
            currentNumber += digit
            updateExpressionAndIntermediate()
        }

        updateDisplay()
    }

    private fun onOperatorClick(op: String) {
        //если есть результат и нет текущей операции, начинаем новое выражение
        if (operator.isEmpty() && currentNumber.isEmpty() && lastResult != 0.0) {
            firstNumber = lastResult
            currentExpression = formatNumber(firstNumber) + " " + op
            operator = op
            waitingForSecondNumber = true
            tvResult.text = currentExpression
            tvExpression.text = formatNumber(firstNumber)
            return
        }

        if (currentNumber.isNotEmpty()) {
            if (operator.isEmpty()) {
                //первая операция
                firstNumber = currentNumber.toDoubleOrNull() ?: 0.0
                operator = op
                currentExpression = formatNumber(firstNumber) + " " + operator
                currentNumber = ""
                waitingForSecondNumber = true

                tvResult.text = currentExpression
                tvExpression.text = formatNumber(firstNumber)
                return
            } else if (waitingForSecondNumber) {
                //меняем оператор
                operator = op
                currentExpression = formatNumber(firstNumber) + " " + operator
                tvResult.text = currentExpression
                return
            }
        } else if (operator.isNotEmpty() && currentNumber.isEmpty() && !waitingForSecondNumber) {
            //новая операция с результата
            operator = op
            currentExpression = formatNumber(firstNumber) + " " + operator
            waitingForSecondNumber = true
            tvResult.text = currentExpression
            return
        }
    }

    private fun updateExpressionAndIntermediate() {
        if (operator.isNotEmpty() && currentNumber.isNotEmpty()) {
            //полное выражение в большой строке - но пока там только два коэффициента и оператор??
            currentExpression = formatNumber(firstNumber) + " " + operator + " " + currentNumber
            tvResult.text = currentExpression

            //промежуточный результат
            val secondNumber = currentNumber.toDoubleOrNull() ?: 0.0
            val intermediateResult = when (operator) {
                "+" -> firstNumber + secondNumber
                "-" -> firstNumber - secondNumber
                "×" -> firstNumber * secondNumber
                "÷" -> {
                    if (secondNumber != 0.0) {
                        firstNumber / secondNumber
                    } else {
                        firstNumber
                    }
                }
                else -> firstNumber
            }
            //промежуточный результат в маленькой строке
            tvExpression.text = formatNumber(intermediateResult)
        } else if (operator.isEmpty() && currentNumber.isNotEmpty()) {
            currentExpression = currentNumber
            tvResult.text = currentExpression
            tvExpression.text = currentNumber
        }
    }

    private fun calculate() {
        if (operator.isEmpty() || currentNumber.isEmpty()) {
            return
        }

        try {
            val secondNumber = currentNumber.toDoubleOrNull() ?: return

            val result = when (operator) {
                "+" -> firstNumber + secondNumber
                "-" -> firstNumber - secondNumber
                "×" -> firstNumber * secondNumber
                "÷" -> {
                    if (secondNumber != 0.0) {
                        firstNumber / secondNumber
                    } else {
                        tvExpression.text = "Ошибка"
                        return
                    }
                }
                else -> return
            }

            //сохраняем результат
            lastResult = result
            val formattedResult = formatNumber(result)

            //после нажатия = показываем результат в обеих строках
            tvResult.text = formattedResult
            tvExpression.text = formattedResult

            //сбрасываем состояние но сохраняем результат
            currentNumber = ""
            currentExpression = formattedResult
            operator = ""
            firstNumber = result
            waitingForSecondNumber = false

        } catch (e: Exception) {
            tvExpression.text = "Ошибка"
            onClearClick()
        }
    }

    private fun onEqualsClick() {
        if (operator.isNotEmpty() && currentNumber.isNotEmpty()) {
            calculate()
        } else if (operator.isEmpty() && currentNumber.isEmpty() && lastResult != 0.0) {
            //если без ввода то последний результат
            tvResult.text = formatNumber(lastResult)
            tvExpression.text = formatNumber(lastResult)
        }
    }

    private fun onClearClick() {
        currentNumber = ""
        currentExpression = ""
        operator = ""
        firstNumber = 0.0
        lastResult = 0.0
        waitingForSecondNumber = false
        tvResult.text = "0"
        tvExpression.text = ""
    }

    private fun onDecimalClick() {
        if (waitingForSecondNumber) {
            currentNumber = ""
            waitingForSecondNumber = false
        }

        if (!currentNumber.contains(".")) {
            if (currentNumber.isEmpty()) {
                currentNumber = "0."
            } else {
                currentNumber += "."
            }
            updateExpressionAndIntermediate()
        }
    }

    private fun formatNumber(number: Double): String {
        return try {
            if (number == number.toLong().toDouble()) {
                number.toLong().toString()
            } else {
                val df = DecimalFormat("#.##########")
                df.format(number)
            }
        } catch (e: Exception) {
            number.toString()
        }
    }

    private fun updateDisplay() {
        if (currentExpression.isEmpty() && currentNumber.isEmpty() && lastResult == 0.0) {
            tvResult.text = "0"
            tvExpression.text = ""
        }
    }
}