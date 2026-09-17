package ru.ivan.mobchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val login = view.findViewById<TextInputEditText>(R.id.login_input)
        val password = view.findViewById<TextInputEditText>(R.id.password_input)
        val button = view.findViewById<MaterialButton>(R.id.login_button)
        val progress = view.findViewById<View>(R.id.login_progress)

        val loginLayout = view.findViewById<TextInputLayout>(R.id.login_layout)
        val passwordLayout = view.findViewById<TextInputLayout>(R.id.password_layout)
        fun submit() {
            val name = login.text?.toString().orEmpty()
            val secret = password.text?.toString().orEmpty()
            loginLayout.error = if (name.isBlank()) getString(R.string.required_field) else null
            passwordLayout.error = if (secret.isBlank()) getString(R.string.required_field) else null
            if (name.isNotBlank() && secret.isNotBlank()) viewModel.login(name, secret)
        }
        login.doAfterTextChanged { loginLayout.error = null }
        password.doAfterTextChanged { passwordLayout.error = null }
        button.setOnClickListener { submit() }
        view.findViewById<View>(R.id.register_button).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://faerytea.name/adduser.html")))
            } catch (_: ActivityNotFoundException) {
                com.google.android.material.snackbar.Snackbar.make(
                    view, R.string.browser_unavailable, com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
        }
        password.setOnEditorActionListener { _, action, _ ->
            (action == EditorInfo.IME_ACTION_DONE).also { if (it) submit() }
        }

        viewModel.state.observe(viewLifecycleOwner) { uiState ->
            progress.visibility = if (uiState.loginLoading) View.VISIBLE else View.INVISIBLE
            button.isEnabled = !uiState.loginLoading
            login.isEnabled = !uiState.loginLoading
            password.isEnabled = !uiState.loginLoading
        }
    }
}
