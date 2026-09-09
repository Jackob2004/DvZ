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

    fun <T> get(url: String, serializer: KSerializer<T>): CallResult<T> {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI(url))
                .header("Accept", "application/json")
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            return when (val status = response.statusCode()) {
                in 200..299 -> CallResult.Success(json.decodeFromString(serializer, response.body()))
                404 -> CallResult.NotFound
                else -> CallResult.Failure(Exception("Unexpected GET error, $url status code: $status"))
            }
        } catch (e: Exception) {
            return CallResult.Failure(e)
        }
    }

    fun <T> put(url: String, serializer: KSerializer<T>, body: T): CallResult<Unit> {
        try {
            val jsonBody = json.encodeToString(serializer, body)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                return CallResult.Failure(Exception("Unexpected post error, $url returned: ${response.statusCode()}"))
            }

        } catch (e: Exception) {
            return CallResult.Failure(e)
        }

        return CallResult.Success(Unit)
    }

    sealed class CallResult<out T> {
        data class Success<T>(val data: T) : CallResult<T>()
        data class Failure(val error: Throwable) : CallResult<Nothing>()
        object NotFound : CallResult<Nothing>()
    }

}