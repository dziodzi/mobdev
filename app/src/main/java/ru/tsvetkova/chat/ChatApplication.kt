package ru.tsvetkova.chat

import android.app.Application
import ru.tsvetkova.chat.data.AuthHolder
import ru.tsvetkova.chat.data.ChatApi
import ru.tsvetkova.chat.data.ChatRepository
import ru.tsvetkova.chat.data.CredentialsStore
import ru.tsvetkova.chat.data.Network

class ChatApplication : Application() {

    lateinit var auth: AuthHolder
        private set
    lateinit var api: ChatApi
        private set
    lateinit var repository: ChatRepository
        private set
    lateinit var credentials: CredentialsStore
        private set

    override fun onCreate() {
        super.onCreate()
        auth = AuthHolder()
        api = Network.buildApi(auth)
        repository = ChatRepository(api, auth)
        credentials = CredentialsStore(applicationContext)
    }
}
