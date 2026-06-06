package io.github.mobdev.ui.chats

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
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.mobdev.databinding.FragmentChatListBinding
import io.github.mobdev.ui.ChatViewModel
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }
    private lateinit var adapter: ChannelListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChannelListAdapter { channel ->
            viewModel.selectChannel(channel)
        }
        binding.channelsList.layoutManager = LinearLayoutManager(requireContext())
        binding.channelsList.adapter = adapter

        binding.btnLogout.setOnClickListener { viewModel.logout() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val items = state.channels.map { name ->
                        ChannelItem(name, name == state.selectedChannel)
                    }
                    adapter.submitList(items)
                    binding.offlineBanner.isVisible = !state.isOnline
                    binding.progress.isVisible = state.isLoading && state.channels.isEmpty()
                    binding.emptyView.isVisible = !state.isLoading && state.channels.isEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
