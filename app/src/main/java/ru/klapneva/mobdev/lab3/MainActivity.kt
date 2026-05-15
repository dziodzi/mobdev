package ru.klapneva.mobdev.lab3

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// модели
data class LoginRequest(val name: String, val pwd: String)

data class MessageDto(
    val id: String? = null,
    val from: String,
    val to: String? = null,
    val data: MessageDataDto,
    val time: String? = null,
)

data class MessageDataDto(
    @Json(name = "Text") val text: TextPayload? = null,
    @Json(name = "Image") val image: ImagePayload? = null,
)

data class TextPayload(val text: String)
data class ImagePayload(val link: String? = null)

data class ChatMessage(
    val id: String,
    val from: String,
    val to: String,
    val text: String?,
    val imagePath: String?,
    val time: String?,
)

//интерфейс апи
interface ChatApi {
    @FormUrlEncoded
    @POST("/addusr")
    suspend fun register(@Field("name") name: String): retrofit2.Response<ResponseBody>

    @POST("/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<ResponseBody>

    @GET("/channels")
    suspend fun getChannels(@Header("X-Auth-Token") token: String): retrofit2.Response<List<String>>

    @GET("/channel/{name}")
    suspend fun getMessages(
        @Path("name") name: String,
        @Header("X-Auth-Token") token: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String = "0"
    ): retrofit2.Response<List<MessageDto>>

    @POST("/messages")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Body message: MessageDto
    ): retrofit2.Response<ResponseBody>

    @POST("/logout")
    suspend fun logout(@Header("X-Auth-Token") token: String): retrofit2.Response<Unit>
}

//авторизация
class AuthSession {
    var token: String? = null
}

// репозиторий для хранения
class ChatRepository(
    private val api: ChatApi,
    private val session: AuthSession,
) {
    suspend fun register(name: String): String {
        val response = api.register(name)
        if (!response.isSuccessful) throw Exception(response.code().toString())
        val body = response.body()?.string() ?: throw Exception("empty")
        return body.substringAfter("'").substringBefore("'")
    }

    suspend fun login(name: String, password: String): String {
        val response = api.login(LoginRequest(name, password))
        if (!response.isSuccessful) throw Exception("unauthorized")
        val token = response.body()?.string()?.trim() ?: throw Exception("empty")
        session.token = token
        return token
    }

    suspend fun getChannels(): List<String> {
        val token = session.token ?: throw Exception("unauthorized")
        val response = api.getChannels(token)
        if (!response.isSuccessful) throw Exception(response.code().toString())
        return response.body() ?: emptyList()
    }

    suspend fun getMessages(channel: String, lastId: String = "0"): List<ChatMessage> {
        val token = session.token ?: throw Exception("unauthorized")
        val response = api.getMessages(channel, token, limit = 20, lastKnownId = lastId)
        if (!response.isSuccessful) throw Exception(response.code().toString())
        return response.body()?.mapNotNull { dto ->
            dto.id?.let { id ->
                ChatMessage(
                    id = id,
                    from = dto.from,
                    to = dto.to ?: "",
                    text = dto.data.text?.text,
                    imagePath = dto.data.image?.link,
                    time = dto.time,
                )
            }
        }?.sortedBy { it.id.toLongOrNull() } ?: emptyList()
    }

    suspend fun sendMessage(username: String, channel: String, text: String): String {
        val token = session.token ?: throw Exception("unauthorized")
        val response = api.sendMessage(
            token,
            MessageDto(
                from = username,
                to = channel,
                data = MessageDataDto(text = TextPayload(text))
            )
        )
        if (!response.isSuccessful) throw Exception(response.code().toString())
        return response.body()?.string() ?: ""
    }

    suspend fun logout() {
        session.token?.let { api.logout(it) }
        session.token = null
    }
}

// создаем
fun createApi(session: AuthSession): ChatApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
            session.token?.let { request.header("X-Auth-Token", it) }
            chain.proceed(request.build())
        }
        .build()

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    return Retrofit.Builder()
        .baseUrl("https://faerytea.name/")
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ChatApi::class.java)
}

// время
fun formatTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        val time = timestamp.toLongOrNull() ?: return ""
        val date = Date(time * 1000)
        val format = SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault())
        format.format(date)
    } catch (e: Exception) {
        ""
    }
}

// все про viewmosel
class ChatViewModel : ViewModel() {
    private val session = AuthSession()
    private val api = createApi(session)
    private val repository = ChatRepository(api, session)

    var isLoggedIn by mutableStateOf(false)
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var messageText by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorCode by mutableStateOf<Int?>(null)

    var chats by mutableStateOf<List<String>>(emptyList())
    var currentChat by mutableStateOf<String?>(null)
    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
    var currentImage by mutableStateOf<String?>(null)

    var hasMoreMessages by mutableStateOf(true)
    private var oldestMessageId by mutableStateOf("0")

    fun onUsernameChange(newValue: String) { username = newValue }
    fun onPasswordChange(newValue: String) { password = newValue }
    fun onMessageChange(newValue: String) { messageText = newValue }
    fun clearError() { errorCode = null }

    fun openImage(imagePath: String) { currentImage = imagePath }
    fun closeImage() { currentImage = null }

    fun register() {
        if (username.isBlank()) {
            errorCode = R.string.error_empty_login
            return
        }
        isLoading = true
        viewModelScope.launch {
            try {
                val pwd = repository.register(username)
                password = pwd
                errorCode = R.string.registered_success
            } catch (e: Exception) {
                errorCode = when (e.message) {
                    "409" -> R.string.user_exists
                    else -> R.string.error_registration_failed
                }
            }
            isLoading = false
        }
    }

    fun login() {
        when {
            username.isBlank() && password.isBlank() -> {
                errorCode = R.string.error_empty_login_password
                return
            }
            username.isBlank() -> {
                errorCode = R.string.error_empty_login
                return
            }
            password.isBlank() -> {
                errorCode = R.string.error_empty_password
                return
            }
        }
        isLoading = true
        viewModelScope.launch {
            try {
                repository.login(username, password)
                isLoggedIn = true
                loadChats()
            } catch (e: Exception) {
                errorCode = R.string.error_invalid_credentials
            }
            isLoading = false
        }
    }

    fun loadChats() {
        viewModelScope.launch {
            try {
                chats = repository.getChannels()
            } catch (e: Exception) {
                errorCode = R.string.error_loading_chats
            }
        }
    }

    fun openChat(chatName: String) {
        currentChat = chatName
        loadMessages()
    }

    fun closeChat() {
        currentChat = null
        messages = emptyList()
        hasMoreMessages = true
        oldestMessageId = "0"
    }

    fun loadMessages() {
        val chat = currentChat ?: return
        isLoading = true
        viewModelScope.launch {
            try {
                val newMessages = repository.getMessages(chat, lastId = "0")
                messages = newMessages
                oldestMessageId = messages.firstOrNull()?.id ?: "0"
                hasMoreMessages = messages.size >= 20
            } catch (e: Exception) {
                if (e.message == "unauthorized") {
                    logout()
                } else {
                    errorCode = R.string.error_loading_messages
                }
            }
            isLoading = false
        }
    }

    fun loadMoreMessages() {
        val chat = currentChat ?: return
        if (isLoading || !hasMoreMessages) return
        isLoading = true
        viewModelScope.launch {
            try {
                val olderMessages = repository.getMessages(chat, lastId = oldestMessageId)
                if (olderMessages.isNotEmpty()) {
                    messages = (olderMessages + messages).distinctBy { it.id }
                    oldestMessageId = messages.firstOrNull()?.id ?: "0"
                    hasMoreMessages = olderMessages.size >= 20
                } else {
                    hasMoreMessages = false
                }
            } catch (e: Exception) {
                errorCode = R.string.error_loading_messages
            }
            isLoading = false
        }
    }

    fun sendMessage() {
        val text = messageText
        val chat = currentChat ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                repository.sendMessage(username, chat, text)
                messageText = ""
                loadMessages()
            } catch (e: Exception) {
                errorCode = R.string.error_send_failed
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            isLoggedIn = false
            username = ""
            password = ""
            chats = emptyList()
            currentChat = null
            messages = emptyList()
            currentImage = null
            hasMoreMessages = true
            oldestMessageId = "0"
        }
    }
}

// все про ui
@Composable
fun ChatApp(viewModel: ChatViewModel) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BackHandler(enabled = true) {
        when {
            viewModel.currentImage != null -> viewModel.closeImage()
            viewModel.currentChat != null && !isLandscape -> viewModel.closeChat()
            viewModel.isLoggedIn && viewModel.currentChat == null -> Unit
            else -> viewModel.logout()
        }
    }

    if (viewModel.currentImage != null) {
        ImageScreen(
            imagePath = viewModel.currentImage!!,
            onClose = { viewModel.closeImage() }
        )
        return
    }

    when {
        !viewModel.isLoggedIn -> LoginScreen(viewModel)
        isLandscape -> LandscapeScreen(viewModel)
        else -> PortraitScreen(viewModel)
    }

    if (viewModel.errorCode != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(stringResource(viewModel.errorCode!!)) },
            confirmButton = { Button(onClick = { viewModel.clearError() }) { Text(stringResource(R.string.ok)) } }
        )
    }
}

@Composable
fun PortraitScreen(viewModel: ChatViewModel) {
    when {
        viewModel.currentChat == null -> ChatsScreen(viewModel, selectedChat = null)
        else -> MessagesScreen(viewModel)
    }
}

@Composable
fun LandscapeScreen(viewModel: ChatViewModel) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            ChatsScreen(viewModel, selectedChat = viewModel.currentChat)
        }
        Box(modifier = Modifier.weight(1f)) {
            if (viewModel.currentChat != null) {
                MessagesScreen(viewModel)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.select_chat_hint))
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: ChatViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.username,
            onValueChange = { viewModel.onUsernameChange(it) },
            label = { Text(stringResource(R.string.login_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading,
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = { Text(stringResource(R.string.password_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.login() },
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.isLoading) Text(stringResource(R.string.loading))
            else Text(stringResource(R.string.login_button))
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.register() },
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.register_button))
        }
    }
}

@Composable
fun ChatsScreen(viewModel: ChatViewModel, selectedChat: String?) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.chats_title), style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { viewModel.logout() }) { Text(stringResource(R.string.logout_button)) }
        }

        LazyColumn {
            items(viewModel.chats) { chat ->
                val isSelected = chat == selectedChat
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openChat(chat) },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (isSelected) 4.dp else 1.dp
                ) {
                    Text(
                        text = chat,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Divider()
            }
        }
    }
}

@Composable
fun MessagesScreen(viewModel: ChatViewModel) {
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { viewModel.closeChat() }) { Text(stringResource(R.string.back_button)) }
            Text(viewModel.currentChat ?: "", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(48.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            reverseLayout = true
        ) {
            if (viewModel.hasMoreMessages && viewModel.messages.isNotEmpty()) {
                item {
                    Button(
                        onClick = { viewModel.loadMoreMessages() },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) Text(stringResource(R.string.loading))
                        else Text(stringResource(R.string.load_more))
                    }
                }
            }

            items(viewModel.messages.reversed()) { msg ->
                MessageItem(
                    message = msg,
                    onImageClick = { viewModel.openImage(it) }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            OutlinedTextField(
                value = viewModel.messageText,
                onValueChange = { viewModel.onMessageChange(it) },
                placeholder = { Text(stringResource(R.string.message_hint)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = { viewModel.sendMessage() },
                enabled = viewModel.messageText.isNotBlank() && !viewModel.isLoading
            ) {
                Text(stringResource(R.string.send_button))
            }
        }
    }

    LaunchedEffect(viewModel.messages) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
}

@Composable
fun MessageItem(
    message: ChatMessage,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = message.from,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            when {
                message.imagePath != null -> {
                    AsyncImage(
                        model = "https://faerytea.name/thumb/${message.imagePath}",
                        contentDescription = stringResource(R.string.image_description),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clickable { onImageClick(message.imagePath!!) },
                        contentScale = ContentScale.Crop
                    )
                }
                message.text != null -> {
                    Text(message.text)
                }
            }

            if (message.time != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(message.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ImageScreen(
    imagePath: String,
    onClose: () -> Unit
) {
    BackHandler(enabled = true) { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onClose() }
    ) {
        AsyncImage(
            model = "https://faerytea.name/img/${imagePath}",
            contentDescription = stringResource(R.string.image_description),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = Color.White
            )
        }
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ChatApp(viewModel = ChatViewModel())
            }
        }
    }
}