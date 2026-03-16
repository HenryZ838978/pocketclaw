package com.pocketclaw.app.messaging

/**
 * Unified abstraction for multi-platform messaging.
 * Each platform (Telegram, Discord, Feishu, Slack) implements this interface.
 */
interface MessageChannel {
    val platformId: String
    val displayName: String
    val isConnected: Boolean

    suspend fun connect(config: Map<String, String>): Boolean
    suspend fun disconnect()
    suspend fun sendMessage(text: String): Boolean
    fun setIncomingHandler(handler: suspend (IncomingMessage) -> String?)
}

data class IncomingMessage(
    val platform: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Central bridge that routes messages between platforms and the PocketClaw brain.
 */
object MessageBridge {

    private val channels = mutableMapOf<String, MessageChannel>()

    fun register(channel: MessageChannel) {
        channels[channel.platformId] = channel
    }

    fun get(platformId: String): MessageChannel? = channels[platformId]

    fun allChannels(): List<MessageChannel> = channels.values.toList()

    fun connectedChannels(): List<MessageChannel> = channels.values.filter { it.isConnected }
}
