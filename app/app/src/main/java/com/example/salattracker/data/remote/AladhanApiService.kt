package com.example.salattracker.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AladhanApiService {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getTimingsByDate(
        date: String,
        latitude: Double,
        longitude: Double,
        method: Int = 1,
        school: Int = 0
    ): AladhanResponse {
        return client.get("https://api.aladhan.com/v1/timings/$date") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("method", method)
            parameter("school", school)
        }.body()
    }

    suspend fun getTimingsForMonth(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        method: Int = 1,
        school: Int = 0
    ): AladhanMonthResponse {
        return client.get("https://api.aladhan.com/v1/calendar/$year/$month") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("method", method)
            parameter("school", school)
        }.body()
    }

    fun close() {
        client.close()
    }
}
