package io.github.mobdev.ui.chats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.mobdev.R
import io.github.mobdev.databinding.ItemChannelBinding

data class ChannelItem(
    val name: String,
    val isSelected: Boolean,
)

class ChannelListAdapter(
    private val onChannelClick: (String) -> Unit,
) : ListAdapter<ChannelItem, ChannelListAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemChannelBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChannelItem) {
            binding.channelName.text = item.name
            val backgroundColor = if (item.isSelected) {
                ContextCompat.getColor(binding.root.context, R.color.channel_selected)
            } else {
                ContextCompat.getColor(binding.root.context, android.R.color.transparent)
            }
            binding.root.setBackgroundColor(backgroundColor)
            binding.root.setOnClickListener { onChannelClick(item.name) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChannelItem>() {
        override fun areItemsTheSame(oldItem: ChannelItem, newItem: ChannelItem): Boolean =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: ChannelItem, newItem: ChannelItem): Boolean =
            oldItem == newItem
    }
}
