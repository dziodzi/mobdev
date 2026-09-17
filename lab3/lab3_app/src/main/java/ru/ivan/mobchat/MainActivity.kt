package ru.ivan.mobchat

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), ChatNavigator {
    private val viewModel: ChatViewModel by viewModels()
    private val twoPane: Boolean get() = resources.getBoolean(R.bool.is_two_pane)
    private var renderPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = resources.getBoolean(R.bool.light_system_bars)
            isAppearanceLightNavigationBars = resources.getBoolean(R.bool.light_system_bars)
        }

        viewModel.state.observe(this) { scheduleRender() }
        viewModel.events.observe(this) { wrapper ->
            when (wrapper.getContentIfNotHandled()) {
                UiEvent.InvalidCredentials -> showLoginError()
                UiEvent.NetworkError -> showMessage(R.string.network_error)
                UiEvent.ServerError -> showMessage(R.string.server_error)
                UiEvent.Unauthorized -> MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.session_expired)
                    .setPositiveButton(android.R.string.ok, null).show()
                null -> Unit
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.state.value?.selectedChannel != null) viewModel.closeChannel() else finish()
            }
        })
    }

    override fun onPostResume() {
        super.onPostResume()
        scheduleRender()
    }

    private fun scheduleRender() {
        if (renderPending) return
        renderPending = true
        findViewById<android.view.View>(R.id.primary_container).post {
            renderPending = false
            if (!isFinishing && !isDestroyed) viewModel.state.value?.let { render(it) }
        }
    }

    private fun render(state: ChatUiState) {
        if (supportFragmentManager.isStateSaved) return
        findViewById<android.view.View>(R.id.detail_container).visibility =
            if (twoPane && state.authenticated) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<android.view.View>(R.id.pane_divider)?.visibility =
            if (twoPane && state.authenticated) android.view.View.VISIBLE else android.view.View.GONE
        if (!state.authenticated) {
            (supportFragmentManager.findFragmentByTag(TAG_IMAGE) as? FullscreenImageDialog)?.dismiss()
            showInPrimary(LoginFragment(), TAG_LOGIN)
            showInDetail(PlaceholderFragment(), TAG_PLACEHOLDER)
            return
        }

        if (twoPane) {
            showInPrimary(ChannelsFragment(), TAG_CHANNELS)
            val channel = state.selectedChannel
            if (channel == null) showInDetail(PlaceholderFragment(), TAG_PLACEHOLDER)
            else showInDetail(MessagesFragment.newInstance(channel), messageTag(channel))
        } else {
            val channel = state.selectedChannel
            if (channel == null) showInPrimary(ChannelsFragment(), TAG_CHANNELS)
            else showInPrimary(MessagesFragment.newInstance(channel), messageTag(channel))
            showInDetail(PlaceholderFragment(), TAG_PLACEHOLDER)
        }
    }

    private fun showInPrimary(fragment: Fragment, tag: String) {
        val containerTag = "primary:$tag"
        if (supportFragmentManager.findFragmentById(R.id.primary_container)?.tag == containerTag) return
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.primary_container, fragment, containerTag)
            .commitNow()
    }

    private fun showInDetail(fragment: Fragment, tag: String) {
        val containerTag = "detail:$tag"
        if (supportFragmentManager.findFragmentById(R.id.detail_container)?.tag == containerTag) return
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.detail_container, fragment, containerTag)
            .commitNow()
    }

    override fun openChannel(channel: String) = viewModel.selectChannel(channel)

    override fun closeChannel() = viewModel.closeChannel()

    override fun openImage(path: String) {
        if (!supportFragmentManager.isStateSaved && supportFragmentManager.findFragmentByTag(TAG_IMAGE) == null) {
            FullscreenImageDialog.newInstance(path).showNow(supportFragmentManager, TAG_IMAGE)
        }
    }

    override fun isTwoPane(): Boolean = twoPane

    private fun showLoginError() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.login_error)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showMessage(message: Int) {
        Snackbar.make(findViewById(R.id.primary_container), message, Snackbar.LENGTH_LONG).show()
    }

    private fun messageTag(channel: String) = "messages:$channel"

    companion object {
        private const val TAG_LOGIN = "login"
        private const val TAG_CHANNELS = "channels"
        private const val TAG_PLACEHOLDER = "placeholder"
        private const val TAG_IMAGE = "image"
    }
}

interface ChatNavigator {
    fun openChannel(channel: String)
    fun closeChannel()
    fun openImage(path: String)
    fun isTwoPane(): Boolean
}
