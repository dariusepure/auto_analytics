package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.ui.cars.ChatMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteChatMessage(
    @SerialName("car_id") val carId: String,
    @SerialName("is_user") val isUser: Boolean,
    @SerialName("text") val text: String,
    @SerialName("timestamp") val timestamp: Long
) {
    fun toChatMessage() = ChatMessage(
        text = text,
        isUser = isUser,
        timestamp = timestamp
    )

    companion object {
        fun fromChatMessage(message: ChatMessage) = RemoteChatMessage(
            carId = "",
            isUser = message.isUser,
            text = message.text,
            timestamp = message.timestamp
        )
    }
}
