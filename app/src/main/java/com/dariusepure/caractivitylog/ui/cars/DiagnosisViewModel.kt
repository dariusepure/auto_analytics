/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.ai.GeminiRepository
import com.dariusepure.caractivitylog.data.ai.text
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.displayName
import com.dariusepure.caractivitylog.util.DiagnosticUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carRepository: CarRepository,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosisUiState())
    val state = _state.asStateFlow()

    private var carContext = ""
    private var currentCarId: String? = null
    private var currentCar: Car? = null

    fun loadCarData(carId: String) {
        currentCarId = carId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val car = carRepository.getCar(carId)
            currentCar = car
            if (car != null) {
                val mileageLogs = carRepository.getMileageLogs(carId).first()
                val latestKm = mileageLogs.maxByOrNull { it.date }?.km ?: 0.0
                
                carContext = """
                    Car: ${car.displayName}, Year: ${car.year}, Engine: ${car.engineSize} ${car.fuelType}
                    Specs: Power: ${car.power}${car.powerUnit}, Torque: ${car.torque}Nm, Color: ${car.color}, Gears: ${car.gears}
                    System: ${car.fuelSystem}
                    Current Mileage: $latestKm km
                """.trimIndent()

                _state.update { it.copy(
                    isLoading = false,
                    carName = car.displayName
                ) }
            }
            
            // Collect messages from Firestore
            carRepository.getDiagnosisMessages(carId).collect { messages ->
                _state.update { it.copy(
                    messages = if (messages.isEmpty()) {
                        listOf(ChatMessage("Hello! I am your AI assistant. How can I help you with your ${car?.make ?: "car"} today?", false))
                    } else {
                        messages
                    }
                ) }
            }
        }
    }

    fun sendMessage(text: String) {
        val carId = currentCarId ?: return
        if (text.isBlank()) return
        
        val userMessage = ChatMessage(text, true)
        
        viewModelScope.launch {
            // Clear previous error
            _state.update { it.copy(errorMessage = null) }
            
            // Save user message to Firestore
            try {
                carRepository.addDiagnosisMessage(carId, userMessage)
            } catch (e: Exception) {
                // If writing to Firestore fails (e.g. App Check), show error locally
                _state.update { it.copy(
                    errorMessage = "Firestore Error: ${e.localizedMessage ?: "Check your connection or App Check status"}"
                ) }
                return@launch
            }
            
            _state.update { it.copy(isTyping = true) }

            try {
                val currentMsgs = _state.value.messages
                val currentLanguage = java.util.Locale.getDefault().displayLanguage
                val response = geminiRepository.getDiagnosisResponse(text, carContext, currentMsgs, currentLanguage)
                
                val aiResponseText = response.text
                if (!aiResponseText.isNullOrBlank()) {
                    val cleanedResponse = cleanAiResponse(aiResponseText)
                    val aiMessage = ChatMessage(cleanedResponse, false)
                    carRepository.addDiagnosisMessage(carId, aiMessage)
                }
                
            } catch (t: Throwable) {
                val sha1 = DiagnosticUtils.getAppSignatureSha1(context, withColons = true)
                val error = when {
                    t.message?.contains("403") == true || t.message?.contains("PERMISSION_DENIED") == true -> {
                        "AI Access Denied: Please add this SHA-1 to Google Cloud Console for package ${context.packageName}: \n\n$sha1"
                    }
                    t.message?.contains("404") == true -> {
                        "AI Model Not Found: Check if 'gemini-1.5-flash' is the correct model name in your config."
                    }
                    else -> "AI Error: ${t.localizedMessage ?: "Unknown error"}"
                }
                
                _state.update { it.copy(
                    errorMessage = error
                ) }
            } finally {
                _state.update { it.copy(isTyping = false) }
            }
        }
    }

    fun resetConversation() {
        val carId = currentCarId ?: return
        viewModelScope.launch {
            carRepository.clearDiagnosisMessages(carId)
        }
    }

    private fun cleanAiResponse(text: String): String {
        return text.replace("*", "").replace("#", "")
    }
}

