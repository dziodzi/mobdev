package com.helpmethen.chat.ui.screens.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.helpmethen.chat.R
import com.helpmethen.chat.data.remote.NetworkModule
import com.helpmethen.chat.data.repository.ChatRepository
import com.helpmethen.chat.data.session.ChatSession
import com.helpmethen.chat.data.session.SessionStore
import com.helpmethen.chat.databinding.FragmentLoginBinding
import com.helpmethen.chat.ui.screens.chats.ChatsFragment
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val repository = ChatRepository(NetworkModule.chatApi)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.loginButton.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val username = binding.loginEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.empty_login_password))
            return
        }

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.login(username, password)
            setLoading(false)

            result
                .onSuccess { token -> openChat(token, username) }
                .onFailure { exception -> showLoginError(exception.message) }
        }
    }

    private fun openChat(token: String, username: String) {
        ChatSession.setLogin(token, username)
        SessionStore.save(requireContext(), token, username)
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, ChatsFragment.newInstance(token, username))
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        binding.loginEditText.isEnabled = !isLoading
        binding.passwordEditText.isEnabled = !isLoading
        binding.loginProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showLoginError(message: String?) {
        val text = if (message == "Invalid username or password") {
            getString(R.string.wrong_login_password)
        } else {
            getString(R.string.login_network_error)
        }

        showError(text)
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error_title)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
