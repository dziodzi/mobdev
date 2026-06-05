package io.github.mobdev.api

import io.github.mobdev.data.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager,
    private val onUnauthorized: () -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        sessionManager.token?.let { token ->
            requestBuilder.header(HEADER_AUTH, token)
        }
        val response = chain.proceed(requestBuilder.build())
        if (response.code == 401 && !chain.request().url.encodedPath.endsWith("/login")) {
            sessionManager.clearToken()
            onUnauthorized()
        }
        return response
    }

    companion object {
        const val HEADER_AUTH = "X-Auth-Token"
    }
}
