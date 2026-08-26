package com.dariusepure.caractivitylog.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.dariusepure.caractivitylog.BuildConfig
import com.dariusepure.caractivitylog.util.DiagnosticUtils
import com.dariusepure.caractivitylog.domain.ScannedCarData
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: FirebaseRemoteConfig,
    private val httpClient: HttpClient
) {

    init {
        remoteConfig.fetchAndActivate()
    }

    private val modelName: String
        get() = remoteConfig.getString("gemini_model_name").ifBlank { "gemini-3.5-flash-lite" }

    private val temperature: Float
        get() = remoteConfig.getDouble("gemini_temperature").toFloat()

    private val systemPrompt: String
        get() = remoteConfig.getString("gemini_prompt")

    private val geminiApiKey: String
        get() = remoteConfig.getString("gemini_api_key")

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    private val tag = "GeminiRepository"

    private suspend fun postGemini(request: GeminiRequest): GeminiResponse {
        val model = modelName
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=$geminiApiKey"
        val sha1 = DiagnosticUtils.getAppSignatureSha1(context, withColons = true)
        val packageName = context.packageName

        Log.d(tag, "Requesting Gemini model: $model")
        Log.d(tag, "Using Package: $packageName")
        Log.d(tag, "Using real SHA1 (standard): $sha1")

        try {
            val response: io.ktor.client.statement.HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                header("X-Android-Package", packageName)
                header("X-Android-Cert", sha1)
                setBody(request)
            }
            
            if (response.status.value !in 200..299) {
                val errorBody = response.body<String>()
                Log.e(tag, "Gemini API Error (${response.status}): $errorBody")
                throw Exception("Gemini API Error ${response.status}: $errorBody")
            }

            return response.body()
        } catch (e: Exception) {
            Log.e(tag, "Gemini Post failed", e)
            throw e
        }
    }

    suspend fun scanRegistrationCertificate(bitmap: Bitmap): Result<List<ScannedCarData>> {
        return try {
            val prompt = """
                Extract technical details from this vehicle document (registration certificate, invoice, insurance, or technical sheet).
                Analyze the document and look for these fields:
                - make, model, vin (MUST be 17 chars), year (4 digits), fuelType, engineSize (cc), power (hp or kW), torque (Nm), color, gears, registrationPlate, hasAbs (boolean), hasEsp (boolean), airbags (number).
                
                COLOR MAPPING (Return one of these standard values for 'color'):
                - White, Black, Silver, Gray, Blue, Red, Brown, Green, Yellow, Orange.
                
                ROMANIAN TO ENGLISH COLOR RULES:
                - Alb -> White, Negru -> Black, Argintiu -> Silver, Gri -> Gray, Albastru -> Blue, Rosu -> Red, Maro -> Brown, Verde -> Green, Galben -> Yellow, Portocaliu -> Orange.
                
                CRITICAL VALIDATION:
                1. Verify VIN format: must be 17 characters, only letters and digits (excluding I, O, Q).
                2. Verify Year: must be a realistic year (e.g., 1900-2026).
                3. Verify Engine Size: must be in cubic centimeters (cc).
                4. Brand (make) MUST be returned in Title Case (e.g., "Bmw", "Volkswagen").
                5. NUMERIC FIELDS (year, engineSize, power, torque, weight, capacity, speed, consumption, emissions, mileage, etc.) MUST contain ONLY the raw number, NO units (e.g., 230 instead of "230 Nm").
                6. If a value is unreadable, illogical, or not found, return null for that field.
                
                Return ONLY a JSON ARRAY containing one object with these keys: 
                make, model, vin, year, fuelType, engineSize, power, powerUnit, torque, color, 
                registrationPlate, numberOfSeats, numberOfDoors, weight, engineCode, 
                emissionStandard, gearboxType, gears, drivetrain, engineLayout, cylinderLayout, 
                fuelTankCapacity, topSpeed, acceleration0to100, fuelConsumptionCombined, co2Emissions,
                hasAbs, hasEsp, airbags,
                mileage,
                mileageHistory (a list of objects with 'km' and 'date' in YYYY-MM-DD format).
                
                Standard fuelType: Petrol, Diesel, Electric, Hybrid, LPG.
                Standard powerUnit: 'hp'. If kW is found, convert to hp (kW * 1.36).
            """.trimIndent()

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = Blob(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                generationConfig = GenerationConfig(temperature = temperature)
            )

            val response = postGemini(request)
            val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: throw Exception("Empty response from AI")

            val jsonText = extractJson(fullText)
            val data = if (jsonText.trim().startsWith("[")) {
                json.decodeFromString<List<ScannedCarData>>(jsonText)
            } else {
                listOf(json.decodeFromString<ScannedCarData>(jsonText))
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanDocument(uri: Uri, mimeType: String): Result<List<ScannedCarData>> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Could not read file")
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val prompt = """
                Extract technical details from this vehicle document (registration certificate, invoice, insurance, or technical sheet).
                The document might have multiple pages or be a complex PDF. Scan all visible text carefully.
                Analyze the document and look for these fields:
                - make, model, vin (MUST be 17 chars), year (4 digits), fuelType, engineSize (cc), power (hp or kW), torque (Nm), color, gears, registrationPlate, hasAbs (boolean), hasEsp (boolean), airbags (number).
                
                COLOR MAPPING (Return one of these standard values for 'color'):
                - White, Black, Silver, Gray, Blue, Red, Brown, Green, Yellow, Orange.
                
                ROMANIAN TO ENGLISH COLOR RULES:
                - Alb -> White, Negru -> Black, Argintiu -> Silver, Gri -> Gray, Albastru -> Blue, Rosu -> Red, Maro -> Brown, Verde -> Green, Galben -> Yellow, Portocaliu -> Orange.
                
                CRITICAL VALIDATION:
                1. Verify VIN format: must be 17 characters, only letters and digits (excluding I, O, Q).
                2. Verify Year: must be a realistic year (e.g., 1900-2026).
                3. Verify Engine Size: must be in cubic centimeters (cc).
                4. Brand (make) MUST be returned in Title Case (e.g., "Bmw", "Volkswagen").
                5. NUMERIC FIELDS (year, engineSize, power, torque, weight, capacity, speed, consumption, emissions, mileage, etc.) MUST contain ONLY the raw number, NO units (e.g., 230 instead of "230 Nm").
                6. If a value is unreadable, illogical, or not found, return null for that field.
                
                Return ONLY a JSON ARRAY containing one object with these keys: 
                make, model, vin, year, fuelType, engineSize, power, powerUnit, torque, color, 
                registrationPlate, numberOfSeats, numberOfDoors, weight, engineCode, 
                emissionStandard, gearboxType, gears, drivetrain, engineLayout, cylinderLayout, 
                fuelTankCapacity, topSpeed, acceleration0to100, fuelConsumptionCombined, co2Emissions,
                hasAbs, hasEsp, airbags,
                mileage,
                mileageHistory (a list of objects with 'km' and 'date' in YYYY-MM-DD format).
                
                Standard fuelType: Petrol, Diesel, Electric, Hybrid, LPG.
                Standard powerUnit: 'hp'. If kW is found, convert to hp (kW * 1.36).
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = Blob(mimeType = mimeType, data = base64Data))
                        )
                    )
                ),
                generationConfig = GenerationConfig(temperature = temperature)
            )

            val response = postGemini(request)
            val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: throw Exception("Empty response from AI")

            val jsonText = extractJson(fullText)
            val data = if (jsonText.trim().startsWith("[")) {
                json.decodeFromString<List<ScannedCarData>>(jsonText)
            } else {
                listOf(json.decodeFromString<ScannedCarData>(jsonText))
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDiagnosisResponse(
        prompt: String,
        carContext: String,
        history: List<com.dariusepure.caractivitylog.ui.cars.ChatMessage>,
        language: String = "English"
    ): GeminiResponse {
        val validatedHistory = history
            .dropWhile { !it.isUser }
            .let { h ->
                if (h.isNotEmpty() && h.size % 2 != 0) h.dropLast(1) else h
            }
            .map {
                Content(
                    role = if (it.isUser) "user" else "model",
                    parts = listOf(Part(text = it.text))
                )
            }

        val isFirstMessage = validatedHistory.isEmpty()
        val greetingRule = if (isFirstMessage) {
            "Always start with a brief, friendly mechanic's greeting."
        } else {
            "Do not repeat the greeting or introduction. Get straight to the point and answer the user's question directly."
        }

        val personaInstruction = "Act as a professional, experienced car mechanic. $greetingRule Use human-friendly terms only; NEVER use internal data names or variable names like 'fuelType', 'vin', or 'make' in your sentences—use natural language like 'tip de combustibil', 'serie sasiu', or 'marca' instead. NEVER mention that you are reading from a 'context' or 'database'. Talk to the user as if you are standing next to their car in a garage. If you see recent maintenance or relevant history, mention it naturally (e.g., 'Am văzut că ai făcut recent revizia, e foarte bine pentru motor')."
        val formattingInstruction = "STRICTLY PROHIBIT the use of markdown bold (**text**) or italics (_text_). Respond using ONLY plain text. Use simple paragraphs or plain lists with dashes (-) if needed."
        val languageInstruction = "IMPORTANT: Please respond in $language."
        
        val finalizedPrompt = systemPrompt.replace("{{context}}", carContext)
        val fullPrompt = "$finalizedPrompt\n\nINSTRUCTIONS:\n- $personaInstruction\n- $formattingInstruction\n- $languageInstruction\n\nUser: $prompt"

        val userContent = Content(
            role = "user",
            parts = listOf(Part(text = fullPrompt))
        )

        val request = GeminiRequest(
            contents = validatedHistory + userContent,
            generationConfig = GenerationConfig(temperature = temperature)
        )

        return postGemini(request)
    }

    private fun extractJson(text: String): String {
        val cleanedText = text.replace("```json", "").replace("```", "").trim()
        val startBrace = cleanedText.indexOf('{')
        val startBracket = cleanedText.indexOf('[')
        
        val start = when {
            startBrace != -1 && startBracket != -1 -> minOf(startBrace, startBracket)
            startBrace != -1 -> startBrace
            startBracket != -1 -> startBracket
            else -> -1
        }
        
        val endBrace = cleanedText.lastIndexOf('}')
        val endBracket = cleanedText.lastIndexOf(']')
        
        val end = when {
            endBrace != -1 && endBracket != -1 -> maxOf(endBrace, endBracket)
            endBrace != -1 -> endBrace
            endBracket != -1 -> endBracket
            else -> -1
        }

        if (start != -1 && end != -1 && end > start) {
            return cleanedText.substring(start, end + 1)
        }
        return cleanedText
    }
}

