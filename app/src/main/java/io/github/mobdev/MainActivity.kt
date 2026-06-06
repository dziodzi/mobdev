package io.github.mobdev

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.mobdev.R
import io.github.mobdev.databinding.ActivityMainBinding
import io.github.mobdev.ui.AppScreen
import io.github.mobdev.ui.ChatUiState
import io.github.mobdev.ui.ChatViewModel
import io.github.mobdev.ui.SelectChatFragment
import io.github.mobdev.ui.chats.ChatListFragment
import io.github.mobdev.ui.image.ImageFragment
import io.github.mobdev.ui.login.LoginFragment
import io.github.mobdev.ui.messages.MessagesFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ChatViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBack()
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingOverlay.isVisible =
                        state.skipLoginScreen && state.isLoading &&
                        (state.screen == AppScreen.LOGIN || !state.channelsLoaded)
                    render(state)
                    state.errorDialogMessage?.let { message ->
                        showErrorDialog(message)
                        viewModel.dismissError()
                    }
                }
            }
        }
    }

    private fun render(state: ChatUiState) {
        if (isLandscape()) {
            renderLandscape(state)
        } else {
            renderPortrait(state)
        }
    }

    private fun renderPortrait(state: ChatUiState) {
        binding.landscapeContainer.isVisible = false
        binding.portraitContainer.isVisible = true

        val fragment = when (state.screen) {
            AppScreen.LOGIN -> if (state.skipLoginScreen && state.isLoading) null else LoginFragment()
            AppScreen.CHATS -> ChatListFragment()
            AppScreen.MESSAGES -> MessagesFragment()
            AppScreen.IMAGE -> ImageFragment()
        }

        if (fragment != null) {
            showFragment(binding.portraitContainer.id, fragment, PORTRAIT_TAG)
        } else {
            clearFragment(binding.portraitContainer.id)
        }
    }

    private fun renderLandscape(state: ChatUiState) {
        binding.portraitContainer.isVisible = false
        binding.landscapeContainer.isVisible = true

        if (state.screen == AppScreen.LOGIN && !state.skipLoginScreen) {
            binding.landscapeFullContainer.isVisible = true
            binding.landscapeSplit.isVisible = false
            showFragment(binding.landscapeFullContainer.id, LoginFragment(), LANDSCAPE_LOGIN_TAG)
            return
        }

        if (state.skipLoginScreen && state.isLoading && !state.channelsLoaded) {
            binding.landscapeFullContainer.isVisible = false
            binding.landscapeSplit.isVisible = false
            clearFragment(binding.landscapeFullContainer.id)
            return
        }

        binding.landscapeFullContainer.isVisible = false
        binding.landscapeSplit.isVisible = true
        clearFragment(binding.landscapeFullContainer.id)
        showFragment(binding.chatListContainer.id, ChatListFragment(), CHAT_LIST_TAG)

        val detailFragment: Fragment = when {
            state.screen == AppScreen.IMAGE -> ImageFragment()
            state.selectedChannel != null -> MessagesFragment()
            else -> SelectChatFragment()
        }
        showFragment(binding.messagesContainer.id, detailFragment, MESSAGES_TAG)
    }

    private fun showFragment(containerId: Int, fragment: Fragment, tag: String) {
        val current = supportFragmentManager.findFragmentByTag(tag)
        if (current != null && current::class == fragment::class) return
        supportFragmentManager.commit {
            replace(containerId, fragment, tag)
        }
    }

    private fun clearFragment(containerId: Int) {
        supportFragmentManager.findFragmentById(containerId)?.let { fragment ->
            supportFragmentManager.commit { remove(fragment) }
        }
    }

    private fun handleBack() {
        val state = viewModel.uiState.value
        when {
            state.screen == AppScreen.IMAGE -> viewModel.closeImage()
            isLandscape() && state.selectedChannel != null -> viewModel.closeChatInLandscape()
            !isLandscape() && state.screen == AppScreen.MESSAGES -> viewModel.navigateToChats()
            else -> finish()
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun isLandscape(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    companion object {
        private const val PORTRAIT_TAG = "portrait"
        private const val CHAT_LIST_TAG = "chat_list"
        private const val MESSAGES_TAG = "messages"
        private const val LANDSCAPE_LOGIN_TAG = "landscape_login"
    }
}
