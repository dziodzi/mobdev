package com.helpmethen.chat.ui.screens.chat

import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.helpmethen.chat.R
import com.helpmethen.chat.data.cache.ChatCache
import com.helpmethen.chat.data.model.MessageDto
import com.helpmethen.chat.data.remote.ImageLoader
import com.helpmethen.chat.data.network.NetworkMonitor
import com.helpmethen.chat.data.remote.NetworkModule
import com.helpmethen.chat.data.repository.ChatRepository
import com.helpmethen.chat.data.repository.UnauthorizedException
import com.helpmethen.chat.data.session.ChatSession
import com.helpmethen.chat.data.session.SessionStore
import com.helpmethen.chat.databinding.FragmentChatBinding
import com.helpmethen.chat.ui.navigation.BackPressHandler
import com.helpmethen.chat.ui.screens.login.LoginFragment
import com.helpmethen.chat.ui.screens.image.ImageFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatFragment : Fragment(), BackPressHandler {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val repository = ChatRepository(NetworkModule.chatApi)
    private lateinit var token: String
    private lateinit var username: String
    private lateinit var channelName: String
    private var isEmbedded: Boolean = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var waitingForNetwork = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = ChatSession.token ?: requireArguments().getString(ARG_TOKEN).orEmpty()
        username = ChatSession.username ?: requireArguments().getString(ARG_USERNAME).orEmpty()
        channelName = requireArguments().getString(ARG_CHANNEL).orEmpty()
        ChatSession.selectedChannel = channelName
        isEmbedded = requireArguments().getBoolean(ARG_EMBEDDED, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.userTextView.text = getString(R.string.logged_in_as, username)
        binding.channelTitleTextView.text = channelName
        binding.backButton.visibility = if (isEmbedded) View.GONE else View.VISIBLE
        binding.backButton.setOnClickListener {
            closeChat()
        }
        binding.refreshButton.setOnClickListener {
            loadMessages(forceNetwork = true)
        }
        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        setupSidePanel()
        updateNetworkControls()
        loadMessages()
    }

    override fun onStart() {
        super.onStart()
        networkCallback = NetworkMonitor.register(requireContext()) {
            _binding?.root?.post {
                updateNetworkControls()
                if (waitingForNetwork) {
                    waitingForNetwork = false
                    loadChannelsIntoSidePanel(forceNetwork = true)
                    loadMessages(forceNetwork = true)
                }
            }
        }
    }

    override fun onStop() {
        NetworkMonitor.unregister(requireContext(), networkCallback)
        networkCallback = null
        super.onStop()
    }

    private fun setupSidePanel() {
        val sidePanel = view?.findViewById<LinearLayout>(R.id.chatSidePanel) ?: return
        if (isEmbedded) {
            sidePanel.visibility = View.GONE
            return
        }

        loadChannelsIntoSidePanel()
    }

    private fun loadChannelsIntoSidePanel(forceNetwork: Boolean = false) {
        ChatSession.channels?.let { channels ->
            showSideChannels(channels)
            if (!forceNetwork) {
                return
            }
        }

        if (ChatSession.channels == null) {
            val diskChannels = ChatCache.loadChannels(requireContext())
            if (diskChannels.isNotEmpty()) {
                ChatSession.channels = diskChannels
                showSideChannels(diskChannels)
            }
        }

        if (!NetworkMonitor.isOnline(requireContext())) {
            waitingForNetwork = true
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.getChannels()
                .onSuccess { channels ->
                    ChatSession.channels = channels
                    ChatCache.saveChannels(requireContext(), channels)
                    showSideChannels(channels)
                }
                .onFailure { exception -> handleFailure(exception, R.string.channels_load_error) }
        }
    }

    private fun showSideChannels(channels: List<String>) {
        val container = view?.findViewById<LinearLayout>(R.id.sideChatsContainer) ?: return
        container.removeAllViews()

        channels.forEach { channel ->
            val isSelected = channel == channelName
            val row = createSideTextRow(channel, isSelected, channels)

            container.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun createSideTextRow(
        channel: String,
        isSelected: Boolean,
        channels: List<String>
    ): TextView {
        return TextView(requireContext()).apply {
            text = channel
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isSelected) R.color.selected_chat else R.color.surface
                )
            )
            if (isSelected) {
                setTypeface(typeface, Typeface.BOLD)
            }
            setOnClickListener { switchChannel(channel, channels) }
        }
    }

    private fun switchChannel(channel: String, channels: List<String>) {
        channelName = channel
        ChatSession.selectedChannel = channel
        binding.channelTitleTextView.text = channelName
        showSideChannels(channels)
        loadMessages()
    }

    private fun loadMessages(forceNetwork: Boolean = false) {
        val cachedMessages = ChatSession.messagesByChannel[channelName]
        if (!forceNetwork && cachedMessages != null) {
            showMessages(cachedMessages)
            return
        }

        if (cachedMessages == null) {
            val diskMessages = ChatCache.loadMessages(requireContext(), channelName)
            if (diskMessages.isNotEmpty()) {
                ChatSession.messagesByChannel[channelName] = diskMessages
                showMessages(diskMessages)
            }
        }

        if (!NetworkMonitor.isOnline(requireContext())) {
            waitingForNetwork = true
            binding.messagesProgressBar.visibility = View.GONE
            updateNetworkControls()
            return
        }

        binding.messagesProgressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getMessages(channelName)
            binding.messagesProgressBar.visibility = View.GONE

            result
                .onSuccess { messages ->
                    ChatSession.messagesByChannel[channelName] = messages
                    ChatCache.saveMessages(requireContext(), channelName, messages)
                    showMessages(messages)
                }
                .onFailure { exception -> handleFailure(exception, R.string.messages_load_error) }
        }
    }

    private fun showMessages(messages: List<MessageDto>) {
        val uniqueMessages = messages.distinctBy { message -> message.id }
        binding.messagesContainer.removeAllViews()

        if (uniqueMessages.isEmpty()) {
            showEmptyState(getString(R.string.no_messages))
            return
        }

        binding.emptyStateTextView.visibility = View.GONE
        uniqueMessages.forEach { message ->
            binding.messagesContainer.addView(createMessageView(message))
        }

        binding.messagesScrollView.post {
            binding.messagesScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun createMessageView(message: MessageDto): View {
        val isMine = message.from == username
        val bubble = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundResource(
                if (isMine) R.drawable.bg_message_mine else R.drawable.bg_message_other
            )
        }

        val authorView = TextView(requireContext()).apply {
            text = getString(R.string.message_author, message.from, message.time)
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        bubble.addView(authorView)
        addMessageContent(bubble, message)

        val row = LinearLayout(requireContext()).apply {
            gravity = if (isMine) Gravity.END else Gravity.START
            setPadding(0, dp(5), 0, dp(5))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(
                bubble,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        return row
    }

    private fun sendMessage() {
        val text = binding.messageEditText.text.toString().trim()

        if (text.isEmpty()) {
            return
        }

        if (!NetworkMonitor.isOnline(requireContext())) {
            showError(getString(R.string.offline_send_blocked))
            updateNetworkControls()
            waitingForNetwork = true
            return
        }

        binding.sendButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.sendMessage(
                token = token,
                from = username,
                to = channelName,
                text = text
            )
            binding.sendButton.isEnabled = true

            result
                .onSuccess {
                    binding.messageEditText.text?.clear()
                    ChatSession.lastMessagePreviews[channelName] =
                        getString(R.string.last_message_preview, username, text)
                    ChatCache.saveLastMessagePreviews(requireContext(), ChatSession.lastMessagePreviews)
                    loadMessages(forceNetwork = true)
                }
                .onFailure { exception -> handleFailure(exception, R.string.message_send_error) }
        }
    }

    private fun showEmptyState(message: String) {
        binding.messagesContainer.removeAllViews()
        binding.emptyStateTextView.text = message
        binding.emptyStateTextView.visibility = View.VISIBLE
    }

    private fun setLoading(isLoading: Boolean) {
        binding.refreshButton.isEnabled = !isLoading
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
            updateNetworkControls()
        } else {
            showError(getString(messageResId))
        }
    }

    private fun restartLogin() {
        ChatSession.clear()
        SessionStore.clear(requireContext())
        (requireActivity() as AppCompatActivity).supportFragmentManager.commit {
            replace(R.id.fragmentContainer, LoginFragment())
        }
    }

    private fun updateNetworkControls() {
        val isOnline = NetworkMonitor.isOnline(requireContext())
        binding.sendButton.isEnabled = isOnline
        binding.messageEditText.isEnabled = isOnline
    }

    private fun addMessageContent(bubble: LinearLayout, message: MessageDto) {
        val text = message.data.Text?.text
        val imagePath = message.data.Image?.link

        if (!text.isNullOrBlank()) {
            bubble.addView(
                TextView(requireContext()).apply {
                    this.text = text
                    textSize = 16f
                    maxWidth = (resources.displayMetrics.widthPixels * 0.65f).toInt()
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                }
            )
            return
        }

        if (!imagePath.isNullOrBlank()) {
            val imageView = ImageView(requireContext()).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
                setOnClickListener { openImage(imagePath) }
            }

            bubble.addView(
                imageView,
                LinearLayout.LayoutParams(dp(220), dp(150))
            )
            loadImage(imageView, imageUrl("thumb", imagePath))
            return
        }

        bubble.addView(
            TextView(requireContext()).apply {
                setText(R.string.unsupported_message)
                textSize = 16f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        )
    }

    private fun loadImage(imageView: ImageView, url: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            ChatSession.images[url]?.let { cachedBitmap ->
                imageView.setImageBitmap(cachedBitmap)
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                ImageLoader.loadBitmap(url)
            }

            result
                .onSuccess { bitmap ->
                    if (bitmap == null) {
                        imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                    } else {
                        ChatSession.images[url] = bitmap
                        imageView.setImageBitmap(bitmap)
                    }
                }
                .onFailure { exception ->
                    handleFailure(exception, R.string.messages_load_error)
                }
        }
    }

    private fun openImage(imagePath: String) {
        (requireActivity() as AppCompatActivity).supportFragmentManager.commit {
            replace(R.id.fragmentContainer, ImageFragment.newInstance(imagePath))
            addToBackStack(null)
        }
    }

    private fun closeChat() {
        ChatSession.selectedChannel = null
        parentFragmentManager.popBackStack()
    }

    override fun onBackPressed(): Boolean {
        if (isEmbedded) {
            return false
        }

        closeChat()
        return true
    }

    private fun imageUrl(kind: String, imagePath: String): String {
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath
        }

        return "https://faerytea.name/$kind/${imagePath.trimStart('/').replace(" ", "%20")}"
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
        private const val ARG_CHANNEL = "channel"
        private const val ARG_EMBEDDED = "embedded"

        fun newInstance(
            token: String,
            username: String,
            channelName: String,
            isEmbedded: Boolean = false
        ): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TOKEN, token)
                    putString(ARG_USERNAME, username)
                    putString(ARG_CHANNEL, channelName)
                    putBoolean(ARG_EMBEDDED, isEmbedded)
                }
            }
        }
    }
}
