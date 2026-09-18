package ru.ivan.mobchat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.ivan.mobchat.data.ApiFactory
import ru.ivan.mobchat.data.ChatMessage
import java.text.DateFormat
import java.util.Date

data class ChannelRow(val name: String, val selected: Boolean)

class ChannelAdapter(private val onClick: (String) -> Unit) :
    ListAdapter<ChannelRow, ChannelAdapter.Holder>(ChannelDiff) {
    fun submit(channels: List<String>, selected: String?) {
        submitList(channels.map { ChannelRow(it, it == selected) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(channel: ChannelRow) {
            itemView.findViewById<TextView>(R.id.channel_name).text = channel.name.removeSuffix("@channel")
            itemView.isSelected = channel.selected
            itemView.findViewById<View>(R.id.channel_selected).visibility =
                if (channel.selected) View.VISIBLE else View.INVISIBLE
            itemView.setOnClickListener { onClick(channel.name) }
        }
    }

    private object ChannelDiff : DiffUtil.ItemCallback<ChannelRow>() {
        override fun areItemsTheSame(old: ChannelRow, new: ChannelRow) = old.name == new.name
        override fun areContentsTheSame(old: ChannelRow, new: ChannelRow) = old == new
    }
}

class MessageAdapter(private val onImageClick: (String) -> Unit) :
    ListAdapter<ChatMessage, MessageAdapter.Holder>(MessageDiff) {
    var username: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    override fun onViewRecycled(holder: Holder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val row = itemView.findViewById<LinearLayout>(R.id.message_row)
        private val card = itemView.findViewById<LinearLayout>(R.id.message_card)
        private val author = itemView.findViewById<TextView>(R.id.message_author)
        private val text = itemView.findViewById<TextView>(R.id.message_text)
        private val time = itemView.findViewById<TextView>(R.id.message_time)
        private val image = itemView.findViewById<ImageView>(R.id.message_image)

        init {
            row.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
                if (right - left != oldRight - oldLeft) sizeBubble()
            }
        }

        private fun sizeBubble() {
            val available = row.width - row.paddingLeft - row.paddingRight
            if (available <= 0) return
            val maxWidth = (available * 0.86f).toInt()
            text.maxWidth = (maxWidth - card.paddingLeft - card.paddingRight).coerceAtLeast(1)
            author.maxWidth = text.maxWidth
            time.maxWidth = text.maxWidth
            if (image.isVisible && card.layoutParams.width != maxWidth) {
                card.layoutParams = card.layoutParams.apply { width = maxWidth }
            }
        }

        fun bind(message: ChatMessage) {
            val own = message.from == username
            row.gravity = if (own) Gravity.END else Gravity.START
            card.setBackgroundResource(if (own) R.drawable.bg_message_own else R.drawable.bg_message_other)
            author.text = if (own) itemView.context.getString(R.string.you) else message.from
            time.text = formatTime(message.time)
            text.text = message.data.text?.text.orEmpty()
            text.isVisible = message.data.text != null
            val path = message.data.image?.link
            image.isVisible = !path.isNullOrBlank()
            card.layoutParams = card.layoutParams.apply { width = ViewGroup.LayoutParams.WRAP_CONTENT }
            image.setOnClickListener(null)
            if (path.isNullOrBlank()) {
                Glide.with(image).clear(image)
            } else {
                Glide.with(image).load(imageUrl(path, thumbnail = true))
                    .placeholder(R.drawable.ic_image).error(R.drawable.ic_image).into(image)
                image.setOnClickListener { onImageClick(path) }
            }
            sizeBubble()
        }

        fun clear() = Glide.with(image).clear(image)
    }

    private object MessageDiff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem.id != null && oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem == newItem
    }
}

internal fun formatTime(raw: String?): String {
    val value = raw?.toLongOrNull() ?: return ""
    val millis = if (value in 0..99_999_999_999L) value * 1000L else value
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))
}

fun imageUrl(path: String, thumbnail: Boolean): String {
    val endpoint = if (thumbnail) "thumb/" else "img/"
    val base = okhttp3.HttpUrl.parse(ApiFactory.BASE_URL)!!
    val absolute = okhttp3.HttpUrl.parse(path)
    if (absolute != null && absolute.host() != base.host()) return absolute.toString()
    val relative = (absolute?.encodedPath() ?: path).trimStart('/')
        .removePrefix("img/").removePrefix("thumb/")
    return base.newBuilder().addPathSegment(endpoint.trimEnd('/'))
        .addEncodedPathSegments(relative).build().toString()
}
