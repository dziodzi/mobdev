package ru.ivan.mobchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar

class ChannelsFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_channels, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = requireActivity() as ChatNavigator
        val adapter = ChannelAdapter { navigator.openChannel(it) }
        val list = view.findViewById<RecyclerView>(R.id.channels_list)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.channels_refresh)
        val empty = view.findViewById<TextView>(R.id.channels_empty)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.channels_toolbar)

        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        refresh.setColorSchemeResources(R.color.primary)
        refresh.setOnRefreshListener { viewModel.loadChannels(force = true) }
        toolbar.menu.add(R.string.logout_action).apply {
            setIcon(R.drawable.ic_logout)
            icon?.setTint(requireContext().getColor(R.color.ink))
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setOnMenuItemClickListener { viewModel.logout(); true }
        }
        toolbar.menu.add(R.string.refresh_action).apply {
            setIcon(R.drawable.ic_refresh)
            icon?.setTint(requireContext().getColor(R.color.ink))
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setOnMenuItemClickListener { viewModel.loadChannels(force = true); true }
        }

        viewModel.state.observe(viewLifecycleOwner) { uiState ->
            adapter.submit(uiState.channels, uiState.selectedChannel)
            toolbar.subtitle = if (uiState.offline) {
                "${uiState.username} · ${getString(R.string.offline_status)}"
            } else {
                uiState.username
            }
            empty.setText(if (uiState.channelsError) R.string.load_failed else R.string.empty_channels)
            refresh.isRefreshing = uiState.channelsLoading
            empty.visibility = if (!uiState.channelsLoading && uiState.channels.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
