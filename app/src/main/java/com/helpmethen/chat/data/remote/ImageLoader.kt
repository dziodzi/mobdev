package com.helpmethen.chat.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.helpmethen.chat.data.repository.UnauthorizedException
import okhttp3.Request

object ImageLoader {

    fun loadBitmap(url: String): Result<Bitmap?> {
        return runCatching {
            val request = Request.Builder().url(url).build()

            NetworkModule.okHttpClient.newCall(request).execute().use { response ->
                if (response.code == 401) {
                    throw UnauthorizedException()
                }

                if (!response.isSuccessful) {
                    return@runCatching null
                }

                response.body?.byteStream()?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        }
    }
}
