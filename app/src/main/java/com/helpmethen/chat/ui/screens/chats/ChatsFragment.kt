package com.helpmethen.chat.ui.screens.chats

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.helpmethen.chat.R
import com.helpmethen.chat.data.cache.ChatCache
import com.helpmethen.chat.data.model.MessageDto
import com.helpmethen.chat.data.network.NetworkMonitor
import com.helpmethen.chat.data.remote.NetworkModule
import com.helpmethen.chat.data.repository.ChatRepository
import com.helpmethen.chat.data.repository.UnauthorizedException
import com.helpmethen.chat.data.session.ChatSession
import com.helpmethen.chat.data.session.SessionStore
import com.helpmethen.chat.databinding.FragmentChatsBinding
import com.helpmethen.chat.ui.navigation.BackPressHandler
import com.helpmethen.chat.ui.screens.chat.ChatFragment
import com.helpmethen.chat.ui.screens.login.LoginFragment
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ChatsFragment : Fragment(), BackPressHandler {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!

    private val repository = ChatRepository(NetworkModule.chatApi)
    private lateinit var token: String
    private lateinit var username: String
    private var channels: List<String> = emptyList()
    private var lastMessagePreviews: Map<String, String> = emptyMap()
    private var selectedChannel: String? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var waitingForNetwork = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = ChatSession.token ?: requireArguments().getString(ARG_TOKEN).orEmpty()
        username = ChatSession.username ?: requireArguments().getString(ARG_USERNAME).orEmpty()
        selectedChannel = ChatSession.selectedChannel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.userTextView.text = getString(R.string.logged_in_as, username)
        binding.refreshButton.setOnClickListener {
            loadChannels(forceNetwork = true)
        }
        binding.logoutButton.setOnClickListener {
            logout()
        }

        loadChannels()
        restoreSelectedChatIfNeeded()
    }

    override fun onStart() {
        super.onStart()
        networkCallback = NetworkMonitor.register(requireContext()) {
            _binding?.root?.post {
                if (waitingForNetwork) {
                    waitingForNetwork = false
                    loadChannels(forceNetwork = true)
                }
            }
        }
    }

    override fun onStop() {
        NetworkMonitor.unregister(requireContext(), networkCallback)
        networkCallback = null
        super.onStop()
    }

    private fun loadChannels(forceNetwork: Boolean = false) {
        ChatSession.channels?.let { cachedChannels ->
            channels = cachedChannels
            lastMessagePreviews = ChatSession.lastMessagePreviews.toMap()
            showChannels()
            restoreSelectedChatIfNeeded()
            loadMissingLastMessagePreviews()
            if (!forceNetwork) {
                return
            }
        }

        if (channels.isEmpty()) {
            val diskChannels = ChatCache.loadChannels(requireContext())
            val diskPreviews = ChatCache.loadLastMessagePreviews(requireContext())
            if (diskChannels.isNotEmpty()) {
                channels = diskChannels
                lastMessagePreviews = diskPreviews
                ChatSession.channels = diskChannels
                ChatSession.lastMessagePreviews.putAll(diskPreviews)
                showChannels()
                restoreSelectedChatIfNeeded()
            }
        }

        if (!NetworkMonitor.isOnline(requireContext())) {
            waitingForNetwork = true
            setLoading(false)
            return
        }

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getChannels()

            result
                .onSuccess { loadedChannels ->
                    channels = loadedChannels
                    ChatSession.channels = loadedChannels
                    ChatCache.saveChannels(requireContext(), loadedChannels)
                    lastMessagePreviews = emptyMap()
                    showChannels()
                    runCatching { loadLastMessagePreviews(loadedChannels) }
                        .onSuccess { previews ->
                            lastMessagePreviews = previews
                            ChatSession.lastMessagePreviews.putAll(previews)
                            ChatCache.saveLastMessagePreviews(requireContext(), lastMessagePreviews)
                            showChannels()
                            restoreSelectedChatIfNeeded()
                        }
                        .onFailure { exception ->
                            handleFailure(exception, R.string.channels_load_error)
                        }
                }
                .onFailure { exception -> handleFailure(exception, R.string.channels_load_error) }

            setLoading(false)
        }
    }

    private fun loadMissingLastMessagePreviews() {
        val missingChannels = channels.filter { channel ->
            !ChatSession.lastMessagePreviews.containsKey(channel)
        }

        if (missingChannels.isEmpty()) {
            return
        }

        if (!NetworkMonitor.isOnline(requireContext())) {
            waitingForNetwork = true
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { loadLastMessagePreviews(missingChannels) }
                .onSuccess { previews ->
                    lastMessagePreviews = lastMessagePreviews + previews
                    ChatSession.lastMessagePreviews.putAll(previews)
                    ChatCache.saveLastMessagePreviews(requireContext(), lastMessagePreviews)
                    showChannels()
                }
                .onFailure { exception ->
                    handleFailure(exception, R.string.channels_load_error)
                }
        }
    }

    private suspend fun loadLastMessagePreviews(channels: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()

        channels.chunked(8).forEach { chunk ->
            val previews = coroutineScope {
                chunk.map { channel ->
                    async {
                        val messageResult = repository.getLastMessage(channel)
                        val preview = if (messageResult.isSuccess) {
                            messageResult.getOrNull()
                                ?.let { message -> messagePreview(message) }
                                ?: getString(R.string.no_messages_short)
                        } else {
                            val exception = messageResult.exceptionOrNull()
                            if (exception is UnauthorizedException) {
                                throw exception
                            }
                            getString(R.string.no_messages_short)
                        }

                        channel to preview
                    }
                }.awaitAll()
            }
            result.putAll(previews)
        }

        return result
    }

    private fun showChannels() {
        binding.chatsContainer.removeAllViews()

        if (channels.isEmpty()) {
            binding.emptyStateTextView.visibility = View.VISIBLE
            return
        }

        binding.emptyStateTextView.visibility = View.GONE
        channels.forEach { channel ->
            val isSelected = channel == selectedChannel
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                minimumHeight = dp(68)
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (isSelected) R.color.selected_chat else R.color.surface
                    )
                )
                foreground = selectableItemBackground()
                setOnClickListener { openChat(channel) }
            }

            val avatarTextView = TextView(requireContext()).apply {
                text = channel.firstOrNull()?.uppercase() ?: "#"
                gravity = android.view.Gravity.CENTER
                textSize = 18f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                background = avatarBackground()
            }

            val titleTextView = TextView(requireContext()).apply {
                text = channel
                textSize = 17f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                setSingleLine(true)
                if (isSelected) {
                    setTypeface(typeface, Typeface.BOLD)
                }
            }

            val subtitleTextView = TextView(requireContext()).apply {
                text = lastMessagePreviews[channel] ?: getString(R.string.loading_last_message)
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                setSingleLine(true)
            }

            val textContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                addView(titleTextView)
                addView(subtitleTextView)
            }

            row.addView(
                avatarTextView,
                LinearLayout.LayoutParams(dp(44), dp(44))
            )
            row.addView(
                textContainer,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(14)
                }
            )

            val divider = View(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#E2E8F0"))
            }

            binding.chatsContainer.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            binding.chatsContainer.addView(
                divider,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {
                    marginStart = dp(74)
                }
            )
        }
    }

    private fun messagePreview(message: MessageDto): String {
        val content = message.data.Text?.text
            ?: message.data.Image?.link?.let { getString(R.string.photo_message_preview) }
            ?: getString(R.string.unsupported_message)

        return getString(R.string.last_message_preview, message.from, content)
    }

    private fun openChat(channel: String) {
        selectedChannel = channel
        ChatSession.selectedChannel = channel
        showChannels()

        val detailContainer = view?.findViewById<FrameLayout>(R.id.chatDetailContainer)
        if (detailContainer == null) {
            parentFragmentManager.commit {
                replace(R.id.fragmentContainer, ChatFragment.newInstance(token, username, channel))
                addToBackStack(null)
            }
        } else {
            view?.findViewById<TextView>(R.id.selectChatTextView)?.visibility = View.GONE
            childFragmentManager.commit {
                replace(
                    R.id.chatDetailContainer,
                    ChatFragment.newInstance(token, username, channel, isEmbedded = true)
                )
            }
        }
    }

    private fun logout() {
        ChatSession.clear()
        SessionStore.clear(requireContext())
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, LoginFragment())
        }
    }

    private fun restoreSelectedChatIfNeeded() {
        val channel = selectedChannel ?: return
        val detailContainer = view?.findViewById<FrameLayout>(R.id.chatDetailContainer)
        if (detailContainer == null) {
            binding.root.post {
                if (!isAdded || ChatSession.selectedChannel != channel) {
                    return@post
                }

                if (parentFragmentManager.findFragmentById(R.id.fragmentContainer) !== this) {
                    return@post
                }

                parentFragmentManager.commit {
                    replace(R.id.fragmentContainer, ChatFragment.newInstance(token, username, channel))
                    addToBackStack(null)
                }
            }
            return
        }

        if (childFragmentManager.findFragmentById(R.id.chatDetailContainer) == null) {
            view?.findViewById<TextView>(R.id.selectChatTextView)?.visibility = View.GONE
            childFragmentManager.commit {
                replace(
                    detailContainer.id,
                    ChatFragment.newInstance(token, username, channel, isEmbedded = true)
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.refreshButton.isEnabled = !isLoading
        binding.logoutButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error_title)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun handleFailure(exception: Throwable, messageResId: Int) {
        if (exception is UnauthorizedException) {
            restartLogin()
        } else if (!NetworkMonitor.isOnline(requireContext())) {
            waitingForNetwork = true
        } else {
            showError(getString(messageResId))
        }
    }

    private fun restartLogin() {
        ChatSession.clear()
        SessionStore.clear(requireContext())
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, LoginFragment())
        }
    }

    override fun onBackPressed(): Boolean {
        val detailContainer = view?.findViewById<FrameLayout>(R.id.chatDetailContainer)
            ?: return false

        if (childFragmentManager.findFragmentById(detailContainer.id) == null) {
            return false
        }

        childFragmentManager.commit {
            remove(childFragmentManager.findFragmentById(detailContainer.id) ?: return@commit)
        }
        selectedChannel = null
        ChatSession.selectedChannel = null
        view?.findViewById<TextView>(R.id.selectChatTextView)?.visibility = View.VISIBLE
        showChannels()
        return true
    }

    private fun selectableItemBackground(): android.graphics.drawable.Drawable? {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(
            android.R.attr.selectableItemBackground,
            typedValue,
            true
        )
        return ContextCompat.getDrawable(requireContext(), typedValue.resourceId)
    }

    private fun avatarBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(requireContext(), R.color.primary))
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TOKEN = "token"
        private const val ARG_USERNAME = "username"

        fun newInstance(token: String, username: String): ChatsFragment {
            return ChatsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TOKEN, token)
                    putString(ARG_USERNAME, username)
                }
            }
        }
    }
}
