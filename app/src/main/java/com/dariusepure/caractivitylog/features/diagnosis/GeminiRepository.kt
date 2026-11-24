package com.dariusepure.caractivitylog.features.diagnosis

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.dariusepure.caractivitylog.BuildConfig
import com.dariusepure.caractivitylog.core.util.DiagnosticUtils
import com.dariusepure.caractivitylog.core.domain.ScannedCarData
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
        get() = remoteConfig.getString("gemini_model_name").ifBlank { "gemini-2.0-flash" }

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
    private val responseCache = java.util.concurrent.ConcurrentHashMap<String, GeminiResponse>()

    private suspend fun postGemini(request: GeminiRequest): GeminiResponse {
        val candidateModels = listOf(modelName, "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-flash-8b")
            .filter { it.isNotBlank() }
            .distinct()

        val sha1 = DiagnosticUtils.getAppSignatureSha1(context, withColons = true)
        val packageName = context.packageName

        var lastException: Exception? = null

        for (model in candidateModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=$geminiApiKey"
            Log.d(tag, "Attempting Gemini request with model: $model")

            for (attempt in 1..2) {
                try {
                    val response: io.ktor.client.statement.HttpResponse = httpClient.post(url) {
                        contentType(ContentType.Application.Json)
                        header("X-Android-Package", packageName)
                        header("X-Android-Cert", sha1)
                        setBody(request)
                    }

                    val statusCode = response.status.value
                    if (statusCode in 200..299) {
                        Log.d(tag, "Gemini request succeeded with model: $model")
                        return response.body()
                    }

                    val errorBody = response.body<String>()
                    Log.w(tag, "Gemini API Attempt $attempt for model $model returned status $statusCode: $errorBody")

                    if (statusCode == 503 || statusCode == 429 || statusCode == 500) {
                        lastException = Exception("Server Gemini supra-solicitat ($statusCode). Reîncercăm...")
                        kotlinx.coroutines.delay(1000L * attempt)
                    } else if (statusCode == 404) {
                        lastException = Exception("Modelul $model nu este disponibil ($statusCode).")
                        break
                    } else {
                        throw Exception("Gemini API Error $statusCode: $errorBody")
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Failed attempt $attempt for model $model: ${e.message}")
                    lastException = e
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(1000L * attempt)
                    }
                }
            }
        }

        throw lastException ?: Exception("Serverul Gemini este supra-solicitat (High Demand). Te rugăm să încerci din nou în câteva secunde.")
    }

    suspend fun scanRegistrationCertificate(bitmap: Bitmap): Result<List<ScannedCarData>> {
        return try {
            val prompt = """
                Extract technical details from this vehicle document (registration certificate, invoice, insurance, or technical sheet).
                Analyze the document and look for these fields:
                - make, model, vin (MUST be 17 chars), year (4 digits), fuelType, engineSize (cc), power (hp or kW), torque (Nm), color, gears, registrationPlate, airbags (number).

                ENGINE VARIANT / COMMERCIAL NAME:
                - Deduce the commercial engine name/variant (e.g., 320d, 1.9 TDI, 118i, M Sport, BlueHDi, EcoBoost) based on make, model, year, engineSize, and fuelType. Return it in the 'engineVariant' field.
                - Do NOT fill in or return engineSize unless you have successfully deduced or found an engineVariant (engine badge/commercial name).
                
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
                make, model, generation, vin, year, fuelType, engineSize, power, powerUnit, torque, color, 
                registrationPlate, vehicleType, manufacturingCountry, numberOfSeats, numberOfDoors, weight, engineCode, 
                emissionStandard, gearboxType, gears, drivetrain, engineLayout, cylinderLayout, aspiration, 
                fuelTankCapacity, topSpeed, acceleration0to100, fuelConsumptionCombined, fuelConsumptionUrban, fuelConsumptionExtraUrban, co2Emissions,
                length, width, height, wheelbase, bootSpace, frontSuspension, rearSuspension, frontBrakes, rearBrakes,
                hasAbs, hasEsp, airbags,
                mileage,
                mileageHistory (a list of objects with 'km' and 'date' in YYYY-MM-DD format),
                engineVariant.
                
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
                generationConfig = GenerationConfig(temperature = temperature, responseMimeType = "application/json")
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
                - make, model, vin (MUST be 17 chars), year (4 digits), fuelType, engineSize (cc), power (hp or kW), torque (Nm), color, gears, registrationPlate, airbags (number).

                ENGINE VARIANT / COMMERCIAL NAME:
                - Deduce the commercial engine name/variant (e.g., 320d, 1.9 TDI, 118i, M Sport, BlueHDi, EcoBoost) based on make, model, year, engineSize, and fuelType. Return it in the 'engineVariant' field.
                - Do NOT fill in or return engineSize unless you have successfully deduced or found an engineVariant (engine badge/commercial name).
                
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
                make, model, generation, vin, year, fuelType, engineSize, power, powerUnit, torque, color, 
                registrationPlate, vehicleType, manufacturingCountry, numberOfSeats, numberOfDoors, weight, engineCode, 
                emissionStandard, gearboxType, gears, drivetrain, engineLayout, cylinderLayout, aspiration, 
                fuelTankCapacity, topSpeed, acceleration0to100, fuelConsumptionCombined, fuelConsumptionUrban, fuelConsumptionExtraUrban, co2Emissions,
                length, width, height, wheelbase, bootSpace, frontSuspension, rearSuspension, frontBrakes, rearBrakes,
                hasAbs, hasEsp, airbags,
                mileage,
                mileageHistory (a list of objects with 'km' and 'date' in YYYY-MM-DD format),
                engineVariant.
                
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
                generationConfig = GenerationConfig(temperature = temperature, responseMimeType = "application/json")
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
        history: List<com.dariusepure.caractivitylog.features.diagnosis.ChatMessage>,
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

        val cacheKey = "$prompt|$carContext|$language"
        responseCache[cacheKey]?.let { return it }

        val request = GeminiRequest(
            contents = validatedHistory + userContent,
            generationConfig = GenerationConfig(temperature = temperature)
        )

        val response = postGemini(request)
        responseCache[cacheKey] = response
        return response
    }

    private fun extractJson(text: String): String {
        val cleanedText = text.replace(Regex("(?i)```json"), "").replace("```", "").trim()
        
        val firstBracket = cleanedText.indexOf('[')
        val firstBrace = cleanedText.indexOf('{')
        
        if (firstBracket != -1 && (firstBrace == -1 || firstBracket < firstBrace)) {
            val lastBracket = cleanedText.lastIndexOf(']')
            if (lastBracket > firstBracket) {
                return cleanedText.substring(firstBracket, lastBracket + 1)
            }
        }
        
        if (firstBrace != -1) {
            val lastBrace = cleanedText.lastIndexOf('}')
            if (lastBrace > firstBrace) {
                return cleanedText.substring(firstBrace, lastBrace + 1)
            }
        }

        return cleanedText
    }
}

