package io.github.mobdev.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.mobdev.databinding.FragmentLoginBinding
import io.github.mobdev.ui.AppScreen
import io.github.mobdev.ui.ChatViewModel
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            viewModel.updateUsername(binding.inputUsername.text?.toString().orEmpty())
            viewModel.updatePassword(binding.inputPassword.text?.toString().orEmpty())
            viewModel.login()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.skipLoginScreen && state.screen != AppScreen.LOGIN) {
                        return@collect
                    }
                    if (!binding.inputUsername.isFocused) {
                        binding.inputUsername.setText(state.username)
                    }
                    if (!binding.inputPassword.isFocused) {
                        binding.inputPassword.setText(state.password)
                    }
                    binding.progress.isVisible = state.isLoading
                    binding.btnLogin.isEnabled = !state.isLoading
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
