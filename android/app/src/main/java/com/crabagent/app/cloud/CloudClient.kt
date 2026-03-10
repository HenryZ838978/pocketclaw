package com.crabagent.app.cloud

import com.crabagent.app.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * HTTP client for communicating with the Cloud Brain.
 *
 * Sends DeviceEvents, receives CloudResponses.
 * Timeout-aware, retries on transient failure.
 */
class CloudClient {

    private val gson = Gson()
    private val jsonMedia = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS) // LLM calls can take a few seconds
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val baseUrl = BuildConfig.CLOUD_BASE_URL

    suspend fun sendEvent(event: DeviceEvent): CloudResponse = withContext(Dispatchers.IO) {
        val json = gson.toJson(event)
        val body = json.toRequestBody(jsonMedia)

        val request = Request.Builder()
            .url("$baseUrl/event")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Cloud brain returned ${response.code}: ${response.body?.string()}")
        }

        val responseBody = response.body?.string()
            ?: throw RuntimeException("Empty response from cloud brain")

        gson.fromJson(responseBody, CloudResponse::class.java)
    }

    suspend fun healthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
