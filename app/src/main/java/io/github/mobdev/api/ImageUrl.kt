package io.github.mobdev.api

object ImageUrl {
    fun thumb(path: String): String = "${ApiClient.BASE_URL}thumb/$path"
    fun full(path: String): String = "${ApiClient.BASE_URL}img/$path"
}
