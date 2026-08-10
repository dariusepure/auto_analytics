/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.ui.cars.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class FirestoreChatMessage(
    @get:PropertyName("isUser") @set:PropertyName("isUser")
    var isUser: Boolean = false,
    
    @get:PropertyName("text") @set:PropertyName("text")
    var text: String = "",
    
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Timestamp = Timestamp.now()
) {
    fun toChatMessage() = ChatMessage(
        text = text,
        isUser = isUser,
        timestamp = timestamp.toDate().time
    )

    companion object {
        fun fromChatMessage(message: ChatMessage) = FirestoreChatMessage(
            text = message.text,
            isUser = message.isUser,
            timestamp = Timestamp(java.util.Date(message.timestamp))
        )
    }
}

