package com.helpmethen.chat.data.session

import android.graphics.Bitmap
import com.helpmethen.chat.data.model.MessageDto

object ChatSession {
    var token: String? = null
    var username: String? = null
    var channels: List<String>? = null
    var selectedChannel: String? = null

    val lastMessagePreviews = mutableMapOf<String, String>()
    val messagesByChannel = mutableMapOf<String, List<MessageDto>>()
    val images = mutableMapOf<String, Bitmap>()

    fun setLogin(token: String, username: String) {
        this.token = token
        this.username = username
    }

    fun clear() {
        token = null
        username = null
        channels = null
        selectedChannel = null
        lastMessagePreviews.clear()
        messagesByChannel.clear()
        images.clear()
    }
}
