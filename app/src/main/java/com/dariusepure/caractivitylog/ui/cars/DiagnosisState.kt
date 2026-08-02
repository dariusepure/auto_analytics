package com.dariusepure.caractivitylog.ui.cars

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class DiagnosisUiState(
    val isLoading: Boolean = false,
    val isTyping: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val carName: String = "",
    val errorMessage: String? = null
)
