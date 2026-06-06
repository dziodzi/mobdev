package io.github.mobdev.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

data class LoginRequest(
    val name: String,
    val pwd: String,
)

data class ChatMessage(
    val id: Long,
    val from: String,
    val to: String?,
    @JsonAdapter(MessageDataAdapter::class)
    val data: MessageData?,
    val time: Long?,
)

sealed interface MessageData {
    data class Text(val text: String) : MessageData
    data class Image(val link: String) : MessageData
}

class MessageDataAdapter : JsonDeserializer<MessageData?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): MessageData? {
        if (json == null || !json.isJsonObject) return null
        val obj = json.asJsonObject
        return when {
            obj.has("Text") -> {
                val textObj = obj.getAsJsonObject("Text")
                MessageData.Text(textObj.get("text").asString)
            }
            obj.has("Image") -> {
                val imageObj = obj.getAsJsonObject("Image")
                MessageData.Image(imageObj.get("link").asString)
            }
            else -> null
        }
    }
}

data class SendTextMessageRequest(
    val from: String,
    val to: String,
    val data: JsonObject,
) {
    companion object {
        fun create(from: String, to: String, text: String): SendTextMessageRequest {
            val data = JsonObject().apply {
                add("Text", JsonObject().apply { addProperty("text", text) })
            }
            return SendTextMessageRequest(from, to, data)
        }
    }
}
