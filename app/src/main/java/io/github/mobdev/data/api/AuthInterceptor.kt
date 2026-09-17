package io.github.mobdev.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val onUnauthorized: () -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()

        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header(HEADER_AUTH_TOKEN, token)
                .build()
        }

        val response = chain.proceed(request)

        if (response.code == HTTP_UNAUTHORIZED && LOGIN_PATH !in request.url.encodedPathSegments) {
            onUnauthorized()
        }

        return response
    }

    private companion object {
        const val HEADER_AUTH_TOKEN = "X-Auth-Token"
        const val HTTP_UNAUTHORIZED = 401
        const val LOGIN_PATH = "login"
    }
}