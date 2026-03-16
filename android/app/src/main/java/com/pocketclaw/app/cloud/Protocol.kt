package com.pocketclaw.app.cloud

import com.google.gson.annotations.SerializedName

// ──── Phone → Cloud ────

data class DeviceEvent(
    val type: EventType,
    val timestamp: String,
    val source: EventSource,
    val device: DeviceContext,
)

enum class EventType {
    @SerializedName("notification") NOTIFICATION,
    @SerializedName("user_command") USER_COMMAND,
    @SerializedName("schedule") SCHEDULE,
    @SerializedName("location") LOCATION,
}

data class EventSource(
    val app: String,
    val title: String?,
    val text: String,
    val sender: String?,
)

data class DeviceContext(
    val battery: Int,
    val network: NetworkType,
)

enum class NetworkType {
    @SerializedName("wifi") WIFI,
    @SerializedName("cellular") CELLULAR,
    @SerializedName("offline") OFFLINE,
}

// ──── Cloud → Phone ────

data class CloudResponse(
    val actions: List<Action>,
    val meta: ResponseMeta,
    val memories: List<ExtractedMemory>? = null,
)

data class ExtractedMemory(
    val type: String,
    val key: String,
    val value: String,
    val confidence: Float = 0.5f,
)

data class Action(
    val type: String,
    val priority: String? = null,
    val title: String? = null,
    val body: String? = null,
    val suggestions: List<String>? = null,
    val at: String? = null,
    @SerializedName("target_app") val targetApp: String? = null,
    val text: String? = null,
)

data class ResponseMeta(
    @SerializedName("skill_used") val skillUsed: String,
    @SerializedName("tokens_consumed") val tokensConsumed: Int,
    @SerializedName("tokens_saved_vs_fullcontext") val tokensSaved: Int,
    @SerializedName("latency_ms") val latencyMs: Int,
)
