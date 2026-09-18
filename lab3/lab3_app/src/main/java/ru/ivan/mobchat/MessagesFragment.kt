package ru.ivan.mobchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class MessagesFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private val channel: String by lazy { requireArguments().getString(ARG_CHANNEL).orEmpty() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_messages, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = requireActivity() as ChatNavigator
        val adapter = MessageAdapter { navigator.openImage(it) }.apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        val list = view.findViewById<RecyclerView>(R.id.messages_list)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.messages_refresh)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.messages_toolbar)
        val older = view.findViewById<MaterialButton>(R.id.load_older_button)
        val input = view.findViewById<EditText>(R.id.message_input)
        val send = view.findViewById<MaterialButton>(R.id.send_button)
        val empty = view.findViewById<TextView>(R.id.messages_empty)
        val layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        list.layoutManager = layoutManager
        list.adapter = adapter
        toolbar.title = channel.removeSuffix("@channel")
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationIconTint(requireContext().getColor(R.color.ink))
        toolbar.setNavigationContentDescription(R.string.back_action)
        toolbar.setNavigationOnClickListener { navigator.closeChannel() }
        val refreshItem = toolbar.menu.add(R.string.refresh_action).apply {
            setIcon(R.drawable.ic_refresh)
            icon?.setTint(requireContext().getColor(R.color.ink))
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setOnMenuItemClickListener { viewModel.refreshMessages(); true }
        }
        TooltipCompat.setTooltipText(send, getString(R.string.send_action))
        refresh.setColorSchemeResources(R.color.primary)
        refresh.setOnRefreshListener { viewModel.refreshMessages() }
        older.setOnClickListener { viewModel.loadOlderMessages() }
        input.setText(viewModel.state.value?.draft.orEmpty())
        input.doAfterTextChanged { viewModel.updateDraft(channel, it.toString()) }
        send.setOnClickListener { viewModel.sendMessage(input.text.toString()) }

        var displayedMessages: List<ru.ivan.mobchat.data.ChatMessage>? = null
        var lastSentId = viewModel.state.value?.sentMessageId
        val owner = viewLifecycleOwner
        viewModel.state.observe(viewLifecycleOwner) { uiState ->
            if (uiState.selectedChannel != channel) return@observe
            adapter.username = uiState.username
            if (input.text.toString() != uiState.draft) {
                input.setText(uiState.draft)
                input.setSelection(input.length())
            }
            if (displayedMessages != uiState.messages) {
                val oldList = adapter.currentList
                val firstPosition = layoutManager.findFirstVisibleItemPosition()
                val anchorId = oldList.getOrNull(firstPosition)?.id
                val offset = layoutManager.findViewByPosition(firstPosition)?.top ?: 0
                val atBottom = !list.canScrollVertically(1)
                val sent = uiState.sentMessageId != null && uiState.sentMessageId != lastSentId
                val addedAtStart = oldList.isNotEmpty() &&
                    oldList.firstOrNull()?.id != uiState.messages.firstOrNull()?.id
                displayedMessages = uiState.messages
                lastSentId = uiState.sentMessageId
                adapter.submitList(uiState.messages) {
                    if (owner.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) return@submitList
                    when {
                        sent || (oldList.isEmpty() && savedInstanceState == null) ->
                            if (adapter.itemCount > 0) list.scrollToPosition(adapter.itemCount - 1)
                        addedAtStart && anchorId != null -> {
                            val position = adapter.currentList.indexOfFirst { it.id == anchorId }
                            if (position >= 0) layoutManager.scrollToPositionWithOffset(position, offset)
                        }
                        atBottom && oldList.isNotEmpty() && oldList.lastOrNull()?.id != uiState.messages.lastOrNull()?.id ->
                            if (adapter.itemCount > 0) list.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
            refresh.isRefreshing = uiState.messagesLoading
            refreshItem.isEnabled = !uiState.messagesLoading
            older.isEnabled = !uiState.messagesLoading
            older.visibility = if (uiState.hasMoreMessages && uiState.messages.isNotEmpty()) View.VISIBLE else View.GONE
            send.isEnabled = !uiState.sending && uiState.draft.isNotBlank()
            send.visibility = if (uiState.sending) View.INVISIBLE else View.VISIBLE
            view.findViewById<View>(R.id.send_progress).visibility =
                if (uiState.sending) View.VISIBLE else View.GONE
            empty.setText(if (uiState.messagesError) R.string.load_failed else R.string.empty_messages)
            empty.visibility = if (!uiState.messagesLoading && uiState.messages.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    companion object {
        private const val ARG_CHANNEL = "channel"
        fun newInstance(channel: String) = MessagesFragment().apply {
            arguments = Bundle().apply { putString(ARG_CHANNEL, channel) }
        }
    }
}
