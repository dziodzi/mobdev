package ru.tsvetkova.chat.data

import java.util.concurrent.atomic.AtomicReference

class AuthHolder {
    private val ref = AtomicReference<String?>(null)

    var token: String?
        get() = ref.get()
        set(value) { ref.set(value) }
}
