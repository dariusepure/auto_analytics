/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiRequest(
    @SerialName("contents") val contents: List<Content>,
    @SerialName("systemInstruction") val systemInstruction: Content? = null,
    @SerialName("tools") val tools: List<Tool>? = null,
    @SerialName("generationConfig") val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    @SerialName("role") val role: String? = null,
    @SerialName("parts") val parts: List<Part>
)

@Serializable
data class Part(
    @SerialName("text") val text: String? = null,
    @SerialName("inlineData") val inlineData: Blob? = null,
    @SerialName("functionCall") val functionCall: FunctionCall? = null,
    @SerialName("functionResponse") val functionResponse: FunctionResponse? = null
)

@Serializable
data class Blob(
    @SerialName("mimeType") val mimeType: String,
    @SerialName("data") val data: String // Base64
)

@Serializable
data class Tool(
    @SerialName("functionDeclarations") val functionDeclarations: List<FunctionDeclaration>
)

@Serializable
data class FunctionDeclaration(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("parameters") val parameters: JsonObject? = null
)

@Serializable
data class FunctionCall(
    @SerialName("name") val name: String,
    @SerialName("args") val args: JsonObject? = null
)

@Serializable
data class FunctionResponse(
    @SerialName("name") val name: String,
    @SerialName("response") val response: JsonObject
)

@Serializable
data class GenerationConfig(
    @SerialName("temperature") val temperature: Float? = null,
    @SerialName("topP") val topP: Float? = null,
    @SerialName("topK") val topK: Int? = null,
    @SerialName("candidateCount") val candidateCount: Int? = null,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null,
    @SerialName("stopSequences") val stopSequences: List<String>? = null
)

@Serializable
data class GeminiResponse(
    @SerialName("candidates") val candidates: List<Candidate>? = null,
    @SerialName("promptFeedback") val promptFeedback: PromptFeedback? = null
)

@Serializable
data class Candidate(
    @SerialName("content") val content: Content? = null,
    @SerialName("finishReason") val finishReason: String? = null,
    @SerialName("index") val index: Int? = null,
    @SerialName("safetyRatings") val safetyRatings: List<SafetyRating>? = null
)

@Serializable
data class SafetyRating(
    @SerialName("category") val category: String,
    @SerialName("probability") val probability: String
)

@Serializable
data class PromptFeedback(
    @SerialName("safetyRatings") val safetyRatings: List<SafetyRating>? = null
)

val GeminiResponse.text: String?
    get() = candidates?.firstOrNull()?.content?.parts?.find { it.text != null }?.text

