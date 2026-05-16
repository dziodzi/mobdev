package io.github.mobdev

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import io.github.mobdev.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm by viewModels<CalculatorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = top)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = bars.left,
                right = bars.right,
                bottom = bars.bottom,
            )
            insets
        }

        binding.btn0.setOnClickListener { tap { vm.digit(0) } }
        binding.btn1.setOnClickListener { tap { vm.digit(1) } }
        binding.btn2.setOnClickListener { tap { vm.digit(2) } }
        binding.btn3.setOnClickListener { tap { vm.digit(3) } }
        binding.btn4.setOnClickListener { tap { vm.digit(4) } }
        binding.btn5.setOnClickListener { tap { vm.digit(5) } }
        binding.btn6.setOnClickListener { tap { vm.digit(6) } }
        binding.btn7.setOnClickListener { tap { vm.digit(7) } }
        binding.btn8.setOnClickListener { tap { vm.digit(8) } }
        binding.btn9.setOnClickListener { tap { vm.digit(9) } }

        binding.btnDot.setOnClickListener { tap { vm.dot() } }
        binding.btnC.setOnClickListener { tap { vm.clear() } }
        binding.btnBs.setOnClickListener { tap { vm.backspace() } }
        binding.btnEq.setOnClickListener { tap { vm.evaluate() } }

        binding.btnAdd.setOnClickListener { tap { vm.operator(Op.ADD) } }
        binding.btnSub.setOnClickListener { tap { vm.operator(Op.SUB) } }
        binding.btnMul.setOnClickListener { tap { vm.operator(Op.MUL) } }
        binding.btnDiv.setOnClickListener { tap { vm.operator(Op.DIV) } }

        refresh()
    }

    private inline fun tap(block: () -> Unit) {
        block()
        refresh()
    }

    private fun refresh() {
        binding.display.text = vm.displayText()
    }
}
