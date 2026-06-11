package io.github.mobdev.data

import io.github.mobdev.data.db.AppDatabase
import io.github.mobdev.data.db.ChannelEntity
import io.github.mobdev.data.db.MessageEntity
import io.github.mobdev.data.db.PendingMessageEntity
import retrofit2.HttpException
import java.io.IOException

class Unauthorized401Exception : Exception("Unauthorized")

class ChatRepository(private val db: AppDatabase) {

    private val api = RetrofitClient.api
    private val messageDao = db.messageDao()
    private val channelDao = db.channelDao()
    private val pendingDao = db.pendingMessageDao()

    suspend fun login(username: String, password: String): String {
        val body = api.login(LoginRequest(username, password))
        return body.string().trim()
    }

    /**
     * Loads the channel list from the server. If that fails because the
     * device is offline (or the server can't be reached) and we have a
     * cached copy, the cache is returned instead so the UI still has
     * something to show.
     */
    suspend fun getChannels(token: String): List<String> = wrapAuth {
        try {
            val channels = api.getChannels(token)
            channelDao.clearAll()
            channelDao.insertAll(channels.mapIndexed { index, name -> ChannelEntity(name, index) })
            channels
        } catch (e: IOException) {
            channelDao.getChannels().ifEmpty { throw e }
        }
    }

    /**
     * Loads messages for a channel.
     *
     * - On success, the result is cached so it can be shown offline later.
     * - On the *initial* load (lastKnownId == MAX_VALUE) if the request fails
     *   because of a network error, cached messages are returned instead.
     * - For pagination ("load more") a network error is rethrown - there is
     *   nothing older cached to fall back to.
     */
    suspend fun getMessages(
        token: String,
        channel: String,
        lastKnownId: Long = Long.MAX_VALUE,
        limit: Int = 20,
    ): List<Message> = wrapAuth {
        try {
            val result = api.getMessages(token, channel, limit = limit, lastKnownId = lastKnownId, reverse = true)
            messageDao.insertAll(result.map { it.toEntity(channel) })
            result
        } catch (e: IOException) {
            if (lastKnownId == Long.MAX_VALUE) {
                messageDao.getMessagesForChannel(channel).map { it.toMessage() }
            } else {
                throw e
            }
        }
    }

    /**
     * Sends a message right now. Throws if the device is offline or the
     * request otherwise fails - the caller decides whether to queue it.
     */
    suspend fun sendMessage(token: String, username: String, channel: String, text: String) = wrapAuth {
        val message = SendMessageRequest(
            from = username,
            to = channel,
            data = MessageData(text = TextContent(text)),
        )
        api.sendMessage(token, message).string()
    }

    /** Saves a message to disk so it can be sent once the network is back. */
    suspend fun queueMessage(channel: String, username: String, text: String) {
        pendingDao.insert(
            PendingMessageEntity(
                channel = channel,
                from = username,
                text = text,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    /** Pending (not-yet-sent) messages for a channel, as displayable [Message]s. */
    suspend fun getPendingMessages(channel: String): List<Message> =
        pendingDao.getForChannel(channel).map { it.toMessage() }

    /**
     * Sends every queued message, in the order it was created. Stops at the
     * first failure (e.g. the network dropped again mid-flush) so the rest
     * stay queued for next time.
     */
    suspend fun flushPendingMessages(token: String) = wrapAuth {
        for (pending in pendingDao.getAll()) {
            val message = SendMessageRequest(
                from = pending.from,
                to = pending.channel,
                data = MessageData(text = TextContent(pending.text)),
            )
            api.sendMessage(token, message).string()
            pendingDao.delete(pending)
        }
    }

    suspend fun logout(token: String) {
        runCatching { api.logout(token) }
    }

    private suspend fun <T> wrapAuth(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            if (e.code() == 401) throw Unauthorized401Exception()
            throw e
        }
    }
}

private fun Message.toEntity(channel: String) = MessageEntity(
    id = id,
    channel = channel,
    from = from,
    to = to,
    text = data.text?.text,
    imageLink = data.image?.link,
    time = time,
)

private fun MessageEntity.toMessage() = Message(
    id = id,
    from = from,
    to = to,
    data = MessageData(
        text = text?.let { TextContent(it) },
        image = imageLink?.let { ImageContent(it) },
    ),
    time = time,
)

private fun PendingMessageEntity.toMessage() = Message(
    id = "pending-$localId",
    from = from,
    to = channel,
    data = MessageData(text = TextContent(text)),
    time = null,
    pending = true,
)
