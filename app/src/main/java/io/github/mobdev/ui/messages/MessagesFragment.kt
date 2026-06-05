package io.github.mobdev.ui.messages

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.mobdev.databinding.FragmentMessagesBinding
import io.github.mobdev.ui.ChatViewModel
import kotlinx.coroutines.launch

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }
    private lateinit var adapter: MessagesAdapter
    private var isLoadingMore = false
    private var lastSeenBottomId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MessagesAdapter { link -> viewModel.openImage(link) }
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.messagesList.layoutManager = layoutManager
        binding.messagesList.adapter = adapter

        binding.messagesList.addOnScrollListener(object :
            androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                if (dy >= 0) return
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible <= 2 && !isLoadingMore) {
                    viewModel.loadMoreMessages()
                }
            }
        })

        binding.btnSend.setOnClickListener {
            val text = binding.inputMessage.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.inputMessage.text?.clear()
            }
        }

        binding.btnBackToChats.setOnClickListener { viewModel.navigateToChats() }

        binding.inputMessage.doAfterTextChanged {
            binding.btnSend.isEnabled = !it.isNullOrBlank() && !viewModel.uiState.value.isSending
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    isLoadingMore = state.isLoadingMore
                    binding.channelTitle.text = state.selectedChannel.orEmpty()
                    binding.btnBackToChats.isVisible =
                        resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
                    val bottomId = state.messages.lastOrNull()?.id
                    adapter.submitList(state.messages.toListItems()) {
                        if (bottomId != null && bottomId != lastSeenBottomId) {
                            binding.messagesList.scrollToPosition(adapter.itemCount - 1)
                        }
                        lastSeenBottomId = bottomId
                    }
                    binding.progress.isVisible = state.isLoading && state.messages.isEmpty()
                    binding.btnSend.isEnabled =
                        !binding.inputMessage.text.isNullOrBlank() && !state.isSending
                    binding.loadMoreProgress.isVisible = state.isLoadingMore
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
