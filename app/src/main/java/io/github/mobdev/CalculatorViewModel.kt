package io.github.mobdev

import androidx.lifecycle.ViewModel
import java.util.Locale
import kotlin.math.abs

class CalculatorViewModel : ViewModel() {

    private var display = "0"
    private var acc: Double? = null
    private var pending: Op? = null
    private var awaitingRight = false
    private var lastOp: Op? = null
    private var lastRight: Double? = null

    fun displayText(): String = display

    fun clear() {
        display = "0"
        acc = null
        pending = null
        awaitingRight = false
        lastOp = null
        lastRight = null
    }

    fun backspace() {
        if (awaitingRight) return
        if (display == ERR) {
            clear()
            return
        }
        if (display.length <= 1 || (display.startsWith('-') && display.length == 2)) {
            display = "0"
            return
        }
        display = display.dropLast(1)
        if (display == "-" || display.isEmpty()) display = "0"
    }

    fun digit(d: Int) {
        require(d in 0..9)
        if (display == ERR) clear()
        if (awaitingRight) {
            lastOp = null
            lastRight = null
            display = d.toString()
            awaitingRight = false
            return
        }
        if (display.replace(".", "").replace("-", "").length >= MAX_DIGITS) return
        if (display == "0" && d == 0) return
        if (display == "0" && d != 0) {
            display = d.toString()
            return
        }
        display += d.toString()
    }

    fun dot() {
        if (display == ERR) {
            clear()
            display = "0."
            return
        }
        if (awaitingRight) {
            lastOp = null
            lastRight = null
            display = "0."
            awaitingRight = false
            return
        }
        if (!display.contains('.')) display += "."
    }

    fun operator(next: Op) {
        if (display == ERR) {
            clear()
            return
        }
        lastOp = null
        lastRight = null
        if (pending != null && awaitingRight) {
            pending = next
            return
        }
        val cur = display.toDoubleOrNull() ?: return
        val p = pending
        val leftAcc = acc
        if (p != null && leftAcc != null && !awaitingRight) {
            val r = apply(leftAcc, cur, p) ?: run {
                display = ERR
                acc = null
                pending = null
                awaitingRight = false
                return
            }
            acc = r
            display = format(r)
        } else {
            acc = cur
        }
        pending = next
        awaitingRight = true
    }

    fun evaluate() {
        if (display == ERR) {
            clear()
            return
        }
        if (pending != null && acc != null) {
            val right = display.toDoubleOrNull() ?: return
            val left = acc!!
            val p = pending!!
            val r = apply(left, right, p) ?: run {
                display = ERR
                acc = null
                pending = null
                awaitingRight = false
                lastOp = null
                lastRight = null
                return
            }
            display = format(r)
            lastOp = p
            lastRight = right
            acc = null
            pending = null
            awaitingRight = true
            return
        }
        val lo = lastOp
        val lr = lastRight
        if (lo != null && lr != null) {
            val left = display.toDoubleOrNull() ?: return
            val r = apply(left, lr, lo) ?: run {
                display = ERR
                lastOp = null
                lastRight = null
                awaitingRight = false
                return
            }
            display = format(r)
            awaitingRight = true
        }
    }

    private fun apply(a: Double, b: Double, op: Op): Double? =
        when (op) {
            Op.ADD -> a + b
            Op.SUB -> a - b
            Op.MUL -> a * b
            Op.DIV -> if (nearZero(b)) null else a / b
        }

    private fun nearZero(x: Double) = abs(x) < 1e-15

    private fun format(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return ERR
        if (abs(v) < 1e15 && abs(v - v.toLong()) < 1e-9) return v.toLong().toString()
        val s = String.format(Locale.US, "%.10f", v).trimEnd('0').trimEnd('.')
        if (s == "-0") return "0"
        return s
    }
}

enum class Op {
    ADD,
    SUB,
    MUL,
    DIV,
}

private const val ERR = "Ошибка"
private const val MAX_DIGITS = 14
