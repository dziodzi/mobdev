package ru.ivan.mobchat

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import ru.ivan.mobchat.data.ApiException
import ru.ivan.mobchat.data.ChatApi
import ru.ivan.mobchat.data.ChatMessage
import ru.ivan.mobchat.data.ChatRepository
import ru.ivan.mobchat.data.LoginRequest
import ru.ivan.mobchat.data.MessageData
import ru.ivan.mobchat.data.TextPayload

class ChatRegressionTest {
    private fun message(id: String, text: String = id) =
        ChatMessage(id = id, from = "sender", data = MessageData(text = TextPayload(text)))

    @Test fun historyAndRefreshAreSortedNumericallyWithoutDuplicates() {
        val original = listOf(message("10"), message("11"))
        val merged = mergeMessages(original, listOf(message("9"), message("10", "updated")))
        assertEquals(listOf("9", "10", "11"), merged.map { it.id })
        assertEquals("updated", merged[1].data.text?.text)
        assertEquals("10", original[0].data.text?.text)
    }

    @Test fun sentMessageSurvivesAnOlderPageArrivingLater() {
        val confirmed = listOf(message("999"))
        val merged = mergeMessages(confirmed, listOf(message("21"), message("20")))
        assertEquals("999", merged.last().id)
    }

    @Test fun latestHistoryAndRefreshUseDifferentCursorsWithTwentyPerPage() = runBlocking {
        val api = FakeApi()
        val repository = ChatRepository(api)
        repository.messages("room@channel")
        assertEquals(20, api.limit)
        assertEquals(Long.MAX_VALUE.toString(), api.cursor)
        assertTrue(api.reverse)
        repository.olderMessages("room@channel", "41")
        assertEquals("41", api.cursor)
        assertTrue(api.reverse)
        repository.newerMessages("room@channel", "60")
        assertEquals(20, api.limit)
        assertEquals("60", api.cursor)
        assertFalse(api.reverse)
    }

    @Test fun sendingReturnsConfirmedServerId() = runBlocking {
        assertEquals("123", ChatRepository(FakeApi()).send(message("ignored")))
    }

    @Test fun unauthorizedIsNotTreatedAsAnEmptyChat() = runBlocking {
        val api = FakeApi().apply { unauthorized = true }
        try {
            ChatRepository(api).messages("room@channel")
            throw AssertionError("Expected authentication failure")
        } catch (error: ApiException) {
            assertEquals(401, error.statusCode)
        }
    }

    @Test fun imageEndpointsDoNotDuplicatePrefixes() {
        assertEquals("https://faerytea.name/thumb/folder/a.jpg", imageUrl("/img/folder/a.jpg", true))
        assertEquals("https://faerytea.name/img/folder/a.jpg", imageUrl("thumb/folder/a.jpg", false))
        assertEquals("https://faerytea.name/thumb/a.jpg", imageUrl("https://faerytea.name/img/a.jpg", true))
    }

    @Test fun secondsAndMillisecondsProduceTheSameDisplayTime() {
        assertEquals(formatTime("1700000000"), formatTime("1700000000000"))
    }

    private class FakeApi : ChatApi {
        var limit = 0
        var cursor: String? = null
        var reverse = false
        var unauthorized = false

        override suspend fun login(request: LoginRequest): Response<ResponseBody> = error("Unused")
        override suspend fun channels(): Response<List<String>> = Response.success(emptyList())
        override suspend fun messages(
            channel: String, limit: Int, lastKnownId: String?, reverse: Boolean
        ): Response<List<ChatMessage>> {
            this.limit = limit
            this.cursor = lastKnownId
            this.reverse = reverse
            return if (unauthorized) Response.error(401, body("Unauthorized"))
            else Response.success(emptyList())
        }
        override suspend fun sendMessage(message: ChatMessage): Response<ResponseBody> =
            Response.success(body("123"))
        override suspend fun logout(token: String): Response<ResponseBody> = Response.success(body(""))
        private fun body(text: String) = ResponseBody.create(MediaType.parse("text/plain"), text)
    }
}
