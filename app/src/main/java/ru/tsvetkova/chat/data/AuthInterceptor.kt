package ru.tsvetkova.chat.data

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val holder: AuthHolder) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = holder.token
        val withAuth = if (token != null) {
            request.newBuilder().header("X-Auth-Token", token).build()
        } else {
            request
        }
        return chain.proceed(withAuth)
    }
}
