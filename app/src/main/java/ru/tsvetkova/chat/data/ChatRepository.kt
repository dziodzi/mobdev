package ru.tsvetkova.chat.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class UnauthorizedException : Exception()

class ChatRepository(
    private val api: ChatApi,
    private val auth: AuthHolder,
    private val dao: MessageDao,
    private val pendingDao: PendingDao,
) {

    suspend fun login(name: String, pwd: String): String = guard {
        val raw = api.login(LoginRequest(name, pwd)).string()
        val token = raw.trim().trim('"')
        auth.token = token
        token
    }

    suspend fun logout() {
        runCatching { api.logout() }
        auth.token = null
    }

    suspend fun channels(): List<String> = guard { api.channels() }

    fun observeChannel(channel: String): Flow<List<Message>> =
        dao.observeChannel(channel).map { rows -> rows.map { it.toMessage() } }

    suspend fun refreshLatest(channel: String): Int = guard {
        val fresh = api.channelMessages(
            name = channel,
            limit = 20,
            lastKnownId = 9_999_999_999L,
            reverse = true,
        )
        dao.upsertAll(fresh.mapNotNull { it.toEntity(channel) })
        fresh.size
    }

    suspend fun loadOlder(channel: String): Int = guard {
        val oldest = dao.oldestId(channel) ?: return@guard 0
        val older = api.channelMessages(
            name = channel,
            limit = 20,
            lastKnownId = oldest,
            reverse = true,
        )
        dao.upsertAll(older.mapNotNull { it.toEntity(channel) })
        older.size
    }

    suspend fun send(from: String, to: String, text: String): Unit = guard {
        api.sendMessage(
            Message(
                from = from,
                to = to,
                data = MessageData(text = TextPayload(text)),
            )
        )
    }

    fun observePending(channel: String): Flow<List<PendingMessageEntity>> =
        pendingDao.observeChannel(channel)

    fun observeAllPending(): Flow<List<PendingMessageEntity>> =
        pendingDao.observeAll()

    suspend fun queueText(channel: String, from: String, text: String) {
        pendingDao.insert(
            PendingMessageEntity(
                channel = channel,
                from = from,
                text = text,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun flushPending(): Unit = guard {
        val items = pendingDao.all()
        for (item in items) {
            api.sendMessage(
                Message(
                    from = item.from,
                    to = item.channel,
                    data = MessageData(text = TextPayload(item.text)),
                )
            )
            pendingDao.delete(item.localId)
        }
    }

    suspend fun sendImage(
        from: String,
        to: String,
        caption: String,
        bytes: ByteArray,
        mime: String,
        fileName: String,
    ): Unit = guard {
        val message = Message(
            from = from,
            to = to,
            data = if (caption.isBlank()) null else MessageData(text = TextPayload(caption)),
        )
        val msgJson = Json { explicitNulls = false }.encodeToString(message)
        val msgPart = msgJson.toRequestBody("application/json".toMediaTypeOrNull())
        val fileBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val picturePart = MultipartBody.Part.createFormData("picture", fileName, fileBody)
        api.sendImage(msgPart, picturePart)
    }

    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: HttpException) {
        if (e.code() == 401) {
            auth.token = null
            throw UnauthorizedException()
        } else {
            throw e
        }
    }
}
