package com.jackob.dvz.core.statistics

import kotlinx.serialization.KSerializer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.json.Json

object APIClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(5))
        .build()

    fun <T> get(url: String, serializer: KSerializer<T>): Result<T?> = runCatching {
        val request = HttpRequest.newBuilder()
            .uri(URI(url))
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        when (val status = response.statusCode()) {
            in 200..299 -> json.decodeFromString(serializer, response.body())
            404 -> null
            else -> throw Exception("Unexpected get error, $url returned: $status")
        }
    }

    fun <T> put(url: String, serializer: KSerializer<T>, body: T): Result<Unit> = runCatching {
        val jsonBody = json.encodeToString(serializer, body)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw Exception("Unexpected post error, $url returned: ${response.statusCode()}")
        }
    }

}