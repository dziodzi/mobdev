package io.github.mobdev.ui.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.github.mobdev.R
import io.github.mobdev.api.ImageUrl
import io.github.mobdev.data.ChatMessage
import io.github.mobdev.data.MessageData
import io.github.mobdev.databinding.ItemMessageImageBinding
import io.github.mobdev.databinding.ItemMessageTextBinding

sealed class MessageListItem {
    abstract val id: Long

    data class TextItem(val message: ChatMessage, val text: String) : MessageListItem() {
        override val id: Long = message.id
    }

    data class ImageItem(val message: ChatMessage, val link: String) : MessageListItem() {
        override val id: Long = message.id
    }
}

class MessagesAdapter(
    private val onImageClick: (String) -> Unit,
) : ListAdapter<MessageListItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MessageListItem.TextItem -> VIEW_TYPE_TEXT
        is MessageListItem.ImageItem -> VIEW_TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TEXT -> TextViewHolder(
                ItemMessageTextBinding.inflate(inflater, parent, false),
            )
            else -> ImageViewHolder(
                ItemMessageImageBinding.inflate(inflater, parent, false),
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MessageListItem.TextItem -> (holder as TextViewHolder).bind(item)
            is MessageListItem.ImageItem -> (holder as ImageViewHolder).bind(item)
        }
    }

    inner class TextViewHolder(
        private val binding: ItemMessageTextBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListItem.TextItem) {
            binding.messageAuthor.text = item.message.from
            binding.messageText.text = if (item.message.id < 0) {
                binding.root.context.getString(R.string.message_pending) + "\n" + item.text
            } else {
                item.text
            }
        }
    }

    inner class ImageViewHolder(
        private val binding: ItemMessageImageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageListItem.ImageItem) {
            binding.messageAuthor.text = item.message.from
            binding.messageImage.load(ImageUrl.thumb(item.link)) {
                crossfade(true)
            }
            binding.messageImage.setOnClickListener { onImageClick(item.link) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MessageListItem>() {
        override fun areItemsTheSame(oldItem: MessageListItem, newItem: MessageListItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MessageListItem, newItem: MessageListItem): Boolean =
            oldItem == newItem
    }

    companion object {
        private const val VIEW_TYPE_TEXT = 0
        private const val VIEW_TYPE_IMAGE = 1
    }
}

fun List<ChatMessage>.toListItems(): List<MessageListItem> = mapNotNull { message ->
    when (val data = message.data) {
        is MessageData.Text -> MessageListItem.TextItem(message, data.text)
        is MessageData.Image -> MessageListItem.ImageItem(message, data.link)
        null -> null
    }
}
