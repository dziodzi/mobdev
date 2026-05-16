package ru.yourname.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.floor

class MainActivity : AppCompatActivity() {

    private var displayValue: String = "0"
    private var firstOperand: Double? = null
    private var pendingOp: String? = null
    private var justEvaluated: Boolean = false

    private lateinit var tvDisplay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)

        savedInstanceState?.let {
            displayValue  = it.getString("displayValue", "0")
            firstOperand  = it.getDouble("firstOperand", Double.NaN).takeIf { v -> !v.isNaN() }
            pendingOp     = it.getString("pendingOp", null)
            justEvaluated = it.getBoolean("justEvaluated", false)
        }

        updateDisplay()
        bindButtons()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("displayValue", displayValue)
        outState.putDouble("firstOperand", firstOperand ?: Double.NaN)
        outState.putString("pendingOp", pendingOp)
        outState.putBoolean("justEvaluated", justEvaluated)
    }

    private fun bindButtons() {
        listOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        ).forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener { onDigit(digit) }
        }

        findViewById<Button>(R.id.btnDot).setOnClickListener { onDot() }

        listOf(
            R.id.btnPlus  to "+", R.id.btnMinus to "-",
            R.id.btnMul   to "*", R.id.btnDiv   to "/"
        ).forEach { (id, op) ->
            findViewById<Button>(id).setOnClickListener { onOperator(op) }
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener    { onEquals()   }
        findViewById<Button>(R.id.btnClear).setOnClickListener     { onClear()    }
        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener { onPlusMinus() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener   { onPercent()  }
    }

    private fun onDigit(d: String) {
        if (justEvaluated) { displayValue = "0"; justEvaluated = false }
        displayValue = if (displayValue == "0") d else displayValue + d
        updateDisplay()
    }

    private fun onDot() {
        if (justEvaluated) { displayValue = "0"; justEvaluated = false }
        if (!displayValue.contains('.')) displayValue += "."
        updateDisplay()
    }

    private fun onOperator(op: String) {
        val current = displayValue.toDoubleOrNull() ?: return
        if (firstOperand != null && !justEvaluated) {
            val result = calculate(firstOperand!!, current, pendingOp!!)
            displayValue = formatResult(result)
            firstOperand = result
        } else {
            firstOperand = current
        }
        pendingOp     = op
        justEvaluated = true
        updateDisplay()
    }

    private fun onEquals() {
        val current = displayValue.toDoubleOrNull() ?: return
        val a  = firstOperand ?: return
        val op = pendingOp    ?: return
        val result = calculate(a, current, op)
        displayValue  = formatResult(result)
        firstOperand  = null
        pendingOp     = null
        justEvaluated = true
        updateDisplay()
    }

    private fun onClear() {
        displayValue  = "0"
        firstOperand  = null
        pendingOp     = null
        justEvaluated = false
        updateDisplay()
    }

    private fun onPlusMinus() {
        val v = displayValue.toDoubleOrNull() ?: return
        displayValue = formatResult(-v)
        updateDisplay()
    }

    private fun onPercent() {
        val v = displayValue.toDoubleOrNull() ?: return
        displayValue = formatResult(v / 100.0)
        updateDisplay()
    }

    private fun calculate(a: Double, b: Double, op: String): Double = when (op) {
        "+" -> a + b
        "-" -> a - b
        "*" -> a * b
        "/" -> if (b != 0.0) a / b else Double.NaN
        else -> b
    }

    private fun formatResult(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "Error"
        return if (v == floor(v) && !v.isInfinite())
            v.toLong().toString()
        else
            v.toBigDecimal().stripTrailingZeros().toPlainString()
    }

    private fun updateDisplay() {
        tvDisplay.text = displayValue
    }
}
